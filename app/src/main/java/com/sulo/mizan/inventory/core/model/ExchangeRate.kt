package com.sulo.mizan.inventory.core.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * ExchangeRate Value Object for Mizan Inventory.
 *
 * Encapsulates the conversion rate from [fromCurrency] to [toCurrency].
 * Rates are strictly stored as positive [BigDecimal] values.
 *
 * DIRECTION DEFINITION:
 * Rate defines how many units of [toCurrency] equal ONE unit of [fromCurrency].
 * Example: 1 USD = 2,500.00 SDG is represented as:
 *   fromCurrency = USD
 *   toCurrency   = SDG
 *   rate         = 2500.00
 *
 * CONSTRAINTS:
 * - Rate must be strictly positive (rate > 0). Zero or negative rates throw [IllegalArgumentException].
 * - Standard exchange rates must have different currencies (fromCurrency != toCurrency).
 * - Rate type must be explicitly defined (REFERENCE, BUY, SELL, TRANSACTIONAL).
 */
data class ExchangeRate(
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val rate: BigDecimal,
    val rateType: RateType = RateType.REFERENCE,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceOrNote: String = "",
    val id: Long = 0L
) {
    init {
        require(rate > BigDecimal.ZERO) { "Exchange rate must be strictly positive: $rate" }
        if (fromCurrency.code == toCurrency.code) {
            require(rate.compareTo(BigDecimal.ONE) == 0) {
                "Exchange rate between the same currency (${fromCurrency.code}) must equal 1.0"
            }
        }
    }

    /**
     * Human-readable unit direction description.
     * Example: "1 USD = 2,500.00 SDG"
     */
    val directionDescription: String
        get() = "1 ${fromCurrency.code} = ${rate.stripTrailingZeros().toPlainString()} ${toCurrency.code}"

    /**
     * Executes explicit currency conversion from [fromCurrency] to [toCurrency].
     * Strict banker's rounding (HALF_EVEN) is applied according to target currency scale.
     */
    fun convert(money: Money, scale: Int = toCurrency.defaultDecimals): Money {
        require(money.currency.code == fromCurrency.code) {
            "Money currency (${money.currency.code}) does not match exchange rate source currency (${fromCurrency.code})"
        }
        val convertedAmount = money.amount.multiply(rate).setScale(scale, RoundingMode.HALF_EVEN)
        return Money(convertedAmount, toCurrency)
    }

    companion object {
        fun identity(currency: Currency): ExchangeRate =
            ExchangeRate(
                fromCurrency = currency,
                toCurrency = currency,
                rate = BigDecimal.ONE,
                rateType = RateType.REFERENCE,
                sourceOrNote = "سعر تطابق العملة الذاتية"
            )
    }
}

/**
 * Result of a currency conversion calculation, documenting the exact rate(s) and path taken.
 */
data class ConversionResult(
    val sourceMoney: Money,
    val targetMoney: Money,
    val effectiveRate: BigDecimal,
    val rateType: RateType,
    val isCrossRate: Boolean = false,
    val intermediateCurrency: Currency? = null,
    val firstLegRate: ExchangeRate? = null,
    val secondLegRate: ExchangeRate? = null,
    val calculationDetailsAr: String = ""
)
