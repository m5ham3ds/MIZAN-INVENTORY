package com.sulo.mizan.inventory.core.repository

import android.content.Context
import com.sulo.mizan.inventory.core.database.AppDatabase
import com.sulo.mizan.inventory.core.database.CurrencyConfigDao
import com.sulo.mizan.inventory.core.database.CurrencyConfigEntity
import com.sulo.mizan.inventory.core.database.ExchangeRateDao
import com.sulo.mizan.inventory.core.database.ExchangeRateEntity
import com.sulo.mizan.inventory.core.model.ConversionResult
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.model.ExchangeRate
import com.sulo.mizan.inventory.core.model.Money
import com.sulo.mizan.inventory.core.model.RateType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Single Authoritative Source of Truth for Currency Configuration and Exchange Rates.
 * Backed by Room database (AppDatabase) for full auditability, persistence, and consistency.
 */
class CurrencyRepository(
    private val currencyDao: CurrencyConfigDao,
    private val exchangeRateDao: ExchangeRateDao,
    private val baseCurrencyChangeGuard: suspend (newBaseCode: String) -> Unit = {}
) {

    constructor(context: Context) : this(
        AppDatabase.getInstance(context).currencyConfigDao(),
        AppDatabase.getInstance(context).exchangeRateDao(),
        baseCurrencyChangeGuard = { newBaseCode ->
            val commerce = AppDatabase.getInstance(context.applicationContext).commerceDao()
            val hasProductValuation = commerce.getProducts().isNotEmpty()
            val hasPostedCommerce = commerce.getPurchaseInvoices().isNotEmpty() ||
                commerce.getSalesInvoices().isNotEmpty() ||
                commerce.getExpenses().isNotEmpty() ||
                commerce.getMovements().any { it.movementType != "OPENING" }
            require(!hasProductValuation && !hasPostedCommerce) {
                "لا يمكن تغيير العملة الأساسية إلى $newBaseCode بعد إضافة أصناف أو تسجيل حركات مالية؛ ذلك يتطلب إعادة تقييم مدروسة حتى لا تختلط العملات في التقارير."
            }
        }
    )

    /**
     * Observes the currently active currencies (maximum 3), sorted by sortOrder.
     */
    val activeCurrenciesFlow: Flow<List<Currency>> = currencyDao.observeActiveCurrencies().map { list ->
        if (list.isEmpty()) {
            Currency.DEFAULT_ACTIVE_CURRENCIES
        } else {
            list.map { it.toDomainModel() }
        }
    }

    /**
     * Observes the designated primary base currency for reporting.
     */
    val baseCurrencyFlow: Flow<Currency> = currencyDao.observeBaseCurrency().map { entity ->
        entity?.toDomainModel() ?: Currency.DEFAULT_BASE_CURRENCY
    }

    /**
     * Observes all exchange rate history sorted by newest first.
     */
    val exchangeRateHistoryFlow: Flow<List<ExchangeRate>> = exchangeRateDao.observeAllHistory().map { list ->
        list.map { it.toDomainModel() }
    }

    /**
     * Ensures initial default currencies are seeded into database if empty.
     */
    suspend fun ensureInitialized() {
        val currentActive = currencyDao.getActiveCurrencies()
        if (currentActive.isEmpty()) {
            val defaults = listOf(
                CurrencyConfigEntity.fromDomainModel(Currency.SDG, isBase = true, isActive = true, sortOrder = 1),
                CurrencyConfigEntity.fromDomainModel(Currency.USD, isBase = false, isActive = true, sortOrder = 2),
                CurrencyConfigEntity.fromDomainModel(Currency.GBP, isBase = false, isActive = true, sortOrder = 3)
            )
            currencyDao.updateActiveCurrencies(defaults, Currency.SDG.code)
        }
    }

    /**
     * Retrieves active currencies directly.
     */
    suspend fun getActiveCurrencies(): List<Currency> {
        val list = currencyDao.getActiveCurrencies()
        return if (list.isEmpty()) {
            ensureInitialized()
            Currency.DEFAULT_ACTIVE_CURRENCIES
        } else {
            list.map { it.toDomainModel() }
        }
    }

    /**
     * Retrieves the base currency directly.
     */
    suspend fun getBaseCurrency(): Currency {
        val base = currencyDao.getBaseCurrency()
        return base?.toDomainModel() ?: run {
            ensureInitialized()
            Currency.DEFAULT_BASE_CURRENCY
        }
    }

    /**
     * Updates the active currencies.
     *
     * CONSTRAINTS:
     * - Must contain between 1 and 3 currencies (strictly max 3).
     * - Base currency must be included in the active list.
     * - Historical exchange rates of deactivated currencies are safely preserved.
     */
    suspend fun setActiveCurrencies(newActiveCodes: List<String>, baseCurrencyCode: String) {
        val distinctCodes = newActiveCodes.map { it.trim().uppercase() }.distinct()
        val normalizedBaseCode = baseCurrencyCode.trim().uppercase()
        require(distinctCodes.isNotEmpty() && distinctCodes.size <= 3) {
            "يجب تحديد 3 عملات نشطة كحد أقصى (تم تحديد ${distinctCodes.size})"
        }
        require(distinctCodes.all { code -> Currency.ALL_SUPPORTED_CURRENCIES.any { it.code == code } }) {
            "تتضمن قائمة العملات رمزًا غير مدعوم"
        }
        require(distinctCodes.contains(normalizedBaseCode)) {
            "يجب أن تكون العملة الأساسية ($normalizedBaseCode) إحدى العملات النشطة المختارة"
        }
        val currentBaseCode = getBaseCurrency().code
        if (currentBaseCode != normalizedBaseCode) {
            // Apply the same accounting safety rule whether base currency changes through the
            // dedicated selector or through the active-currency configuration screen.
            baseCurrencyChangeGuard(normalizedBaseCode)
        }

        val entitiesToSave = distinctCodes.mapIndexed { index, code ->
            val currency = Currency.fromCode(code)
            CurrencyConfigEntity.fromDomainModel(
                currency = currency,
                isBase = (code == normalizedBaseCode),
                isActive = true,
                sortOrder = index + 1
            )
        }

        currencyDao.updateActiveCurrencies(entitiesToSave, normalizedBaseCode)
    }

    /**
     * Sets a new base currency among active currencies.
     */
    suspend fun setBaseCurrency(code: String) {
        val normalizedCode = code.trim().uppercase()
        require(Currency.ALL_SUPPORTED_CURRENCIES.any { it.code == normalizedCode }) {
            "رمز العملة الأساسية غير مدعوم: $normalizedCode"
        }
        val active = getActiveCurrencies()
        require(active.any { it.code == normalizedCode }) {
            "لا يمكن تعيين العملة $normalizedCode كأساسية لأنها ليست من العملات النشطة حالياً"
        }
        if (getBaseCurrency().code == normalizedCode) return
        // Changing the base after products or posted entries exist would mix historical cost
        // snapshots denominated in different base currencies. Require a controlled migration
        // rather than silently corrupting financial reports.
        baseCurrencyChangeGuard(normalizedCode)
        currencyDao.selectBaseCurrencyAtomically(normalizedCode)
    }

    /**
     * Records a new manual exchange rate.
     * Creates an immutable record in the history log without overwriting past rates.
     */
    suspend fun addExchangeRate(
        fromCurrency: Currency,
        toCurrency: Currency,
        rate: BigDecimal,
        rateType: RateType = RateType.REFERENCE,
        sourceOrNote: String = ""
    ): ExchangeRate {
        require(rate > BigDecimal.ZERO) { "يجب أن يكون سعر الصرف أكبر من الصفر تماماً" }
        require(fromCurrency.code != toCurrency.code) { "لا يمكن إدخال سعر صرف لعملة مع نفسها" }

        val entity = ExchangeRateEntity(
            fromCurrencyCode = fromCurrency.code,
            toCurrencyCode = toCurrency.code,
            rate = rate,
            rateType = rateType,
            timestamp = System.currentTimeMillis(),
            sourceOrNote = sourceOrNote.trim()
        )

        val id = exchangeRateDao.insertRate(entity)
        return entity.copy(id = id).toDomainModel(fromCurrency, toCurrency)
    }

    /**
     * Finds the latest rate for a specific pair and rate type.
     */
    suspend fun getLatestRate(
        fromCode: String,
        toCode: String,
        rateType: RateType? = null
    ): ExchangeRate? {
        val entity = if (rateType != null) {
            exchangeRateDao.getLatestRate(fromCode, toCode, rateType)
        } else {
            exchangeRateDao.getLatestRateAnyType(fromCode, toCode)
        }
        return entity?.toDomainModel()
    }

    /**
     * Observes history for a specific pair.
     */
    fun observePairHistory(fromCode: String, toCode: String): Flow<List<ExchangeRate>> {
        return exchangeRateDao.observePairHistory(fromCode, toCode).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    /**
     * Strict Financial Conversion Engine.
     *
     * Rules:
     * 1. If from == to, returns 1:1 match.
     * 2. Checks the direct rate for the exact requested rate type; it never substitutes a different type.
     * 3. If no direct rate, attempts documented cross-rate conversion via base currency.
     * 4. Does NOT invert buy/sell rates with spreads.
     * 5. Throws [MissingExchangeRateException] if required rate(s) are missing.
     */
    suspend fun convertCurrency(
        money: Money,
        toCurrency: Currency,
        preferredRateType: RateType = RateType.REFERENCE
    ): ConversionResult {
        val fromCurrency = money.currency

        // Case 1: Identity conversion
        if (fromCurrency.code == toCurrency.code) {
            return ConversionResult(
                sourceMoney = money,
                targetMoney = money,
                effectiveRate = BigDecimal.ONE,
                rateType = preferredRateType,
                isCrossRate = false,
                calculationDetailsAr = "تحويل متطابق (نفس العملة ${fromCurrency.code})"
            )
        }

        // Case 2: Direct rate lookup
        // Never silently substitute BUY/SELL/TRANSACTIONAL rates for the requested rate type.
        val directRate = getLatestRate(fromCurrency.code, toCurrency.code, preferredRateType)

        if (directRate != null) {
            val convertedMoney = directRate.convert(money, toCurrency.defaultDecimals)
            val details = "تحويل مباشر بسعر ${directRate.rateType.displayNameAr}: 1 ${fromCurrency.code} = ${directRate.rate.stripTrailingZeros().toPlainString()} ${toCurrency.code}"
            return ConversionResult(
                sourceMoney = money,
                targetMoney = money.copy(amount = convertedMoney.amount, currency = toCurrency),
                effectiveRate = directRate.rate,
                rateType = directRate.rateType,
                isCrossRate = false,
                calculationDetailsAr = details
            )
        }

        // Case 3: Cross-rate via Base Currency
        val baseCurrency = getBaseCurrency()
        if (fromCurrency.code != baseCurrency.code && toCurrency.code != baseCurrency.code) {
            val leg1Rate = getLatestRate(fromCurrency.code, baseCurrency.code, preferredRateType)
            val leg2Rate = getLatestRate(baseCurrency.code, toCurrency.code, preferredRateType)

            if (leg1Rate != null && leg2Rate != null) {
                // Compose the rates without rounding the intermediate currency. Round only once,
                // at the final target currency, to avoid cumulative conversion error.
                val combinedEffectiveRate = leg1Rate.rate.multiply(leg2Rate.rate)
                val finalAmount = money.amount
                    .multiply(combinedEffectiveRate)
                    .setScale(toCurrency.defaultDecimals, RoundingMode.HALF_EVEN)
                val finalMoney = Money(finalAmount, toCurrency)

                val details = "تحويل تقاطعي عبر العملة الأساسية (${baseCurrency.code}): " +
                        "الخطوة 1: ${leg1Rate.directionDescription} | " +
                        "الخطوة 2: ${leg2Rate.directionDescription}"

                return ConversionResult(
                    sourceMoney = money,
                    targetMoney = finalMoney,
                    effectiveRate = combinedEffectiveRate,
                    rateType = preferredRateType,
                    isCrossRate = true,
                    intermediateCurrency = baseCurrency,
                    firstLegRate = leg1Rate,
                    secondLegRate = leg2Rate,
                    calculationDetailsAr = details
                )
            }
        }

        // Case 4: No valid rate found
        throw MissingExchangeRateException(
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            requiredRateType = preferredRateType,
            message = "لا يتوفر سعر صرف من النوع ${preferredRateType.displayNameAr} للتحويل من ${fromCurrency.code} إلى ${toCurrency.code}. أضف سعرًا من النوع المطلوب أولًا؛ لا يتم استخدام سعر شراء/بيع مختلف تلقائيًا."
        )
    }
}

class MissingExchangeRateException(
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val requiredRateType: RateType? = null,
    message: String = buildString {
        append("لا يتوفر سعر صرف مسجل للتحويل من ${fromCurrency.code} إلى ${toCurrency.code}")
        requiredRateType?.let { append(" من النوع ${it.displayNameAr}") }
    }
) : IllegalStateException(message)
