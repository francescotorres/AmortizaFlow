package com.example

import com.example.data.local.InitialSpreadsheetData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun initialSpreadsheetData_matchesOriginalSpreadsheetValues() {
        val installments = InitialSpreadsheetData.getInitialInstallments()

        // 1. Total de parcelas deve ser 60
        assertEquals(60, installments.size)

        // 2. Total nominal deve ser 60 * 985.38 = 59.122,80
        val totalNominal = installments.sumOf { it.nominalAmount }
        assertEquals(59122.80, totalNominal, 0.01)

        // 3. Parcelas pagas até o momento: 8
        val paidList = installments.filter { it.isPaid }
        assertEquals(8, paidList.size)

        // 4. Total pago até o momento: R$ 6.141,95
        val totalPaid = paidList.sumOf { it.actualPaidAmount ?: 0.0 }
        assertEquals(6141.95, totalPaid, 0.01)

        // 5. Total de juros economizados obtido com amortizações: R$ 1.741,09
        val totalSaved = paidList.sumOf { it.savedInterest }
        assertEquals(1741.09, totalSaved, 0.01)

        // 6. Parcelas antecipadas do fim (58, 59, 60): 3 parcelas
        val anticipated = paidList.filter { it.isAnticipated }
        assertEquals(3, anticipated.size)
        assertTrue(anticipated.any { it.parcelNumber == 58 })
        assertTrue(anticipated.any { it.parcelNumber == 59 })
        assertTrue(anticipated.any { it.parcelNumber == 60 })
    }

    @Test
    fun dueDateCalculations_spanSixtyMonths() {
        val firstDate = InitialSpreadsheetData.calculateDueDate(1)
        val lastDate = InitialSpreadsheetData.calculateDueDate(60)

        assertEquals("14/05/2026", firstDate)
        assertEquals("14/04/2031", lastDate)
    }

    @Test
    fun theoreticalAmortization_isPositiveAndConsistent() {
        val installments = InitialSpreadsheetData.getInitialInstallments()
        for (item in installments) {
            assertTrue("A amortização teórica da parcela #${item.parcelNumber} deve ser maior que 0", item.theoreticalAmortization > 0)
            assertTrue("A amortização não deve exceder o valor nominal", item.theoreticalAmortization <= item.nominalAmount)
        }
    }
}
