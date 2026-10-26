package com.sulo.mizan.inventory.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sulo.mizan.inventory.core.model.RateType
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: ExchangeRateEntity): Long

    @Query("SELECT * FROM exchange_rates ORDER BY timestamp DESC")
    fun observeAllHistory(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE fromCurrencyCode = :fromCode AND toCurrencyCode = :toCode ORDER BY timestamp DESC")
    fun observePairHistory(fromCode: String, toCode: String): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE fromCurrencyCode = :fromCode AND toCurrencyCode = :toCode AND rateType = :rateType ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRate(fromCode: String, toCode: String, rateType: RateType): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates WHERE fromCurrencyCode = :fromCode AND toCurrencyCode = :toCode ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRateAnyType(fromCode: String, toCode: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates ORDER BY timestamp DESC")
    suspend fun getAllHistoryDirect(): List<ExchangeRateEntity>

    @Query("SELECT COUNT(*) FROM exchange_rates")
    suspend fun getCount(): Int

    @Query("DELETE FROM exchange_rates")
    suspend fun clearAllRates()
}
