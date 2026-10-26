package com.sulo.mizan.inventory.core.database

import androidx.room.TypeConverter
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.model.RateType
import java.math.BigDecimal

/**
 * Room Type Converters for Financial Domain Entities.
 *
 * CRITICAL RULE:
 * BigDecimal amounts and rates are persisted as String (plain string) in SQLite.
 * This ensures exact precision without SQLite IEEE 754 float approximations.
 */
class Converters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }

    @TypeConverter
    fun fromCurrency(currency: Currency?): String? {
        return currency?.code
    }

    @TypeConverter
    fun toCurrency(code: String?): Currency? {
        return code?.let { Currency.fromCode(it) }
    }

    @TypeConverter
    fun fromRateType(rateType: RateType?): String? {
        return rateType?.code
    }

    @TypeConverter
    fun toRateType(code: String?): RateType? {
        return code?.let { RateType.fromCode(it) }
    }
}
