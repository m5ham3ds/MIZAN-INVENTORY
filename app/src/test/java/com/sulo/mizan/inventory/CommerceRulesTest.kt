package com.sulo.mizan.inventory

import com.sulo.mizan.inventory.core.domain.CommerceRules
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CommerceRulesTest {
    private fun bd(value: String) = BigDecimal(value)

    @Test fun quantitiesMustBePositiveAndKeepDecimalPrecision() {
        assertEquals(bd("2.75"), CommerceRules.quantity("2.75"))
        assertThrows(IllegalArgumentException::class.java) { CommerceRules.quantity("0.0") }
        assertThrows(IllegalArgumentException::class.java) { CommerceRules.quantity("-1") }
    }

    @Test fun moneyAndRatesRejectInvalidNegativeOrZeroInputs() {
        assertEquals(bd("0.000"), CommerceRules.nonNegativeMoney("0.000"))
        assertThrows(IllegalArgumentException::class.java) { CommerceRules.nonNegativeMoney("-0.01") }
        assertThrows(IllegalArgumentException::class.java) { CommerceRules.positiveRate("0.000") }
        assertThrows(IllegalArgumentException::class.java) { CommerceRules.positiveRate("-3") }
        assertEquals(bd("2.5"), CommerceRules.positiveRate("2.5"))
    }

    @Test fun invoiceTotalAppliesDiscountTaxAndCurrencyScale() {
        assertEquals(bd("97.50"), CommerceRules.invoiceTotal(bd("100"), bd("12.5"), bd("10"), 2))
    }

    @Test fun invoiceRejectsDiscountAboveSubtotal() {
        assertThrows(IllegalArgumentException::class.java) {
            CommerceRules.invoiceTotal(bd("10"), bd("10.01"), bd("0"), 2)
        }
    }

    @Test fun invoiceRejectsTaxWhenSubtotalIsZero() {
        assertThrows(IllegalArgumentException::class.java) {
            CommerceRules.invoiceTotal(bd("0.00"), bd("0"), bd("1"), 2)
        }
    }

    @Test fun weightedAverageCostWeightsOldAndReceivedStock() {
        assertEquals(bd("6.00000000"), CommerceRules.weightedAverageCost(bd("10"), bd("4"), bd("10"), bd("8")))
    }

    @Test fun firstPurchaseSetsAverageCostToReceivedUnitCost() {
        assertEquals(bd("7.25000000"), CommerceRules.weightedAverageCost(bd("0"), bd("0"), bd("4"), bd("7.25")))
    }

    @Test fun weightedAverageRejectsNegativeAndZeroReceiptQuantity() {
        assertThrows(IllegalArgumentException::class.java) { CommerceRules.weightedAverageCost(bd("1"), bd("1"), bd("0"), bd("1")) }
        assertThrows(IllegalArgumentException::class.java) { CommerceRules.weightedAverageCost(bd("-1"), bd("1"), bd("1"), bd("1")) }
    }

    @Test fun shippingIsAllocatedByLineValue() {
        assertEquals(0, CommerceRules.allocateShipping(bd("100"), bd("25"), bd("100"), 2).compareTo(bd("25")))
        assertEquals(0, CommerceRules.allocateShipping(bd("0"), bd("25"), bd("100"), 2).compareTo(bd("0")))
    }

    @Test fun shippingIsSplitEquallyWhenAllLinesHaveZeroValue() {
        assertEquals(0, CommerceRules.allocateShipping(bd("60"), bd("0"), bd("0"), 3).compareTo(bd("20")))
    }

    @Test fun landedCostIncludesCurrencyConversionAndFreightPerUnit() {
        assertEquals(bd("22.00000000"), CommerceRules.landedUnitCost(bd("10"), bd("1.2"), bd("30"), bd("3")))
    }

    @Test fun outstandingDebtCannotGoBelowZero() {
        assertEquals(bd("30"), CommerceRules.outstanding(bd("100"), bd("50"), bd("20")))
        assertEquals(bd("0"), CommerceRules.outstanding(bd("100"), bd("80"), bd("40")))
    }

    @Test fun invoiceDebtAccountsForReturnsAndSettledRefunds() {
        assertEquals(bd("30"), CommerceRules.outstandingAfterReturns(bd("100"), bd("50"), bd("20"), bd("0")))
        assertEquals(bd("0"), CommerceRules.outstandingAfterReturns(bd("100"), bd("90"), bd("20"), bd("10")))
        assertThrows(IllegalArgumentException::class.java) {
            CommerceRules.outstandingAfterReturns(bd("100"), bd("0"), bd("-1"), bd("0"))
        }
    }

    @Test fun refundDueAppearsOnlyWhenPaymentsExceedFinalInvoiceValue() {
        assertEquals(bd("0"), CommerceRules.refundDueAfterReturns(bd("100"), bd("70"), bd("20"), bd("0")))
        assertEquals(bd("20"), CommerceRules.refundDueAfterReturns(bd("100"), bd("90"), bd("30"), bd("0")))
        assertEquals(bd("5"), CommerceRules.refundDueAfterReturns(bd("100"), bd("90"), bd("30"), bd("15")))
        assertThrows(IllegalArgumentException::class.java) {
            CommerceRules.refundDueAfterReturns(bd("100"), bd("0"), bd("0"), bd("-1"))
        }
    }

    @Test fun stockMovementDirectionsAreSignedCorrectly() {
        val positive = listOf("OPENING", "PURCHASE", "SALE_RETURN", "ADJUSTMENT_IN", "TRANSFER_IN")
        val negative = listOf("SALE", "PURCHASE_RETURN", "ADJUSTMENT_OUT", "TRANSFER_OUT")
        positive.forEach { assertEquals("$it must increase stock", 0, CommerceRules.signedQuantity(it, bd("2.5")).compareTo(bd("2.5"))) }
        negative.forEach { assertEquals("$it must decrease stock", 0, CommerceRules.signedQuantity(it, bd("2.5")).compareTo(bd("-2.5"))) }
        assertEquals(0, CommerceRules.signedQuantity("UNKNOWN", bd("2")).compareTo(bd("0")))
    }

    @Test fun stockMovementRuleRejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException::class.java) { CommerceRules.signedQuantity("SALE", bd("-1")) }
    }

    @Test fun salesReturnAllocatesDiscountAndTaxProportionally() {
        val result = CommerceRules.allocateSalesReturn(
            quantity = bd("1"), lineQuantity = bd("2"), lineTotal = bd("100"),
            lineRevenueBase = bd("250"), invoiceSubtotal = bd("200"), invoiceDiscount = bd("20"),
            invoiceTax = bd("18"), exchangeRateToBase = bd("2.5"),
            transactionCurrencyScale = 2, baseCurrencyScale = 2
        )
        assertEquals(bd("49.50"), result.customerCredit)
        assertEquals(bd("112.50"), result.revenueBase)
    }

    @Test fun fullSalesReturnCreditsDiscountAndTaxOnce() {
        val firstHalf = CommerceRules.allocateSalesReturn(
            bd("1"), bd("2"), bd("100"), bd("250"), bd("200"), bd("20"), bd("18"), bd("2.5"), 2, 2
        )
        val secondHalf = CommerceRules.allocateSalesReturn(
            bd("1"), bd("2"), bd("100"), bd("250"), bd("200"), bd("20"), bd("18"), bd("2.5"), 2, 2
        )
        assertEquals(bd("99.00"), firstHalf.customerCredit + secondHalf.customerCredit)
        assertEquals(bd("225.00"), firstHalf.revenueBase + secondHalf.revenueBase)
    }

    @Test fun salesReturnRejectsQuantityAboveOriginalLine() {
        assertThrows(IllegalArgumentException::class.java) {
            CommerceRules.allocateSalesReturn(
                bd("3"), bd("2"), bd("100"), bd("100"), bd("100"), bd("0"), bd("0"), bd("1"), 2, 2
            )
        }
    }

    @Test fun salesReturnRequiresPositiveExchangeRateAndValidCurrencyScale() {
        assertThrows(IllegalArgumentException::class.java) {
            CommerceRules.allocateSalesReturn(bd("1"), bd("1"), bd("10"), bd("10"), bd("10"), bd("0"), bd("0"), bd("0"), 2, 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CommerceRules.allocateSalesReturn(bd("1"), bd("1"), bd("10"), bd("10"), bd("10"), bd("0"), bd("0"), bd("1"), 9, 2)
        }
    }

    @Test fun successfulSalesReturnNeverHasNegativeRevenue() {
        val result = CommerceRules.allocateSalesReturn(
            bd("1"), bd("1"), bd("10"), bd("10"), bd("10"), bd("10"), bd("0"), bd("3"), 2, 2
        )
        assertTrue(result.revenueBase >= BigDecimal.ZERO)
        assertEquals(bd("0.00"), result.revenueBase)
    }
}
