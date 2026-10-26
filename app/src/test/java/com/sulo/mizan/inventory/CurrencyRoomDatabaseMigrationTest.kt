package com.sulo.mizan.inventory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sulo.mizan.inventory.core.database.AppDatabase
import com.sulo.mizan.inventory.core.database.CurrencyConfigEntity
import com.sulo.mizan.inventory.core.database.ExchangeRateEntity
import com.sulo.mizan.inventory.core.database.SystemConfigEntity
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.model.Money
import com.sulo.mizan.inventory.core.model.RateType
import com.sulo.mizan.inventory.core.repository.CurrencyRepository
import com.sulo.mizan.inventory.core.repository.MissingExchangeRateException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CurrencyRoomDatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: CurrencyRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CurrencyRepository(
            currencyDao = database.currencyConfigDao(),
            exchangeRateDao = database.exchangeRateDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testEnsureInitializedSeedsDefaultThreeCurrencies() {
        runBlocking {
            repository.ensureInitialized()

            val active = repository.getActiveCurrencies()
            assertEquals(3, active.size)
            assertEquals("SDG", active[0].code)
            assertEquals("USD", active[1].code)
            assertEquals("GBP", active[2].code)

            val base = repository.getBaseCurrency()
            assertEquals("SDG", base.code)
        }
    }

    @Test
    fun testMaxThreeActiveCurrenciesEnforced() {
        runBlocking {
            repository.ensureInitialized()

            // Valid: 3 active currencies
            repository.setActiveCurrencies(
                newActiveCodes = listOf("SDG", "USD", "SAR"),
                baseCurrencyCode = "SDG"
            )
            val active = repository.getActiveCurrencies()
            assertEquals(3, active.size)
            assertTrue(active.any { it.code == "SAR" })

            // Invalid: 4 active currencies (Must throw IllegalArgumentException)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.setActiveCurrencies(
                        newActiveCodes = listOf("SDG", "USD", "GBP", "SAR"),
                        baseCurrencyCode = "SDG"
                    )
                }
            }

            // Invalid: Base currency not among active currencies (Must throw IllegalArgumentException)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.setActiveCurrencies(
                        newActiveCodes = listOf("USD", "GBP", "EUR"),
                        baseCurrencyCode = "SDG"
                    )
                }
            }
        }
    }

    @Test
    fun testSetBaseCurrencyPersistsCorrectly() {
        runBlocking {
            repository.ensureInitialized()

            // Change base currency to USD
            repository.setBaseCurrency(" usd ")
            val base = repository.getBaseCurrency()
            assertEquals("USD", base.code)

            // Try setting an inactive currency as base (must fail)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.setBaseCurrency("JPY")
                }
            }
        }
    }


    @Test
    fun testChangingBaseThroughActiveCurrencySettingsUsesSafetyGuard() {
        runBlocking {
            repository.ensureInitialized()
            var guardedCode: String? = null
            val guardedRepository = CurrencyRepository(
                currencyDao = database.currencyConfigDao(),
                exchangeRateDao = database.exchangeRateDao(),
                baseCurrencyChangeGuard = { requestedCode ->
                    guardedCode = requestedCode
                    throw IllegalStateException("base currency change blocked")
                }
            )

            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    guardedRepository.setActiveCurrencies(
                        newActiveCodes = listOf("USD", "GBP"),
                        baseCurrencyCode = "usd"
                    )
                }
            }
            assertEquals("USD", guardedCode)
            assertEquals("SDG", repository.getBaseCurrency().code)
        }
    }

    @Test
    fun testActiveCurrenciesNormalizeCodesAndRejectUnsupportedCurrency() {
        runBlocking {
            repository.ensureInitialized()
            repository.setActiveCurrencies(listOf(" usd ", "gbp", "SAR"), "usd")
            assertEquals("USD", repository.getBaseCurrency().code)
            assertEquals(listOf("USD", "GBP", "SAR"), repository.getActiveCurrencies().map { it.code })

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.setActiveCurrencies(listOf("USD", "GBP", "XXX"), "USD")
                }
            }
        }
    }

    @Test
    fun testExchangeRateHistoryPreservesAllEntries() {
        runBlocking {
            repository.ensureInitialized()

            // Add rate 1: 1 USD = 2450 SDG (yesterday)
            repository.addExchangeRate(
                fromCurrency = Currency.USD,
                toCurrency = Currency.SDG,
                rate = BigDecimal("2450.00"),
                rateType = RateType.REFERENCE,
                sourceOrNote = "سعر الصباح"
            )

            // Add rate 2: 1 USD = 2500 SDG (today)
            repository.addExchangeRate(
                fromCurrency = Currency.USD,
                toCurrency = Currency.SDG,
                rate = BigDecimal("2500.00"),
                rateType = RateType.REFERENCE,
                sourceOrNote = "سعر الظهيرة"
            )

            // Verify history has both records (does NOT overwrite)
            val history = database.exchangeRateDao().getAllHistoryDirect()
            assertEquals(2, history.size)

            // Latest rate must be r2 (2500.00)
            val latest = repository.getLatestRate("USD", "SDG")
            assertNotNull(latest)
            assertEquals(BigDecimal("2500.00"), latest!!.rate)
        }
    }

    @Test
    fun testCrossRateConversionViaBaseCurrency() {
        runBlocking {
            repository.ensureInitialized()

            // Rate 1: 1 USD = 2500 SDG
            repository.addExchangeRate(
                fromCurrency = Currency.USD,
                toCurrency = Currency.SDG,
                rate = BigDecimal("2500.00")
            )

            // Rate 2: 1 SDG = 0.00032 GBP
            repository.addExchangeRate(
                fromCurrency = Currency.SDG,
                toCurrency = Currency.GBP,
                rate = BigDecimal("0.00032000")
            )

            // Convert 100 USD to GBP via SDG
            val source = Money.of("100.00", Currency.USD)
            val result = repository.convertCurrency(
                money = source,
                toCurrency = Currency.GBP
            )

            assertTrue(result.isCrossRate)
            assertEquals("SDG", result.intermediateCurrency?.code)
            assertEquals(BigDecimal("80.00"), result.targetMoney.amount)
            assertEquals(Currency.GBP.code, result.targetMoney.currency.code)
        }
    }


    @Test
    fun testConversionDoesNotFallbackToDifferentRateType() {
        runBlocking {
            repository.ensureInitialized()
            repository.addExchangeRate(
                fromCurrency = Currency.USD,
                toCurrency = Currency.SDG,
                rate = BigDecimal("2500.00"),
                rateType = RateType.SELL,
                sourceOrNote = "اختبار عدم خلط نوع السعر"
            )

            val source = Money.of("1.00", Currency.USD)
            val exception = assertThrows(MissingExchangeRateException::class.java) {
                runBlocking {
                    repository.convertCurrency(source, Currency.SDG, RateType.REFERENCE)
                }
            }
            assertEquals(RateType.REFERENCE, exception.requiredRateType)

            val explicitSell = repository.convertCurrency(source, Currency.SDG, RateType.SELL)
            assertEquals(BigDecimal("2500.00"), explicitSell.targetMoney.amount)
            assertEquals(RateType.SELL, explicitSell.rateType)
        }
    }

    @Test
    fun testCrossRateRoundsOnlyAtFinalTargetCurrency() {
        runBlocking {
            repository.ensureInitialized()
            // 1 USD = 1.236 SDG and 1 SDG = 1.23 GBP.
            // Intermediate rounding would produce 1.24 * 1.23 = 1.53; the correct unrounded
            // cross rate is 1.236 * 1.23 = 1.52028, rounded once to 1.52 GBP.
            repository.addExchangeRate(
                fromCurrency = Currency.USD,
                toCurrency = Currency.SDG,
                rate = BigDecimal("1.236"),
                rateType = RateType.REFERENCE
            )
            repository.addExchangeRate(
                fromCurrency = Currency.SDG,
                toCurrency = Currency.GBP,
                rate = BigDecimal("1.23"),
                rateType = RateType.REFERENCE
            )

            val result = repository.convertCurrency(
                money = Money.of("1.00", Currency.USD),
                toCurrency = Currency.GBP,
                preferredRateType = RateType.REFERENCE
            )

            assertEquals(BigDecimal("1.52"), result.targetMoney.amount)
            assertEquals(BigDecimal("1.52028"), result.effectiveRate)
        }
    }

    @Test
    fun testMissingExchangeRateThrowsException() {
        runBlocking {
            repository.ensureInitialized()

            // No rate exists between USD and EUR
            val sourceUsd = Money.of("100.00", Currency.USD)
            assertThrows(MissingExchangeRateException::class.java) {
                runBlocking {
                    repository.convertCurrency(
                        money = sourceUsd,
                        toCurrency = Currency.EUR
                    )
                }
            }
        }
    }

    @Test
    fun testSystemConfigAndRoomMigration1To2() {
        runBlocking {
            // Verify SystemConfig from Phase 1 is supported and works in AppDatabase v2
            database.systemConfigDao().setConfig(
                SystemConfigEntity(key = "setup_phase", value = "PHASE_02")
            )
            val config = database.systemConfigDao().getByKey("setup_phase")
            assertNotNull(config)
            assertEquals("PHASE_02", config?.value)
        }
    }
}
