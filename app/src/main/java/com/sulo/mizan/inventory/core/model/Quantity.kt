package com.sulo.mizan.inventory.core.model

import java.math.BigDecimal

/**
 * Quantity Value Object for inventory management.
 * Avoids Double/Float to prevent fractional inventory drift (e.g. 0.333 kg vs 1/3).
 */
data class Quantity(
    val value: BigDecimal,
    val unitName: String = "قطعة"
) : Comparable<Quantity> {

    init {
        require(value >= BigDecimal.ZERO) { "Stock quantity cannot be negative" }
    }

    constructor(count: Long, unitName: String = "قطعة") : this(BigDecimal.valueOf(count), unitName)
    constructor(str: String, unitName: String = "قطعة") : this(BigDecimal(str), unitName)

    operator fun plus(other: Quantity): Quantity {
        require(unitName == other.unitName) {
            "Cannot add quantities with different units: '$unitName' vs '${other.unitName}'"
        }
        return Quantity(value.add(other.value), unitName)
    }

    operator fun minus(other: Quantity): Quantity {
        require(unitName == other.unitName) {
            "Cannot subtract quantities with different units: '$unitName' vs '${other.unitName}'"
        }
        val result = value.subtract(other.value)
        require(result >= BigDecimal.ZERO) { "Stock deduction would result in negative stock: $result" }
        return Quantity(result, unitName)
    }

    operator fun times(factor: BigDecimal): Quantity {
        return Quantity(value.multiply(factor), unitName)
    }

    override fun compareTo(other: Quantity): Int {
        require(unitName == other.unitName) {
            "Cannot compare quantities with different units: '$unitName' vs '${other.unitName}'"
        }
        return value.compareTo(other.value)
    }

    fun formatted(): String = "${value.stripTrailingZeros().toPlainString()} $unitName"
}
