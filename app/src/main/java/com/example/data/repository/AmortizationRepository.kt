package com.example.data.repository

import com.example.data.local.InitialSpreadsheetData
import com.example.data.local.InstallmentDao
import com.example.data.model.FinancingSummary
import com.example.data.model.InstallmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AmortizationType {
    REDUCE_TERM,   // Redução de prazo (de trás pra frente, eliminando parcelas finais)
    REDUCE_PAYMENT // Redução do valor da parcela mensal
}

data class SimulationResult(
    val simulatedAmount: Double,
    val type: AmortizationType,
    val eliminatedInstallments: List<Int>,
    val totalSavedInterest: Double,
    val totalPaidInSimulation: Double,
    val monthsShortened: Int,
    val newProjectedEndDate: String,
    val newMonthlyInstallment: Double?
)

class AmortizationRepository(private val dao: InstallmentDao) {

    val allInstallments: Flow<List<InstallmentEntity>> = dao.getAllInstallments()

    val nextPendingInstallment: Flow<InstallmentEntity?> = dao.getNextPendingInstallment()

    /**
     * Calcula dinamicamente o resumo executivo de KPIs a partir da lista atualizada de parcelas.
     */
    val financingSummary: Flow<FinancingSummary> = allInstallments.map { list ->
        calculateSummary(list)
    }

    private fun calculateSummary(list: List<InstallmentEntity>): FinancingSummary {
        val totalContract = 59122.80
        val paidList = list.filter { it.isPaid }
        val pendingList = list.filter { !it.isPaid }

        val totalPaid = paidList.sumOf { it.actualPaidAmount ?: it.nominalAmount }
        val remainingNominal = pendingList.sumOf { it.nominalAmount }
        val totalSavedInterest = paidList.sumOf { it.savedInterest }
        val amortizedPrincipal = paidList.sumOf { it.theoreticalAmortization }

        val anticipatedCount = paidList.count { it.isAnticipated }
        val nextDue = pendingList.minByOrNull { it.parcelNumber }

        val progress = if (list.isNotEmpty()) paidList.size.toFloat() / list.size.toFloat() else 0f

        return FinancingSummary(
            totalContractAmount = totalContract,
            totalPaidAmount = totalPaid,
            remainingNominalBalance = remainingNominal,
            amortizedPrincipalPaid = amortizedPrincipal,
            totalSavedInterest = totalSavedInterest,
            totalInstallments = list.size,
            paidInstallmentsCount = paidList.size,
            remainingInstallmentsCount = pendingList.size,
            anticipatedInstallmentsCount = anticipatedCount,
            nextDueInstallment = nextDue,
            progressPercentage = progress,
            monthsAdvanced = anticipatedCount
        )
    }

    suspend fun updateInstallment(installment: InstallmentEntity) {
        dao.updateInstallment(installment)
    }

    suspend fun toggleInstallmentPaid(parcelNumber: Int) {
        val item = dao.getInstallmentByNumber(parcelNumber) ?: return
        val newStatus = !item.isPaid
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val updated = if (newStatus) {
            // Marcando como paga
            val paidVal = if (item.actualPaidAmount != null && item.actualPaidAmount > 0) {
                item.actualPaidAmount
            } else {
                item.theoreticalAmortization // Se for do fim ou antecipada, default é a amortização
            }
            val saved = (item.nominalAmount - paidVal).coerceAtLeast(0.0)
            item.copy(
                isPaid = true,
                actualPaidAmount = paidVal,
                paymentDate = todayStr,
                savedInterest = saved
            )
        } else {
            // Desmarcando (voltando a pendente)
            item.copy(
                isPaid = false,
                actualPaidAmount = null,
                paymentDate = null,
                savedInterest = 0.0
            )
        }
        dao.updateInstallment(updated)
    }

    /**
     * Simula uma amortização extraordinária baseada no valor informado pelo usuário.
     */
    suspend fun simulateAmortization(amount: Double, type: AmortizationType): SimulationResult {
        val list = dao.getAllInstallments().first()
        val pendingFromEnd = list.filter { !it.isPaid }.sortedByDescending { it.parcelNumber }

        if (type == AmortizationType.REDUCE_TERM) {
            var budgetRemaining = amount
            val eliminated = mutableListOf<Int>()
            var savedInterestTotal = 0.0
            var paidTotal = 0.0

            for (installment in pendingFromEnd) {
                // Custo de amortizar antecipadamente essa parcela (apenas a amortização do principal sem os juros futuros!)
                val costToAmortize = installment.theoreticalAmortization
                if (budgetRemaining >= costToAmortize) {
                    budgetRemaining -= costToAmortize
                    eliminated.add(installment.parcelNumber)
                    val interestSaved = installment.nominalAmount - costToAmortize
                    savedInterestTotal += interestSaved
                    paidTotal += costToAmortize
                } else {
                    break
                }
            }

            // Calcula nova data de quitação
            val remainingPending = pendingFromEnd.filterNot { it.parcelNumber in eliminated }
            val lastActivePending = remainingPending.maxByOrNull { it.parcelNumber }
            val projectedEnd = lastActivePending?.dueDate ?: "Totalmente Quitado!"

            return SimulationResult(
                simulatedAmount = amount,
                type = type,
                eliminatedInstallments = eliminated,
                totalSavedInterest = savedInterestTotal,
                totalPaidInSimulation = paidTotal,
                monthsShortened = eliminated.size,
                newProjectedEndDate = projectedEnd,
                newMonthlyInstallment = null
            )
        } else {
            // Redução do valor das parcelas mantendo o prazo
            val pending = list.filter { !it.isPaid }
            val count = pending.size
            val reductionPerInstallment = if (count > 0) amount / count else 0.0
            val currentNominal = InitialSpreadsheetData.NOMINAL_INSTALLMENT
            val newInstallment = (currentNominal - reductionPerInstallment).coerceAtLeast(0.0)

            return SimulationResult(
                simulatedAmount = amount,
                type = type,
                eliminatedInstallments = emptyList(),
                totalSavedInterest = amount * 0.45, // Projeção de juros poupados pelo valor presente
                totalPaidInSimulation = amount,
                monthsShortened = 0,
                newProjectedEndDate = pending.maxByOrNull { it.parcelNumber }?.dueDate ?: "14/04/2031",
                newMonthlyInstallment = newInstallment
            )
        }
    }

    /**
     * Aplica efetivamente a amortização simulada no banco de dados.
     */
    suspend fun applySimulatedAmortization(result: SimulationResult) {
        if (result.type == AmortizationType.REDUCE_TERM && result.eliminatedInstallments.isNotEmpty()) {
            val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            for (number in result.eliminatedInstallments) {
                val installment = dao.getInstallmentByNumber(number) ?: continue
                val paidVal = installment.theoreticalAmortization
                val saved = installment.nominalAmount - paidVal
                val updated = installment.copy(
                    isPaid = true,
                    isAnticipated = true,
                    actualPaidAmount = paidVal,
                    paymentDate = todayStr,
                    savedInterest = saved,
                    notes = "Amortização extraordinária aplicada pelo app"
                )
                dao.updateInstallment(updated)
            }
        }
    }

    /**
     * Restaura os dados para a planilha original exata.
     */
    suspend fun resetToSpreadsheetData() {
        dao.deleteAll()
        dao.insertAll(InitialSpreadsheetData.getInitialInstallments())
    }
}
