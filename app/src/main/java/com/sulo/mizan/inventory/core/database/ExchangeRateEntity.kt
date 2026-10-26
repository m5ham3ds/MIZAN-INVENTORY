package com.sulo.mizan.inventory.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.model.ExchangeRate
import com.sulo.mizan.inventory.core.model.RateType
import java.math.BigDecimal

/**
 * Room entity representing an exchange rate record.
 * Designed to preserve an immutable historical log of all entered exchange rates.
 */
@Entity(
    tableName = "exchange_rates",
    indices = [
        Index(value = ["fromCurrencyCode", "toCurrencyCode"]),
        Index(value = ["timestamp"]),
        Index(value = ["rateType"])
    ]
)
data class ExchangeRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fromCurrencyCode: String,
    val toCurrencyCode: String,
    val rate: BigDecimal,
    val rateType: RateType,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceOrNote: String = ""
) {
    fun toDomainModel(fromCurrency: Currency? = null, toCurrency: Currency? = null): ExchangeRate {
        val resolvedFrom = fromCurrency ?: Currency.fromCode(fromCurrencyCode)
        val resolvedTo = toCurrency ?: Currency.fromCode(toCurrencyCode)
        return ExchangeRate(
            id = id,
            fromCurrency = resolvedFrom,
            toCurrency = resolvedTo,
            rate = rate,
            rateType = rateType,
            timestamp = timestamp,
            sourceOrNote = sourceOrNote
        )
    }

    companion object {
        fun fromDomainModel(rate: ExchangeRate): ExchangeRateEntity {
            return ExchangeRateEntity(
                id = rate.id,
                fromCurrencyCode = rate.fromCurrency.code,
                toCurrencyCode = rate.toCurrency.code,
                rate = rate.rate,
                rateType = rate.rateType,
                timestamp = rate.timestamp,
                sourceOrNote = rate.sourceOrNote
            )
        }
    }
}
