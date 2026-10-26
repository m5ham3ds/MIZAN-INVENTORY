package com.sulo.mizan.inventory.core.model

/**
 * Currency representation for Mizan Inventory.
 * Represents a recognized currency with its ISO code, Arabic and English display names,
 * visual symbol, and default fraction digits (decimals).
 */
data class Currency(
    val code: String,
    val displayNameAr: String,
    val symbol: String,
    val defaultDecimals: Int = 2,
    val displayNameEn: String = ""
) {
    companion object {
        // Core default currencies for Phase 2
        val SDG = Currency(
            code = "SDG",
            displayNameAr = "جنيه سوداني",
            symbol = "ج.س",
            defaultDecimals = 2,
            displayNameEn = "Sudanese Pound"
        )

        val USD = Currency(
            code = "USD",
            displayNameAr = "دولار أمريكي",
            symbol = "$",
            defaultDecimals = 2,
            displayNameEn = "US Dollar"
        )

        val GBP = Currency(
            code = "GBP",
            displayNameAr = "جنيه إسترليني",
            symbol = "£",
            defaultDecimals = 2,
            displayNameEn = "British Pound"
        )

        // Regional & Global Currencies
        val SAR = Currency(
            code = "SAR",
            displayNameAr = "ريال سعودي",
            symbol = "ر.س",
            defaultDecimals = 2,
            displayNameEn = "Saudi Riyal"
        )

        val AED = Currency(
            code = "AED",
            displayNameAr = "درهم إماراتي",
            symbol = "د.إ",
            defaultDecimals = 2,
            displayNameEn = "UAE Dirham"
        )

        val EUR = Currency(
            code = "EUR",
            displayNameAr = "يورو",
            symbol = "€",
            defaultDecimals = 2,
            displayNameEn = "Euro"
        )

        val YER = Currency(
            code = "YER",
            displayNameAr = "ريال يمني",
            symbol = "ر.ي",
            defaultDecimals = 2,
            displayNameEn = "Yemeni Rial"
        )

        val EGP = Currency(
            code = "EGP",
            displayNameAr = "جنيه مصري",
            symbol = "ج.م",
            defaultDecimals = 2,
            displayNameEn = "Egyptian Pound"
        )

        val QAR = Currency(
            code = "QAR",
            displayNameAr = "ريال قطري",
            symbol = "ر.ق",
            defaultDecimals = 2,
            displayNameEn = "Qatari Riyal"
        )

        val KWD = Currency(
            code = "KWD",
            displayNameAr = "دينار كويتي",
            symbol = "د.ك",
            defaultDecimals = 3,
            displayNameEn = "Kuwaiti Dinar"
        )

        val OMR = Currency(
            code = "OMR",
            displayNameAr = "ريال عماني",
            symbol = "ر.ع",
            defaultDecimals = 3,
            displayNameEn = "Omani Rial"
        )

        val BHD = Currency(
            code = "BHD",
            displayNameAr = "دينار بحريني",
            symbol = "د.ب",
            defaultDecimals = 3,
            displayNameEn = "Bahraini Dinar"
        )

        val TRY = Currency(
            code = "TRY",
            displayNameAr = "ليرة تركية",
            symbol = "₺",
            defaultDecimals = 2,
            displayNameEn = "Turkish Lira"
        )

        val CNY = Currency(
            code = "CNY",
            displayNameAr = "يوان صيني",
            symbol = "¥",
            defaultDecimals = 2,
            displayNameEn = "Chinese Yuan"
        )

        val JPY = Currency(
            code = "JPY",
            displayNameAr = "ين ياباني",
            symbol = "¥",
            defaultDecimals = 0,
            displayNameEn = "Japanese Yen"
        )

        /**
         * The 3 default initial active currencies defined for Phase 2:
         * 1. Primary Base: SDG
         * 2. Secondary: USD
         * 3. Tertiary: GBP
         */
        val DEFAULT_ACTIVE_CURRENCIES = listOf(SDG, USD, GBP)
        val DEFAULT_BASE_CURRENCY = SDG

        /**
         * Comprehensive catalog of supported currencies for merchant selection.
         */
        val ALL_SUPPORTED_CURRENCIES = listOf(
            SDG, USD, GBP, SAR, AED, EUR, YER, EGP, QAR, KWD, OMR, BHD, TRY, CNY, JPY
        )

        // Preserved for Phase 1 backward compatibility
        val DEFAULT_CURRENCIES = listOf(SDG, USD, GBP, SAR, YER)

        fun fromCode(code: String): Currency {
            val trimmed = code.trim().uppercase()
            return ALL_SUPPORTED_CURRENCIES.find { it.code == trimmed }
                ?: Currency(
                    code = trimmed,
                    displayNameAr = trimmed,
                    symbol = trimmed,
                    defaultDecimals = 2,
                    displayNameEn = trimmed
                )
        }
    }
}
