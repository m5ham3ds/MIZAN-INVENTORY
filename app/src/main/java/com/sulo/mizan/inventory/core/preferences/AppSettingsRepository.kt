package com.sulo.mizan.inventory.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mizan_settings")

/**
 * General application preferences for merchant profile and UI theme.
 * NOTE: Currency financial configuration is strictly stored in Room (CurrencyRepository)
 * as the single auditable source of truth.
 */
data class AppSettings(
    val merchantName: String = "مؤسسة التاجر التجارية",
    val themeMode: String = "SYSTEM"
)

/**
 * DataStore-backed repository for general UI settings and preferences.
 */
class AppSettingsRepository(private val context: Context) {

    private object Keys {
        val MERCHANT_NAME = stringPreferencesKey("merchant_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val appSettingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val merchantName = preferences[Keys.MERCHANT_NAME] ?: "مؤسسة التاجر التجارية"
        val themeMode = preferences[Keys.THEME_MODE] ?: "SYSTEM"
        AppSettings(
            merchantName = merchantName,
            themeMode = themeMode
        )
    }

    suspend fun setMerchantName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MERCHANT_NAME] = name
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode
        }
    }
}
