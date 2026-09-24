package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade que representa uma parcela do financiamento.
 * Contém dados nominais, amortização teórica, valores reais pagos, economia e status.
 */
@Entity(tableName = "installments")
data class InstallmentEntity(
    @PrimaryKey
    val parcelNumber: Int,                     // Número da parcela (1 a 60)
    val dueDate: String,                       // Data de vencimento (ex: "14/05/2026")
    val nominalAmount: Double,                 // Valor nominal contratual (ex: R$ 985,38)
    val theoreticalAmortization: Double,       // Amortização teórica do principal
    val interestAmount: Double,                // Parcela de juros nominal (nominal - amortização)
    val actualPaidAmount: Double?,             // Valor efetivamente pago (ex: R$ 395,05 quando amortizada)
    val paymentDate: String?,                  // Data em que foi paga
    val isPaid: Boolean,                       // Se está quitada ou pendente
    val isAnticipated: Boolean,                // Se foi amortizada antecipadamente (de trás pra frente)
    val savedInterest: Double,                 // Economia obtida em juros (nominal - pago)
    val notes: String = ""                     // Observações
)
