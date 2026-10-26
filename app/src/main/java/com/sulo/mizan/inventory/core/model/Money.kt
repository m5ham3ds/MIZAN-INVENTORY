package com.sulo.mizan.inventory.core.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money Value Object for Mizan Inventory.
 *
 * ARCHITECTURAL MANDATE:
 * Absolutely NO Float or Double types are allowed for financial values.
 * All monetary amounts are represented using [BigDecimal] with explicit scale
 * and [RoundingMode.HALF_EVEN] (Banker's rounding) or [RoundingMode.HALF_UP].
 *
 * Direct addition or subtraction of different currencies is strictly forbidden
 * and throws [CurrencyMismatchException].
 */
data class Money(
    val amount: BigDecimal,
    val currency: Currency
) : Comparable<Money> {

    init {
        require(amount.scale() <= 8) { "Monetary scale cannot exceed 8 decimal places: ${amount.scale()}" }
    }

    constructor(amountString: String, currency: Currency) : this(
        BigDecimal(amountString),
        currency
    )

    constructor(amountLong: Long, currency: Currency) : this(
        BigDecimal.valueOf(amountLong),
        currency
    )

    fun scaled(
        scale: Int = currency.defaultDecimals,
        roundingMode: RoundingMode = DEFAULT_ROUNDING
    ): Money {
        return Money(amount.setScale(scale, roundingMode), currency)
    }

    operator fun plus(other: Money): Money {
        checkSameCurrency(other)
        return Money(this.amount.add(other.amount), this.currency)
    }

    operator fun minus(other: Money): Money {
        checkSameCurrency(other)
        return Money(this.amount.subtract(other.amount), this.currency)
    }

    operator fun times(factor: BigDecimal): Money {
        return Money(this.amount.multiply(factor), this.currency)
    }

    operator fun times(quantity: Long): Money {
        return Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency)
    }

    operator fun div(divisor: BigDecimal): Money {
        // BigDecimal.equals is scale-sensitive (0.0 != 0.00), so use compareTo
        // to reject every representation of numeric zero consistently.
        require(divisor.compareTo(BigDecimal.ZERO) != 0) {
            "Division by zero is not allowed"
        }
        return Money(this.amount.divide(divisor, 8, DEFAULT_ROUNDING), this.currency)
    }

    val isZero: Boolean
        get() = amount.compareTo(BigDecimal.ZERO) == 0

    val isPositive: Boolean
        get() = amount > BigDecimal.ZERO

    val isNegative: Boolean
        get() = amount < BigDecimal.ZERO

    override fun compareTo(other: Money): Int {
        checkSameCurrency(other)
        return this.amount.compareTo(other.amount)
    }

    private fun checkSameCurrency(other: Money) {
        if (this.currency.code != other.currency.code) {
            throw CurrencyMismatchException(
                "Cannot perform direct calculation between ${this.currency.code} and ${other.currency.code}. " +
                "Currency exchange conversion must be applied explicitly."
            )
        }
    }

    fun formatted(includeSymbol: Boolean = true): String {
        val scaled = amount.setScale(currency.defaultDecimals, DEFAULT_ROUNDING)
        return if (includeSymbol) {
            "${scaled.toPlainString()} ${currency.symbol}"
        } else {
            scaled.toPlainString()
        }
    }

    companion object {
        val DEFAULT_ROUNDING: RoundingMode = RoundingMode.HALF_EVEN

        fun zero(currency: Currency): Money = Money(BigDecimal.ZERO, currency)

        fun of(amount: String, currency: Currency): Money = Money(BigDecimal(amount), currency)
    }
}

class CurrencyMismatchException(message: String) : IllegalArgumentException(message)
