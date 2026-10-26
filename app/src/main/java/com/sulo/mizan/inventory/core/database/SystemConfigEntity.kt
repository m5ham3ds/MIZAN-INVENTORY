package com.sulo.mizan.inventory.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * System configuration entity for initial setup and system metadata.
 */
@Entity(tableName = "system_config")
data class SystemConfigEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)
