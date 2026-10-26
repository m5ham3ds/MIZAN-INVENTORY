package com.sulo.mizan.inventory

import com.sulo.mizan.inventory.core.model.ConversionResult
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.model.CurrencyMismatchException
import com.sulo.mizan.inventory.core.model.ExchangeRate
import com.sulo.mizan.inventory.core.model.Money
import com.sulo.mizan.inventory.core.model.RateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Acceptance Tests for Phase 2:
 * Currency Configuration, Manual Exchange Rates & Financial Conversions.
 */
class CurrencyAndExchangeRatePhase2Test {

    @Test
    fun testInitialDefaultCurrenciesAreSdgUsdGbp() {
        val defaults = Currency.DEFAULT_ACTIVE_CURRENCIES
        assertEquals(3, defaults.size)
        assertEquals("SDG", defaults[0].code)
        assertEquals("USD", defaults[1].code)
        assertEquals("GBP", defaults[2].code)

        val baseCurrency = Currency.DEFAULT_BASE_CURRENCY
        assertEquals("SDG", baseCurrency.code)
        assertEquals("جنيه سوداني", baseCurrency.displayNameAr)
        assertEquals("ج.س", baseCurrency.symbol)
        assertEquals(2, baseCurrency.defaultDecimals)
    }

    @Test
    fun testCurrencyCatalogSupportsArabAndGlobalCurrenciesWithCorrectDecimals() {
        val kwd = Currency.KWD
        assertEquals(3, kwd.defaultDecimals)
        assertEquals("د.ك", kwd.symbol)

        val jpy = Currency.JPY
        assertEquals(0, jpy.defaultDecimals)
        assertEquals("¥", jpy.symbol)

        val usd = Currency.USD
        assertEquals(2, usd.defaultDecimals)
        assertEquals("$", usd.symbol)

        val gbp = Currency.GBP
        assertEquals(2, gbp.defaultDecimals)
        assertEquals("£", gbp.symbol)
    }

    @Test
    fun testExchangeRateRejectsZeroAndNegativeValues() {
        assertThrows(IllegalArgumentException::class.java) {
            ExchangeRate(
                fromCurrency = Currency.USD,
                toCurrency = Currency.SDG,
                rate = BigDecimal.ZERO
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ExchangeRate(
                fromCurrency = Currency.USD,
                toCurrency = Currency.SDG,
                rate = BigDecimal("-150.00")
            )
        }
    }

    @Test
    fun testExchangeRateRejectsSameCurrencyUnlessIdentity() {
        assertThrows(IllegalArgumentException::class.java) {
            ExchangeRate(
                fromCurrency = Currency.USD,
                toCurrency = Currency.USD,
                rate = BigDecimal("2.50")
            )
        }

        val identity = ExchangeRate.identity(Currency.USD)
        assertEquals(BigDecimal.ONE, identity.rate)
        assertEquals("USD", identity.fromCurrency.code)
        assertEquals("USD", identity.toCurrency.code)
    }

    @Test
    fun testExchangeRateDirectionDescription() {
        val rate = ExchangeRate(
            fromCurrency = Currency.USD,
            toCurrency = Currency.SDG,
            rate = BigDecimal("2500.0000"),
            rateType = RateType.REFERENCE
        )

        assertEquals("1 USD = 2500 SDG", rate.directionDescription)
    }

    @Test
    fun testDirectCurrencyConversionPrecision() {
        val rate = ExchangeRate(
            fromCurrency = Currency.USD,
            toCurrency = Currency.SDG,
            rate = BigDecimal("2500.50"),
            rateType = RateType.BUY
        )

        val sourceMoney = Money.of("100.00", Currency.USD)
        val converted = rate.convert(sourceMoney)

        assertEquals(Currency.SDG.code, converted.currency.code)
        assertEquals(BigDecimal("250050.00"), converted.amount)
        assertEquals("250050.00 ج.س", converted.formatted())
    }

    @Test
    fun testTargetCurrencyCustomDecimalsRounding() {
        // Convert USD to KWD (3 decimal places)
        val usdToKwdRate = ExchangeRate(
            fromCurrency = Currency.USD,
            toCurrency = Currency.KWD,
            rate = BigDecimal("0.3075")
        )

        val sourceUsd = Money.of("100.00", Currency.USD)
        val convertedKwd = usdToKwdRate.convert(sourceUsd)

        assertEquals(3, convertedKwd.currency.defaultDecimals)
        assertEquals(BigDecimal("30.750"), convertedKwd.amount)
        assertEquals("30.750 د.ك", convertedKwd.formatted())

        // Convert USD to JPY (0 decimal places)
        val usdToJpyRate = ExchangeRate(
            fromCurrency = Currency.USD,
            toCurrency = Currency.JPY,
            rate = BigDecimal("155.65")
        )
        val convertedJpy = usdToJpyRate.convert(sourceUsd)
        assertEquals(0, convertedJpy.currency.defaultDecimals)
        assertEquals(BigDecimal("15565"), convertedJpy.amount)
    }

    @Test
    fun testRateTypeDifferentiationPreventsConflation() {
        val referenceRate = ExchangeRate(
            fromCurrency = Currency.USD,
            toCurrency = Currency.SDG,
            rate = BigDecimal("2500.00"),
            rateType = RateType.REFERENCE
        )

        val buyRate = ExchangeRate(
            fromCurrency = Currency.USD,
            toCurrency = Currency.SDG,
            rate = BigDecimal("2480.00"),
            rateType = RateType.BUY
        )

        val sellRate = ExchangeRate(
            fromCurrency = Currency.USD,
            toCurrency = Currency.SDG,
            rate = BigDecimal("2520.00"),
            rateType = RateType.SELL
        )

        assertEquals(RateType.REFERENCE, referenceRate.rateType)
        assertEquals(RateType.BUY, buyRate.rateType)
        assertEquals(RateType.SELL, sellRate.rateType)

        assertTrue(buyRate.rate < sellRate.rate)
    }

    @Test
    fun testCrossCurrencyConversionMath() {
        // Leg 1: USD -> SDG
        val leg1 = ExchangeRate(
            fromCurrency = Currency.USD,
            toCurrency = Currency.SDG,
            rate = BigDecimal("2500.00")
        )
        // Leg 2: SDG -> GBP
        val leg2 = ExchangeRate(
            fromCurrency = Currency.SDG,
            toCurrency = Currency.GBP,
            rate = BigDecimal("0.00032000")
        )

        val usdMoney = Money.of("100.00", Currency.USD)
        val sdgMoney = leg1.convert(usdMoney)
        assertEquals(BigDecimal("250000.00"), sdgMoney.amount)

        val gbpMoney = leg2.convert(sdgMoney)
        assertEquals(BigDecimal("80.00"), gbpMoney.amount)
        assertEquals("80.00 £", gbpMoney.formatted())
    }

    @Test
    fun testDirectAdditionOfDifferentCurrenciesStillBlocked() {
        val sdg = Money.of("1000.00", Currency.SDG)
        val usd = Money.of("10.00", Currency.USD)

        assertThrows(CurrencyMismatchException::class.java) {
            val ignored = sdg + usd
        }
    }
}
