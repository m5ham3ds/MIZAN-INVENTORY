package com.sulo.mizan.inventory.core.model

/**
 * Exchange rate categories to prevent conflating different financial price perspectives.
 *
 * CRITICAL ARCHITECTURAL MANDATE:
 * - A BUY rate (سعر شراء العملة) represents the rate at which the merchant purchases foreign currency.
 * - A SELL rate (سعر بيع العملة) represents the rate at which the merchant sells foreign currency.
 * - A REFERENCE rate (سعر مرجعي) is an indicative/mid-market rate entered for estimation.
 * - A TRANSACTIONAL rate (سعر معاملة فعلية) is the agreed rate recorded on a historical invoice.
 *
 * Rates of different types MUST NOT be used interchangeably without explicit merchant intent,
 * and buy/sell spreads must never be automatically inverted without caution.
 */
enum class RateType(
    val code: String,
    val displayNameAr: String,
    val descriptionAr: String
) {
    REFERENCE(
        code = "REFERENCE",
        displayNameAr = "سعر مرجعي (تقديري)",
        descriptionAr = "سعر السوق أو السعر الإرشادي العام للتقديرات"
    ),
    BUY(
        code = "BUY",
        displayNameAr = "سعر شراء العملة",
        descriptionAr = "السعر الذي يشتري به التاجر العملة الأجنبية مقابل العملة المحلية"
    ),
    SELL(
        code = "SELL",
        displayNameAr = "سعر بيع العملة",
        descriptionAr = "السعر الذي يبيع به التاجر العملة الأجنبية مقابل العملة المحلية"
    ),
    TRANSACTIONAL(
        code = "TRANSACTIONAL",
        displayNameAr = "سعر معاملة فعلية",
        descriptionAr = "السعر المتفق عليه والمنفذ في عملية تجارية أو تسوية حقيقية"
    );

    companion object {
        fun fromCode(code: String?): RateType {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: REFERENCE
        }
    }
}
