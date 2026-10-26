package com.sulo.mizan.inventory.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyConfigDao {

    @Query("SELECT * FROM currency_configs WHERE isActive = 1 ORDER BY sortOrder ASC, code ASC")
    fun observeActiveCurrencies(): Flow<List<CurrencyConfigEntity>>

    @Query("SELECT * FROM currency_configs WHERE isActive = 1 ORDER BY sortOrder ASC, code ASC")
    suspend fun getActiveCurrencies(): List<CurrencyConfigEntity>

    @Query("SELECT * FROM currency_configs WHERE isBase = 1 AND isActive = 1 LIMIT 1")
    fun observeBaseCurrency(): Flow<CurrencyConfigEntity?>

    @Query("SELECT * FROM currency_configs WHERE isBase = 1 AND isActive = 1 LIMIT 1")
    suspend fun getBaseCurrency(): CurrencyConfigEntity?

    @Query("SELECT * FROM currency_configs WHERE code = :code LIMIT 1")
    suspend fun getCurrencyByCode(code: String): CurrencyConfigEntity?

    @Query("SELECT * FROM currency_configs ORDER BY sortOrder ASC, code ASC")
    fun observeAllCurrencies(): Flow<List<CurrencyConfigEntity>>

    @Query("SELECT * FROM currency_configs ORDER BY sortOrder ASC, code ASC")
    suspend fun getAllCurrencies(): List<CurrencyConfigEntity>

    @Query("DELETE FROM currency_configs")
    suspend fun clearAllCurrencies()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: CurrencyConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<CurrencyConfigEntity>)

    @Query("UPDATE currency_configs SET isBase = 0")
    suspend fun resetBaseCurrency()

    @Query("UPDATE currency_configs SET isBase = 1, isActive = 1 WHERE code = :code")
    suspend fun setBaseCurrency(code: String)

    @Transaction
    suspend fun selectBaseCurrencyAtomically(code: String) {
        resetBaseCurrency()
        setBaseCurrency(code)
    }

    @Query("UPDATE currency_configs SET isActive = 0")
    suspend fun deactivateAll()

    @Transaction
    suspend fun updateActiveCurrencies(
        activeEntities: List<CurrencyConfigEntity>,
        baseCurrencyCode: String
    ) {
        deactivateAll()
        resetBaseCurrency()
        val updatedList = activeEntities.mapIndexed { index, entity ->
            entity.copy(
                isActive = true,
                isBase = (entity.code == baseCurrencyCode),
                sortOrder = index + 1,
                updatedAt = System.currentTimeMillis()
            )
        }
        insertOrUpdateAll(updatedList)
    }
}
