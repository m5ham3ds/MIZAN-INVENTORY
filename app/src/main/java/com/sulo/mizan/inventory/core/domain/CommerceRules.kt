package com.sulo.mizan.inventory.core.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Pure deterministic accounting rules. These functions are independently testable without Android. */
object CommerceRules {
    private val ZERO = BigDecimal.ZERO
    private val CONTEXT = MathContext(18, RoundingMode.HALF_EVEN)

    data class SalesReturnAmounts(
        val customerCredit: BigDecimal,
        /** Revenue reversal excludes tax and uses the original transaction's exchange-rate snapshot. */
        val revenueBase: BigDecimal
    )

    fun quantity(value: String): BigDecimal = BigDecimal(value).also {
        require(it > ZERO) { "Quantity must be greater than zero" }
    }

    fun nonNegativeMoney(value: String): BigDecimal = BigDecimal(value).also {
        require(it >= ZERO) { "Amount cannot be negative" }
    }

    fun positiveRate(value: String): BigDecimal = BigDecimal(value).also {
        require(it > ZERO) { "Exchange rate must be greater than zero" }
    }

    fun invoiceTotal(subtotal: BigDecimal, discount: BigDecimal, tax: BigDecimal, scale: Int): BigDecimal {
        require(scale in 0..8) { "Currency scale must be between 0 and 8" }
        require(subtotal >= ZERO && discount >= ZERO && tax >= ZERO)
        require(discount <= subtotal) { "Discount cannot exceed subtotal" }
        require(subtotal.compareTo(ZERO) != 0 || tax.compareTo(ZERO) == 0) {
            "Tax cannot be charged when the invoice subtotal is zero"
        }
        return subtotal.subtract(discount).add(tax).setScale(scale, RoundingMode.HALF_UP)
    }

    /**
     * Allocate an invoice-level discount and tax to a returned quantity.
     * Customer credit includes the prorated tax; revenue reversal excludes tax.
     */
    fun allocateSalesReturn(
        quantity: BigDecimal,
        lineQuantity: BigDecimal,
        lineTotal: BigDecimal,
        lineRevenueBase: BigDecimal,
        invoiceSubtotal: BigDecimal,
        invoiceDiscount: BigDecimal,
        invoiceTax: BigDecimal,
        exchangeRateToBase: BigDecimal,
        transactionCurrencyScale: Int,
        baseCurrencyScale: Int
    ): SalesReturnAmounts {
        require(quantity > ZERO && lineQuantity > ZERO && quantity <= lineQuantity) {
            "Returned quantity must be positive and no greater than the sold quantity"
        }
        require(lineTotal >= ZERO && lineRevenueBase >= ZERO && invoiceSubtotal >= ZERO)
        require(invoiceDiscount >= ZERO && invoiceDiscount <= invoiceSubtotal && invoiceTax >= ZERO)
        require(exchangeRateToBase > ZERO) { "Exchange rate must be positive" }
        require(transactionCurrencyScale in 0..8 && baseCurrencyScale in 0..8)
        require(invoiceSubtotal.compareTo(ZERO) != 0 || (invoiceDiscount.compareTo(ZERO) == 0 && invoiceTax.compareTo(ZERO) == 0)) {
            "Discount and tax allocation is undefined when the invoice subtotal is zero"
        }
        val quantityRatio = quantity.divide(lineQuantity, CONTEXT)
        val lineRatio = if (invoiceSubtotal.compareTo(ZERO) == 0) ZERO
            else lineTotal.divide(invoiceSubtotal, CONTEXT)
        val grossTransaction = lineTotal.multiply(quantityRatio)
        val discountShare = invoiceDiscount.multiply(lineRatio).multiply(quantityRatio)
        val taxShare = invoiceTax.multiply(lineRatio).multiply(quantityRatio)
        val customerCredit = grossTransaction.subtract(discountShare).add(taxShare)
            .setScale(transactionCurrencyScale, RoundingMode.HALF_UP)
        // The line's stored base revenue is already converted using the original rate; allocate
        // that rounded snapshot proportionally, then subtract this return's share of discount.
        val grossBase = lineRevenueBase.multiply(quantityRatio)
        val discountBase = discountShare.multiply(exchangeRateToBase)
        val revenueBase = grossBase.subtract(discountBase).max(ZERO)
            .setScale(baseCurrencyScale, RoundingMode.HALF_UP)
        return SalesReturnAmounts(customerCredit, revenueBase)
    }

