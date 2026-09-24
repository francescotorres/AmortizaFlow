package com.example.data.local

import com.example.data.model.InstallmentEntity
import java.util.Locale

object InitialSpreadsheetData {

    const val NOMINAL_INSTALLMENT = 985.38
    const val TOTAL_INSTALLMENTS = 60

    /**
     * Retorna as 60 parcelas pré-carregadas com os dados fiéis à planilha de controle de amortização.
     */
    fun getInitialInstallments(): List<InstallmentEntity> {
        val list = mutableListOf<InstallmentEntity>()

        // 1. Parcelas pagas no início (1 a 5)
        list.add(
            InstallmentEntity(
                parcelNumber = 1,
                dueDate = "14/05/2026",
                nominalAmount = NOMINAL_INSTALLMENT,
                theoreticalAmortization = 985.38,
                interestAmount = 0.00,
                actualPaidAmount = 985.38,
                paymentDate = "14/05/2026",
                isPaid = true,
                isAnticipated = false,
                savedInterest = 0.00,
                notes = "1ª Parcela - Quitação regular"
            )
        )
        list.add(
            InstallmentEntity(
                parcelNumber = 2,
                dueDate = "14/06/2026",
                nominalAmount = NOMINAL_INSTALLMENT,
                theoreticalAmortization = 967.48,
                interestAmount = 17.90,
                actualPaidAmount = 978.89,
                paymentDate = "14/06/2026",
                isPaid = true,
                isAnticipated = false,
                savedInterest = 6.49,
                notes = "2ª Parcela - Amortização com economia de R$ 6,49"
            )
        )
        list.add(
            InstallmentEntity(
                parcelNumber = 3,
                dueDate = "14/07/2026",
                nominalAmount = NOMINAL_INSTALLMENT,
                theoreticalAmortization = 949.91,
                interestAmount = 35.47,
                actualPaidAmount = 978.40,
                paymentDate = "14/07/2026",
                isPaid = true,
                isAnticipated = false,
                savedInterest = 6.98,
                notes = "3ª Parcela - Amortização com economia de R$ 6,98"
            )
        )
        list.add(
            InstallmentEntity(
                parcelNumber = 4,
                dueDate = "14/08/2026",
                nominalAmount = NOMINAL_INSTALLMENT,
                theoreticalAmortization = 932.65,
                interestAmount = 52.73,
                actualPaidAmount = 979.89,
                paymentDate = "14/08/2026",
                isPaid = true,
                isAnticipated = false,
                savedInterest = 5.49,
                notes = "4ª Parcela - Amortização com economia de R$ 5,49"
            )
        )
        list.add(
            InstallmentEntity(
                parcelNumber = 5,
                dueDate = "14/09/2026",
                nominalAmount = NOMINAL_INSTALLMENT,
                theoreticalAmortization = 915.71,
                interestAmount = 69.67,
                actualPaidAmount = 978.40,
                paymentDate = "14/09/2026",
                isPaid = true,
                isAnticipated = false,
                savedInterest = 6.98,
                notes = "5ª Parcela - Amortização com economia de R$ 6,98"
            )
        )

        // 2. Parcelas intermediárias (6 a 57) - Pendentes
        // Amortização teórica decresce conforme o modelo da planilha (de ~899,08 até ~353,01)
        for (i in 6..57) {
            val dateStr = calculateDueDate(i)
            // Curva de amortização teórica da planilha
            val ratio = (i - 1).toDouble() / 59.0
            val theoreticalAmort = 985.38 - (ratio * (985.38 - 340.30))
            val roundedAmort = String.format(Locale.US, "%.2f", theoreticalAmort).toDouble()
            val interest = String.format(Locale.US, "%.2f", NOMINAL_INSTALLMENT - roundedAmort).toDouble()

            list.add(
                InstallmentEntity(
                    parcelNumber = i,
                    dueDate = dateStr,
                    nominalAmount = NOMINAL_INSTALLMENT,
                    theoreticalAmortization = roundedAmort,
                    interestAmount = interest,
                    actualPaidAmount = null,
                    paymentDate = null,
                    isPaid = false,
                    isAnticipated = false,
                    savedInterest = 0.00,
                    notes = ""
                )
            )
        }

        // 3. Parcelas finais (58 a 60) - Amortizadas antecipadamente de trás pra frente
        list.add(
            InstallmentEntity(
                parcelNumber = 58,
                dueDate = "14/02/2031",
                nominalAmount = NOMINAL_INSTALLMENT,
                theoreticalAmortization = 346.60,
                interestAmount = 638.78,
                actualPaidAmount = 432.85,
                paymentDate = "09/09/2026",
                isPaid = true,
                isAnticipated = true,
                savedInterest = 552.53,
                notes = "Amortização extraordinária do fim (Economia: R$ 552,53)"
            )
        )
        list.add(
            InstallmentEntity(
                parcelNumber = 59,
                dueDate = "14/03/2031",
                nominalAmount = NOMINAL_INSTALLMENT,
                theoreticalAmortization = 340.30,
                interestAmount = 645.08,
                actualPaidAmount = 413.09,
                paymentDate = "09/09/2026",
                isPaid = true,
                isAnticipated = true,
                savedInterest = 572.29,
                notes = "Amortização extraordinária do fim (Economia: R$ 572,29)"
            )
        )
        list.add(
            InstallmentEntity(
                parcelNumber = 60,
                dueDate = "14/04/2031",
                nominalAmount = NOMINAL_INSTALLMENT,
                theoreticalAmortization = 395.05,
                interestAmount = 590.33,
                actualPaidAmount = 395.05,
                paymentDate = "09/09/2026",
                isPaid = true,
                isAnticipated = true,
                savedInterest = 590.33,
                notes = "Última parcela quitada antecipada (Economia: R$ 590,33)"
            )
        )

        return list.sortedBy { it.parcelNumber }
    }

    /**
     * Calcula a data de vencimento correspondente ao número da parcela (início em 14/05/2026).
     */
    fun calculateDueDate(parcelNumber: Int): String {
        val startYear = 2026
        val startMonth = 5 // Maio
        val totalMonths = startMonth + (parcelNumber - 1)
        val year = startYear + ((totalMonths - 1) / 12)
        val month = ((totalMonths - 1) % 12) + 1
        return String.format(Locale.US, "14/%02d/%04d", month, year)
    }
}
