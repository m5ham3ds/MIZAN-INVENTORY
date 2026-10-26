package com.sulo.mizan.inventory.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sulo.mizan.inventory.core.model.Currency

/**
 * Room entity representing configured currency in Mizan Inventory.
 * Stored in Room to provide an auditable, relational single source of truth.
 *
 * Rules:
 * - Exactly one active currency can be marked as `isBase = true`.
 * - Exactly up to 3 currencies can have `isActive = true`.
 */
@Entity(tableName = "currency_configs")
data class CurrencyConfigEntity(
    @PrimaryKey
    val code: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val symbol: String,
    val decimals: Int,
    val isBase: Boolean,
    val isActive: Boolean,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Currency {
        return Currency(
            code = code,
            displayNameAr = displayNameAr,
            symbol = symbol,
            defaultDecimals = decimals,
            displayNameEn = displayNameEn
        )
    }

    companion object {
        fun fromDomainModel(currency: Currency, isBase: Boolean, isActive: Boolean, sortOrder: Int): CurrencyConfigEntity {
            return CurrencyConfigEntity(
                code = currency.code,
                displayNameAr = currency.displayNameAr,
                displayNameEn = currency.displayNameEn,
                symbol = currency.symbol,
                decimals = currency.defaultDecimals,
                isBase = isBase,
                isActive = isActive,
                sortOrder = sortOrder
            )
        }
    }
}
