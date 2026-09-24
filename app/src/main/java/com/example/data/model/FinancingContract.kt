package com.example.data.model

/**
 * Parâmetros e estatísticas do contrato de financiamento.
 */
data class FinancingContract(
    val title: String = "Financiamento Imobiliário / Veículo",
    val totalInstallments: Int = 60,
    val nominalInstallment: Double = 985.38,
    val totalContractAmount: Double = 59122.80,
    val monthlyInterestRate: Double = 0.0165, // ~1.65% a.m.
    val startDate: String = "14/05/2026",
    val originalEndDate: String = "14/04/2031"
)

/**
 * Resumo dos KPIs executivos do financiamento.
 */
data class FinancingSummary(
    val totalContractAmount: Double,       // R$ 59.122,80
    val totalPaidAmount: Double,           // R$ 6.141,95
    val remainingNominalBalance: Double,   // R$ 52.980,85
    val amortizedPrincipalPaid: Double,    // R$ 36.249,64
    val totalSavedInterest: Double,        // R$ 1.741,09
    val totalInstallments: Int,            // 60
    val paidInstallmentsCount: Int,        // 8
    val remainingInstallmentsCount: Int,   // 52
    val anticipatedInstallmentsCount: Int, // 3 (parcelas 58, 59, 60)
    val nextDueInstallment: InstallmentEntity?, // Parcela #6
    val progressPercentage: Float,         // 8 / 60 = 13.33%
    val monthsAdvanced: Int                // 3 meses adiantados
)
