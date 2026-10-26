package com.sulo.mizan.inventory.core.repository

import android.content.Context
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.model.RateType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

/**
 * Fetches daily market-reference quotes from the public fawazahmed0 Exchange API.
 * This data is reference-only and must never replace manual BUY / SELL rates.
 */
class AutomaticExchangeRateUpdater(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("mizan_reference_rate_sync", Context.MODE_PRIVATE)
    private val currencyRepository = CurrencyRepository(appContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun updateNow(force: Boolean = false): Int = withContext(Dispatchers.IO) {
        val lastSuccess = prefs.getLong(KEY_LAST_SUCCESS, 0L)
        if (!force && System.currentTimeMillis() - lastSuccess < MIN_REFRESH_INTERVAL_MS) return@withContext 0
        currencyRepository.ensureInitialized()
        val active = currencyRepository.getActiveCurrencies()
        require(active.size in 1..3) { "لا توجد عملات نشطة قابلة للتحديث" }

        val payload = fetchUsdBaseRates()
        val date = payload.first
        val usdRates = payload.second
        val usdRate = BigDecimal.ONE
        val unitRates = mutableMapOf("USD" to usdRate)
        active.filter { it.code != "USD" }.forEach { currency ->
            val raw = usdRates.optString(currency.code.lowercase(), "")
            require(raw.isNotBlank()) { "مصدر أسعار الصرف لا يوفّر ${currency.code}; لم تُغيّر الأسعار اليدوية" }
            val quote = raw.toBigDecimalOrNull()
                ?: throw IllegalStateException("قيمة سعر ${currency.code} غير رقمية في المصدر")
            require(quote > BigDecimal.ZERO) { "سعر ${currency.code} من المصدر غير صالح" }
            unitRates[currency.code] = quote
        }
        // All active currencies must be representable from the USD source for cross-rate derivation.
        active.forEach { currency ->
            if (currency.code !in unitRates) {
                val raw = usdRates.optString(currency.code.lowercase(), "")
                val value = raw.toBigDecimalOrNull()
                    ?: throw IllegalStateException("لا يتوفر سعر مرجعي للعملة ${currency.code}")
                require(value > BigDecimal.ZERO)
                unitRates[currency.code] = value
            }
        }

        var inserted = 0
        for (from in active) {
            for (to in active) {
                if (from.code == to.code) continue
                val rate = unitRates.getValue(to.code).divide(unitRates.getValue(from.code), 12, RoundingMode.HALF_EVEN)
                require(rate > BigDecimal.ZERO) { "حُسب سعر مرجعي غير صالح" }
                val previous = currencyRepository.getLatestRate(from.code, to.code, RateType.REFERENCE)
                val noteMarker = "AUTO_REFERENCE:$date"
                if (previous == null || !previous.sourceOrNote.contains(noteMarker)) {
                    currencyRepository.addExchangeRate(
                        fromCurrency = from,
                        toCurrency = to,
                        rate = rate,
                        rateType = RateType.REFERENCE,
                        sourceOrNote = "$noteMarker; source=fawazahmed0 Exchange API; daily indicative rate only"
                    )
                    inserted++
                }
            }
        }
        prefs.edit().putLong(KEY_LAST_SUCCESS, System.currentTimeMillis()).putString(KEY_SOURCE_DATE, date).apply()
        inserted
    }

    fun lastSuccessfulUpdateMillis(): Long = prefs.getLong(KEY_LAST_SUCCESS, 0L)
    fun lastSourceDate(): String = prefs.getString(KEY_SOURCE_DATE, "") ?: ""

    private fun fetchUsdBaseRates(): Pair<String, JSONObject> {
        val urls = listOf(
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json",
            "https://latest.currency-api.pages.dev/v1/currencies/usd.json"
        )
        var lastError: Exception? = null
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).header("Accept", "application/json").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Exchange API returned HTTP ${response.code}")
                    val body = response.body?.string().orEmpty()
                    val root = JSONObject(body)
                    val date = root.optString("date", "")
                    val usd = root.optJSONObject("usd") ?: throw IOException("Exchange API response misses the USD rates object")
                    require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Exchange API returned an invalid date" }
                    return date to usd
                }
            } catch (exception: Exception) {
                lastError = exception
            }
        }
        throw IOException("تعذر جلب أسعار الصرف المرجعية من المصدرين الأساسي والاحتياطي", lastError)
    }

    companion object {
        private const val KEY_LAST_SUCCESS = "last_success_epoch"
        private const val KEY_SOURCE_DATE = "last_source_date"
        private const val MIN_REFRESH_INTERVAL_MS = 23L * 60L * 60L * 1000L
    }
}
