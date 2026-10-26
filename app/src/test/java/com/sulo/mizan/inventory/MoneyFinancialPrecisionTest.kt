package com.sulo.mizan.inventory

import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.model.CurrencyMismatchException
import com.sulo.mizan.inventory.core.model.ExchangeRate
import com.sulo.mizan.inventory.core.model.Money
import com.sulo.mizan.inventory.core.model.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class MoneyFinancialPrecisionTest {

    @Test
    fun testBigDecimalPrecisionEliminatesBinaryFloatingPointErrors() {
        // In IEEE 754 double: 0.1 + 0.2 != 0.3 (results in 0.30000000000000004)
        val m1 = Money.of("0.1", Currency.SAR)
        val m2 = Money.of("0.2", Currency.SAR)
        val sum = m1 + m2

        assertEquals(BigDecimal("0.3"), sum.amount)
        assertEquals("0.30 ر.س", sum.formatted())
    }

    @Test
    fun testCurrencyMismatchThrowsExceptionOnDirectAddition() {
        val yerMoney = Money.of("1000.00", Currency.YER)
        val usdMoney = Money.of("10.00", Currency.USD)

        assertThrows(CurrencyMismatchException::class.java) {
            val ignored = yerMoney + usdMoney
        }
    }

    @Test
    fun testCurrencyMismatchThrowsExceptionOnDirectSubtraction() {
        val sarMoney = Money.of("500.00", Currency.SAR)
        val usdMoney = Money.of("100.00", Currency.USD)

        assertThrows(CurrencyMismatchException::class.java) {
            val ignored = sarMoney - usdMoney
        }
    }

    @Test
    fun testMoneyScalingAndRounding() {
        val money = Money(BigDecimal("125.4567"), Currency.YER)
        val scaled = money.scaled(scale = 2, roundingMode = RoundingMode.HALF_EVEN)

        assertEquals(BigDecimal("125.46"), scaled.amount)
        assertEquals("125.46 ر.ي", scaled.formatted())
    }

    @Test
    fun testMoneyMultiplicationWithFactorAndQuantity() {
        val unitPrice = Money.of("45.50", Currency.SAR)
        val total = unitPrice * 4L

        assertEquals(BigDecimal("182.00"), total.amount)
        assertEquals("182.00 ر.س", total.formatted())
    }

    @Test
    fun testExchangeRateConversion() {
        val usd = Money.of("100.00", Currency.USD)
        val rateUsdToYer = ExchangeRate(
            fromCurrency = Currency.USD,
            toCurrency = Currency.YER,
            rate = BigDecimal("250.0000")
        )

        val convertedYer = rateUsdToYer.convert(usd)
        assertEquals(Currency.YER.code, convertedYer.currency.code)
        assertEquals(BigDecimal("25000.00"), convertedYer.amount)
    }

    @Test
    fun testQuantityValidation() {
        val q1 = Quantity("10.5", "كيلوجرام")
        val q2 = Quantity("4.5", "كيلوجرام")
        val total = q1 + q2

        assertEquals(BigDecimal("15.0"), total.value)

        // Negative stock prevention
        assertThrows(IllegalArgumentException::class.java) {
            val ignored = q2 - q1
        }
    }

    @Test
    fun testMoneyDivisionRejectsZeroRegardlessOfBigDecimalScale() {
        val amount = Money.of("10.00", Currency.USD)
        assertThrows(IllegalArgumentException::class.java) {
            amount / BigDecimal("0.0")
        }
        assertThrows(IllegalArgumentException::class.java) {
            amount / BigDecimal("0.000")
        }
    }

    @Test
    fun testQuantityComparisonRejectsDifferentUnits() {
        val pieces = Quantity("2", "قطعة")
        val kilograms = Quantity("2", "كيلوجرام")

        assertThrows(IllegalArgumentException::class.java) {
            pieces.compareTo(kilograms)
        }
    }
}