    fun weightedAverageCost(
        oldQuantity: BigDecimal,
        oldUnitCost: BigDecimal,
        receivedQuantity: BigDecimal,
        receivedUnitCost: BigDecimal
    ): BigDecimal {
        require(oldQuantity >= ZERO && oldUnitCost >= ZERO)
        require(receivedQuantity > ZERO && receivedUnitCost >= ZERO)
        val totalQuantity = oldQuantity + receivedQuantity
        return oldUnitCost.multiply(oldQuantity).add(receivedUnitCost.multiply(receivedQuantity))
            .divide(totalQuantity, 8, RoundingMode.HALF_EVEN)
    }

    fun allocateShipping(
        shippingBase: BigDecimal,
        lineValue: BigDecimal,
        subtotal: BigDecimal,
        lineCount: Int
    ): BigDecimal {
        require(shippingBase >= ZERO && lineValue >= ZERO && subtotal >= ZERO)
        require(lineCount > 0)
        if (shippingBase.compareTo(ZERO) == 0) return ZERO
        return if (subtotal > ZERO) shippingBase.multiply(lineValue).divide(subtotal, CONTEXT)
        else shippingBase.divide(BigDecimal.valueOf(lineCount.toLong()), CONTEXT)
    }

    fun landedUnitCost(unitCost: BigDecimal, exchangeRate: BigDecimal, allocatedShippingBase: BigDecimal, quantity: BigDecimal): BigDecimal {
        require(unitCost >= ZERO && exchangeRate > ZERO && allocatedShippingBase >= ZERO && quantity > ZERO)
        return unitCost.multiply(exchangeRate).add(allocatedShippingBase.divide(quantity, CONTEXT))
            .setScale(8, RoundingMode.HALF_EVEN)
    }

    fun outstanding(total: BigDecimal, paid: BigDecimal, returns: BigDecimal): BigDecimal {
        require(total >= ZERO && paid >= ZERO && returns >= ZERO)
        return (total - paid - returns).max(ZERO)
    }

    /** Remaining invoice debt after credit notes and cash refunds/receipts already settled. */
    fun outstandingAfterReturns(
        total: BigDecimal,
        paid: BigDecimal,
        returnCredit: BigDecimal,
        settledRefund: BigDecimal
    ): BigDecimal {
        require(total >= ZERO && paid >= ZERO && returnCredit >= ZERO && settledRefund >= ZERO)
        return (total - paid - returnCredit + settledRefund).max(ZERO)
    }

    /** Refund/credit currently owed after accounting for settlements already paid or received. */
    fun refundDueAfterReturns(
        total: BigDecimal,
        paid: BigDecimal,
        returnCredit: BigDecimal,
        settledRefund: BigDecimal
    ): BigDecimal {
        require(total >= ZERO && paid >= ZERO && returnCredit >= ZERO && settledRefund >= ZERO)
        return (paid + returnCredit - total - settledRefund).max(ZERO)
    }

    fun signedQuantity(movementType: String, quantity: BigDecimal): BigDecimal {
        require(quantity >= ZERO)
        return when (movementType) {
            "OPENING", "PURCHASE", "SALE_RETURN", "ADJUSTMENT_IN", "TRANSFER_IN" -> quantity
            "SALE", "PURCHASE_RETURN", "ADJUSTMENT_OUT", "TRANSFER_OUT" -> quantity.negate()
            else -> ZERO
        }
    }
}
