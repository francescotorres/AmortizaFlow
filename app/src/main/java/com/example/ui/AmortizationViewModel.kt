package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.InitialSpreadsheetData
import com.example.data.model.FinancingSummary
import com.example.data.model.InstallmentEntity
import com.example.data.repository.AmortizationRepository
import com.example.data.repository.AmortizationType
import com.example.data.repository.SimulationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InstallmentFilter {
    ALL, PAID, PENDING, ANTICIPATED
}

/**
 * Item analítico para o Relatório Mensal de Saldo Devedor e Juros Acumulados.
 */
data class MonthlyReportItem(
    val monthYear: String,
    val parcelNumber: Int,
    val initialBalance: Double,
    val paymentAmount: Double,
    val amortizationAmount: Double,
    val interestPaid: Double,
    val savedInterest: Double,
    val finalBalance: Double,
    val isPaid: Boolean,
    val isAnticipated: Boolean
)

class AmortizationViewModel(
    private val repository: AmortizationRepository
) : ViewModel() {

    val summary: StateFlow<FinancingSummary> = repository.financingSummary
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FinancingSummary(
                totalContractAmount = 59122.80,
                totalPaidAmount = 6141.95,
                remainingNominalBalance = 52980.85,
                amortizedPrincipalPaid = 36249.64,
                totalSavedInterest = 1741.09,
                totalInstallments = 60,
                paidInstallmentsCount = 8,
                remainingInstallmentsCount = 52,
                anticipatedInstallmentsCount = 3,
                nextDueInstallment = null,
                progressPercentage = 0.133f,
                monthsAdvanced = 3
            )
        )

    val allInstallments: StateFlow<List<InstallmentEntity>> = repository.allInstallments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InitialSpreadsheetData.getInitialInstallments()
        )

    private val _selectedFilter = MutableStateFlow(InstallmentFilter.ALL)
    val selectedFilter: StateFlow<InstallmentFilter> = _selectedFilter.asStateFlow()

    val filteredInstallments: StateFlow<List<InstallmentEntity>> = combine(
        allInstallments,
        _selectedFilter
    ) { list, filter ->
        when (filter) {
            InstallmentFilter.ALL -> list
            InstallmentFilter.PAID -> list.filter { it.isPaid }
            InstallmentFilter.PENDING -> list.filter { !it.isPaid }
            InstallmentFilter.ANTICIPATED -> list.filter { it.isAnticipated }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InitialSpreadsheetData.getInitialInstallments()
    )

    // Simulador
    private val _simulatedAmount = MutableStateFlow("1000")
    val simulatedAmount: StateFlow<String> = _simulatedAmount.asStateFlow()

    private val _simulationType = MutableStateFlow(AmortizationType.REDUCE_TERM)
    val simulationType: StateFlow<AmortizationType> = _simulationType.asStateFlow()

    private val _simulationResult = MutableStateFlow<SimulationResult?>(null)
    val simulationResult: StateFlow<SimulationResult?> = _simulationResult.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>("Bem-vindo ao AmortizaFlow!")
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Relatório mensal gerado dinamicamente
    val monthlyReports: StateFlow<List<MonthlyReportItem>> = allInstallments.combine(summary) { list, _ ->
        generateMonthlyReports(list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        runSimulation()
    }

    fun setFilter(filter: InstallmentFilter) {
        _selectedFilter.value = filter
    }

    fun togglePaid(parcelNumber: Int) {
        viewModelScope.launch {
            repository.toggleInstallmentPaid(parcelNumber)
            _userMessage.value = "Status da parcela #$parcelNumber atualizado!"
        }
    }

    fun updateInstallmentDetails(
        parcelNumber: Int,
        actualPaid: Double,
        paymentDate: String,
        notes: String
    ) {
        viewModelScope.launch {
            val list = allInstallments.value
            val current = list.find { it.parcelNumber == parcelNumber } ?: return@launch
            val saved = (current.nominalAmount - actualPaid).coerceAtLeast(0.0)
            val updated = current.copy(
                actualPaidAmount = actualPaid,
                paymentDate = paymentDate,
                savedInterest = saved,
                notes = notes,
                isPaid = true
            )
            repository.updateInstallment(updated)
            _userMessage.value = "Parcela #$parcelNumber atualizada com sucesso!"
        }
    }

    fun setSimulationAmount(value: String) {
        _simulatedAmount.value = value.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        runSimulation()
    }

    fun setSimulationType(type: AmortizationType) {
        _simulationType.value = type
        runSimulation()
    }

    fun runSimulation() {
        viewModelScope.launch {
            val amount = _simulatedAmount.value.toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                val result = repository.simulateAmortization(amount, _simulationType.value)
                _simulationResult.value = result
            } else {
                _simulationResult.value = null
            }
        }
    }

    fun applySimulation() {
        val result = _simulationResult.value ?: return
        viewModelScope.launch {
            repository.applySimulatedAmortization(result)
            _userMessage.value = "Amortização de ${result.monthsShortened} parcelas aplicada ao contrato!"
            _simulationResult.value = null
        }
    }

    fun resetToSpreadsheetData() {
        viewModelScope.launch {
            repository.resetToSpreadsheetData()
            _userMessage.value = "Dados oficiais da planilha restaurados!"
            runSimulation()
        }
    }

    fun dismissMessage() {
        _userMessage.value = null
    }

    private fun generateMonthlyReports(list: List<InstallmentEntity>): List<MonthlyReportItem> {
        var currentBalance = 59122.80
        val reports = mutableListOf<MonthlyReportItem>()

        for (item in list) {
            val initBal = currentBalance
            val paid = item.actualPaidAmount ?: item.nominalAmount
            val amort = item.theoreticalAmortization
            val interest = item.interestAmount
            val saved = item.savedInterest

            if (item.isPaid) {
                currentBalance -= paid
            } else {
                currentBalance -= amort
            }
            val finalBal = currentBalance.coerceAtLeast(0.0)

            reports.add(
                MonthlyReportItem(
                    monthYear = item.dueDate,
                    parcelNumber = item.parcelNumber,
                    initialBalance = initBal,
                    paymentAmount = paid,
                    amortizationAmount = amort,
                    interestPaid = interest,
                    savedInterest = saved,
                    finalBalance = finalBal,
                    isPaid = item.isPaid,
                    isAnticipated = item.isAnticipated
                )
            )
        }
        return reports
    }
}

class AmortizationViewModelFactory(
    private val repository: AmortizationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AmortizationViewModel::class.java)) {
            return AmortizationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
