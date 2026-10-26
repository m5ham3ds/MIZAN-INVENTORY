package com.sulo.mizan.inventory.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for system configuration.
 */
@Dao
interface SystemConfigDao {

    @Query("SELECT * FROM system_config WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): SystemConfigEntity?

    @Query("SELECT * FROM system_config WHERE `key` = :key LIMIT 1")
    fun observeByKey(key: String): Flow<SystemConfigEntity?>

    @Query("SELECT * FROM system_config ORDER BY `key`")
    suspend fun getAll(): List<SystemConfigEntity>

    @Query("DELETE FROM system_config")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAll(configs: List<SystemConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: SystemConfigEntity)
}
