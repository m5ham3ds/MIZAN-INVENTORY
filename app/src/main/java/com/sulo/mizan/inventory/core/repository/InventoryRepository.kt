package com.sulo.mizan.inventory.core.repository

import android.content.Context
import androidx.room.withTransaction
import com.sulo.mizan.inventory.core.database.AppDatabase
import com.sulo.mizan.inventory.core.database.CashAccountEntity
import com.sulo.mizan.inventory.core.database.CashTransactionEntity
import com.sulo.mizan.inventory.core.database.CommerceDao
import com.sulo.mizan.inventory.core.database.ExpenseEntity
import com.sulo.mizan.inventory.core.database.ProductEntity
import com.sulo.mizan.inventory.core.database.PurchaseInvoiceEntity
import com.sulo.mizan.inventory.core.database.PurchaseInvoiceLineEntity
import com.sulo.mizan.inventory.core.database.PurchaseReturnEntity
import com.sulo.mizan.inventory.core.database.SalesInvoiceEntity
import com.sulo.mizan.inventory.core.database.SalesInvoiceLineEntity
import com.sulo.mizan.inventory.core.database.SalesReturnEntity
import com.sulo.mizan.inventory.core.database.ShipmentEntity
import com.sulo.mizan.inventory.core.database.StockMovementEntity
import com.sulo.mizan.inventory.core.database.StocktakeEntity
import com.sulo.mizan.inventory.core.database.StocktakeLineEntity
import com.sulo.mizan.inventory.core.database.WarehouseEntity
import com.sulo.mizan.inventory.core.domain.CommerceRules
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.UUID

private val ZERO: BigDecimal = BigDecimal.ZERO
private val ONE: BigDecimal = BigDecimal.ONE
private val DIVISION_CONTEXT = MathContext(18, RoundingMode.HALF_EVEN)

private fun String.bd(): BigDecimal = BigDecimal(this)
private fun BigDecimal.moneyString(scale: Int = 8): String = setScale(scale, RoundingMode.HALF_EVEN).stripTrailingZeros().takeIf { it.scale() >= 0 }?.toPlainString() ?: toPlainString()

/** One line in a purchase invoice. Quantity and all monetary inputs are decimal strings. */
data class PurchaseLineDraft(val productId: Long, val quantity: String, val unitCost: String)
data class SaleLineDraft(val productId: Long, val quantity: String, val unitPrice: String)

data class StockBalance(
    val product: ProductEntity,
    val quantity: BigDecimal,
    val valuationAmount: BigDecimal,
    val perWarehouse: Map<Long, BigDecimal>
) {
    val isLowStock: Boolean get() = quantity <= product.minimumStock.bd()
}

data class CashBalance(val account: CashAccountEntity, val amount: BigDecimal)
private data class InventoryReportData(val products: List<ProductEntity>, val movements: List<StockMovementEntity>, val baseCode: String)
private data class SalesReportData(val invoices: List<SalesInvoiceEntity>, val lines: List<SalesInvoiceLineEntity>, val returns: List<SalesReturnEntity>)
private data class PurchaseReportData(val invoices: List<PurchaseInvoiceEntity>, val lines: List<PurchaseInvoiceLineEntity>, val returns: List<PurchaseReturnEntity>, val shipments: List<ShipmentEntity>)
private data class FinanceReportData(val expenses: List<ExpenseEntity>, val accounts: List<CashAccountEntity>, val transactions: List<CashTransactionEntity>)
data class CurrencyTotal(val currencyCode: String, val amount: BigDecimal)
data class FinancialSummary(
    val baseCurrencyCode: String,
    val productCount: Int,
    val lowStockCount: Int,
    val totalStockQuantity: BigDecimal,
    val inventoryValueByCurrency: List<CurrencyTotal>,
    val replacementValueByCurrency: List<CurrencyTotal>,
    /** Replacement cost less book value for products whose two valuations use the same currency. */
    val replacementCostVarianceByCurrency: List<CurrencyTotal>,
    val nonComparableReplacementProductCount: Int,
    val netRevenueBase: BigDecimal,
    val costOfGoodsSoldBase: BigDecimal,
    val grossProfitBase: BigDecimal,
    val expensesBase: BigDecimal,
    val netProfitBase: BigDecimal,
    val receivableByCurrency: List<CurrencyTotal>,
    val payableByCurrency: List<CurrencyTotal>,
    val unpaidExpensesByCurrency: List<CurrencyTotal>,
    val customerRefundDueByCurrency: List<CurrencyTotal>,
    val supplierRefundDueByCurrency: List<CurrencyTotal>,
    val cashBalances: List<CashBalance>,
    val shipmentCount: Int,
    val purchaseCount: Int,
    val salesCount: Int
)

/**
 * Transactional business layer for inventory and accounting. Every operation that changes stock,
 * invoice balances and cash runs in a single Room transaction to avoid partial postings.
 */
class InventoryRepository(context: Context) {
    private val database: AppDatabase = AppDatabase.getInstance(context.applicationContext)
    private val dao: CommerceDao = database.commerceDao()
    private val currencies = CurrencyRepository(context.applicationContext)

    val warehousesFlow: Flow<List<WarehouseEntity>> = dao.observeWarehouses()
    val productsFlow: Flow<List<ProductEntity>> = dao.observeProducts()
    val movementsFlow: Flow<List<StockMovementEntity>> = dao.observeMovements()
    val shipmentsFlow: Flow<List<ShipmentEntity>> = dao.observeShipments()
    val purchaseInvoicesFlow: Flow<List<PurchaseInvoiceEntity>> = dao.observePurchaseInvoices()
    val salesInvoicesFlow: Flow<List<SalesInvoiceEntity>> = dao.observeSalesInvoices()
    val purchaseInvoiceLinesFlow: Flow<List<PurchaseInvoiceLineEntity>> = dao.observePurchaseLines()
    val salesInvoiceLinesFlow: Flow<List<SalesInvoiceLineEntity>> = dao.observeSalesLines()
    val salesReturnsFlow = dao.observeSalesReturns()
    val purchaseReturnsFlow = dao.observePurchaseReturns()
    val expensesFlow: Flow<List<ExpenseEntity>> = dao.observeExpenses()
    val cashAccountsFlow: Flow<List<CashAccountEntity>> = dao.observeCashAccounts()
    val cashTransactionsFlow: Flow<List<CashTransactionEntity>> = dao.observeCashTransactions()
    val stocktakesFlow: Flow<List<StocktakeEntity>> = dao.observeStocktakes()

    suspend fun ensureInitialized() {
        currencies.ensureInitialized()
        database.withTransaction {
            if (dao.getWarehouses().isEmpty()) dao.insertWarehouse(WarehouseEntity(name = "المستودع الرئيسي"))
            if (dao.getCashAccounts().isEmpty()) dao.insertCashAccount(CashAccountEntity(name = "الصندوق الرئيسي", currencyCode = "SDG"))
        }
    }

    suspend fun baseCurrency(): Currency = currencies.getBaseCurrency()

    suspend fun createWarehouse(name: String, location: String = ""): Long {
        require(name.isNotBlank()) { "اسم المستودع مطلوب" }
        return dao.insertWarehouse(WarehouseEntity(name = name.trim(), location = location.trim()))
    }

    suspend fun createProduct(
        sku: String,
        name: String,
        category: String = "عام",
        unitName: String = "قطعة",
        minimumStock: String = "0",
        sellingPrice: String = "0",
        replacementCost: String = sellingPrice,
        barcode: String? = null,
        openingQuantity: String = "0",
        warehouseId: Long? = null
    ): Long = database.withTransaction {
        val normalizedSku = sku.trim().uppercase()
        require(normalizedSku.isNotBlank()) { "رمز الصنف SKU مطلوب" }
        require(name.isNotBlank()) { "اسم الصنف مطلوب" }
        require(unitName.isNotBlank()) { "وحدة القياس مطلوبة" }
        val minimum = minimumStock.bd()
        val opening = openingQuantity.bd()
        val price = sellingPrice.bd()
        val replacement = replacementCost.bd()
        require(minimum >= ZERO && opening >= ZERO && price >= ZERO && replacement >= ZERO) { "الكميات والأسعار لا تقبل قيمة سالبة" }
        val currency = currencies.getBaseCurrency()
        val entity = ProductEntity(
            sku = normalizedSku,
            barcode = barcode?.trim()?.takeIf { it.isNotEmpty() },
            name = name.trim(),
            category = category.trim().ifBlank { "عام" },
            unitName = unitName.trim(),
            minimumStock = minimum.toPlainString(),
            averageCostAmount = "0",
            valuationCurrencyCode = currency.code,
            replacementCostAmount = replacement.toPlainString(),
            sellingPriceAmount = price.toPlainString(),
            priceCurrencyCode = currency.code
        )
        val id = dao.insertProduct(entity)
        if (opening > ZERO) {
            val warehouse = if (warehouseId != null) {
                dao.getWarehouse(warehouseId) ?: throw IllegalArgumentException("المستودع المحدد غير موجود")
            } else {
                dao.getWarehouses().firstOrNull { it.active }
                    ?: throw IllegalStateException("أنشئ مستودعًا نشطًا أولًا")
            }
            require(warehouse.active) { "لا يمكن تسجيل الرصيد الافتتاحي في مستودع غير نشط" }
            dao.insertMovement(
                StockMovementEntity(
                    productId = id, warehouseId = warehouse.id, movementType = "OPENING",
                    quantity = opening.toPlainString(), unitCostAmount = "0", currencyCode = currency.code,
                    note = "رصيد افتتاحي"
                )
            )
        }
        id
    }

    suspend fun updateProductPricing(productId: Long, sellingPrice: String, replacementCost: String) {
        val sell = sellingPrice.bd()
        val replacement = replacementCost.bd()
        require(sell >= ZERO && replacement >= ZERO) { "السعر لا يمكن أن يكون سالبًا" }
        database.withTransaction {
            val product = dao.getProduct(productId) ?: throw IllegalArgumentException("الصنف غير موجود")
            dao.updateProduct(product.copy(sellingPriceAmount = sell.toPlainString(), replacementCostAmount = replacement.toPlainString()))
        }
    }

    fun observeStockBalances(warehouseId: Long? = null): Flow<List<StockBalance>> =
        combine(productsFlow, movementsFlow, warehousesFlow) { products, movements, warehouses ->
            products.map { product ->
                val relevant = movements.filter { it.productId == product.id && (warehouseId == null || it.warehouseId == warehouseId) }
                val perWarehouse = relevant.groupBy { it.warehouseId }.mapValues { (_, items) ->
                    items.fold(ZERO) { acc, movement -> acc + signedQuantity(movement) }
                }
                val quantity = perWarehouse.values.fold(ZERO, BigDecimal::add)
                val valuation = quantity.multiply(product.averageCostAmount.bd())
                StockBalance(product, quantity, valuation, perWarehouse)
            }.sortedWith(compareBy<StockBalance> { !it.isLowStock }.thenBy { it.product.name.lowercase() })
        }

    private fun signedQuantity(movement: StockMovementEntity): BigDecimal =
        CommerceRules.signedQuantity(movement.movementType, movement.quantity.bd())

    private suspend fun currentStock(productId: Long, warehouseId: Long? = null): BigDecimal =
        dao.getMovements().asSequence()
            .filter { it.productId == productId && (warehouseId == null || it.warehouseId == warehouseId) }
            .fold(ZERO) { acc, movement -> acc + signedQuantity(movement) }

    private suspend fun validateFx(currencyCode: String, rateText: String, baseCode: String): BigDecimal {
        val normalizedCode = currencyCode.trim().uppercase()
        require(currencyCode == normalizedCode) { "استخدم رمز العملة بصيغته القياسية: $normalizedCode" }
        require(Currency.ALL_SUPPORTED_CURRENCIES.any { it.code == normalizedCode }) { "رمز العملة غير مدعوم: $normalizedCode" }
        val currency = Currency.fromCode(normalizedCode)
        val active = currencies.getActiveCurrencies()
        require(active.any { it.code == currency.code }) { "العملة $currencyCode غير نشطة؛ فعّلها قبل ترحيل معاملة جديدة" }
        val rate = rateText.bd()
        require(rate > ZERO) { "سعر التحويل إلى العملة الأساسية يجب أن يكون أكبر من الصفر" }
        if (currency.code == baseCode) require(rate.compareTo(ONE) == 0) { "سعر العملة الأساسية يجب أن يساوي 1" }
        return rate
    }

    suspend fun createPurchaseInvoice(
        supplierName: String,
        warehouseId: Long,
        currencyCode: String,
        exchangeRateToBase: String,
        lines: List<PurchaseLineDraft>,
        shippingAmount: String = "0",
        paidAmount: String = "0",
        shipmentReference: String = ""
    ): Long = database.withTransaction {
        require(supplierName.isNotBlank()) { "اسم المورد مطلوب" }
        require(lines.isNotEmpty()) { "أضف بندًا واحدًا على الأقل إلى الفاتورة" }
        val warehouse = dao.getWarehouse(warehouseId) ?: throw IllegalArgumentException("المستودع غير موجود")
        require(warehouse.active) { "المستودع غير نشط" }
        val base = currencies.getBaseCurrency()
        val fx = validateFx(currencyCode, exchangeRateToBase, base.code)
        val currency = Currency.fromCode(currencyCode)
        val shipping = shippingAmount.bd()
        val paid = paidAmount.bd()
        require(shipping >= ZERO && paid >= ZERO) { "الشحن والمدفوع لا يمكن أن يكونا سالبين" }
        val normalized = lines.map { line ->
            val q = line.quantity.bd(); val unit = line.unitCost.bd()
            require(q > ZERO && unit >= ZERO) { "كمية الشراء يجب أن تكون موجبة والتكلفة غير سالبة" }
            require(dao.getProduct(line.productId) != null) { "أحد الأصناف المحددة غير موجود" }
            Triple(line, q, unit)
        }
        val subtotal = normalized.fold(ZERO) { acc, (_, q, unit) -> acc + q.multiply(unit) }
            .setScale(currency.defaultDecimals, RoundingMode.HALF_UP)
        val total = (subtotal + shipping).setScale(currency.defaultDecimals, RoundingMode.HALF_UP)
        require(paid <= total) { "المبلغ المدفوع أكبر من إجمالي الفاتورة" }
        val account = if (paid > ZERO) getOrCreateCashAccount(currencyCode) else null
        if (account != null) require(cashBalanceInternal(account.id) >= paid) { "رصيد الصندوق غير كافٍ لدفع الفاتورة" }

        val shipmentId = if (shipping > ZERO || shipmentReference.isNotBlank()) {
            dao.insertShipment(
                ShipmentEntity(
                    trackingNumber = shipmentReference.trim().ifBlank { "SHIP-${System.currentTimeMillis()}" },
                    description = "شحنة مرتبطة بفاتورة مشتريات",
                    supplierName = supplierName.trim(), costAmount = shipping.toPlainString(), currencyCode = currencyCode,
                    exchangeRateToBase = fx.toPlainString(),
                    costBaseAmount = shipping.multiply(fx).setScale(base.defaultDecimals, RoundingMode.HALF_UP).toPlainString()
                )
            )
        } else null
        val invoiceId = dao.insertPurchaseInvoice(
            PurchaseInvoiceEntity(
                invoiceNumber = "PI-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4)}",
                supplierName = supplierName.trim(), warehouseId = warehouseId, shipmentId = shipmentId,
                currencyCode = currencyCode, exchangeRateToBase = fx.toPlainString(),
                subtotalAmount = subtotal.toPlainString(), shippingAmount = shipping.toPlainString(),
                totalAmount = total.toPlainString(), totalBaseAmount = total.multiply(fx).setScale(base.defaultDecimals, RoundingMode.HALF_UP).toPlainString(),
                paidAmount = paid.toPlainString(), status = if (paid >= total) "PAID" else "POSTED"
            )
        )
        val shippingBase = shipping.multiply(fx)
        normalized.forEach { (line, qty, unitCost) ->
            val product = dao.getProduct(line.productId) ?: throw IllegalArgumentException("الصنف غير موجود")
            val previousStock = currentStock(product.id)
            if (previousStock > ZERO && product.valuationCurrencyCode != base.code) {
                throw IllegalStateException("مخزون ${product.name} مقيم بعملة ${product.valuationCurrencyCode}; يلزم إعادة تقييم المخزون قبل تسجيل شراء بعملة أساس ${base.code}")
            }
            val lineTotal = qty.multiply(unitCost).setScale(currency.defaultDecimals, RoundingMode.HALF_UP)
            val freightShare = CommerceRules.allocateShipping(shippingBase, lineTotal, subtotal, normalized.size)
            val unitBase = CommerceRules.landedUnitCost(unitCost, fx, freightShare, qty)
            dao.insertPurchaseLine(
                PurchaseInvoiceLineEntity(
                    invoiceId = invoiceId, productId = product.id, quantity = qty.toPlainString(),
                    unitCostAmount = unitCost.toPlainString(), lineTotalAmount = lineTotal.toPlainString(),
                    unitCostBaseAmount = unitBase.toPlainString()
                )
            )
            dao.insertMovement(
                StockMovementEntity(
                    productId = product.id, warehouseId = warehouseId, movementType = "PURCHASE",
                    quantity = qty.toPlainString(), unitCostAmount = unitBase.toPlainString(), currencyCode = base.code,
                    sourceType = "PURCHASE_INVOICE", sourceId = invoiceId, note = "فاتورة شراء ${invoiceId}"
                )
            )
            val newStock = previousStock + qty
            val oldCost = if (previousStock == ZERO) ZERO else product.averageCostAmount.bd()
            val newAverage = CommerceRules.weightedAverageCost(previousStock, oldCost, qty, unitBase)
            dao.updateProduct(product.copy(averageCostAmount = newAverage.toPlainString(), valuationCurrencyCode = base.code))
        }
        if (account != null) dao.insertCashTransaction(
            CashTransactionEntity(accountId = account.id, direction = "DEBIT", sourceType = "PURCHASE_PAYMENT", sourceId = invoiceId, amount = paid.toPlainString(), note = "سداد فاتورة مشتريات")
        )
        invoiceId
    }

    suspend fun createSalesInvoice(
        customerName: String,
        warehouseId: Long,
        currencyCode: String,
        exchangeRateToBase: String,
        lines: List<SaleLineDraft>,
        discountAmount: String = "0",
        taxAmount: String = "0",
        paidAmount: String = "0"
    ): Long = database.withTransaction {
        require(customerName.isNotBlank()) { "اسم العميل مطلوب" }
        require(lines.isNotEmpty()) { "أضف بندًا واحدًا على الأقل إلى الفاتورة" }
        val warehouse = dao.getWarehouse(warehouseId) ?: throw IllegalArgumentException("المستودع غير موجود")
        require(warehouse.active) { "المستودع غير نشط" }
        val base = currencies.getBaseCurrency()
        val fx = validateFx(currencyCode, exchangeRateToBase, base.code)
        val currency = Currency.fromCode(currencyCode)
        val discount = discountAmount.bd(); val tax = taxAmount.bd(); val paid = paidAmount.bd()
        require(discount >= ZERO && tax >= ZERO && paid >= ZERO) { "الخصم والضريبة والمدفوع لا يقبلون قيمًا سالبة" }
        // Validate aggregate demand per product first; a product may appear on multiple invoice
        // lines, so checking each line independently can otherwise oversell the same stock.
        val requestedByProduct = lines.groupBy { it.productId }.mapValues { (_, productLines) ->
            productLines.fold(ZERO) { total, line ->
                val q = line.quantity.bd()
                val unit = line.unitPrice.bd()
                require(q > ZERO && unit >= ZERO) { "كمية البيع يجب أن تكون موجبة والسعر غير سالب" }
                total + q
            }
        }
        requestedByProduct.forEach { (productId, requested) ->
            val product = dao.getProduct(productId) ?: throw IllegalArgumentException("الصنف غير موجود")
            val available = currentStock(productId, warehouseId)
            require(available >= requested) {
                "المخزون غير كافٍ للصنف ${product.name}; المطلوب ${requested.toPlainString()} والمتاح ${available.toPlainString()} ${product.unitName}"
            }
        }
        val normalized = lines.map { line ->
            val q = line.quantity.bd(); val unit = line.unitPrice.bd()
            Triple(line, q, unit)
        }
        val subtotal = normalized.fold(ZERO) { acc, (_, q, unit) -> acc + q.multiply(unit) }
            .setScale(currency.defaultDecimals, RoundingMode.HALF_UP)
        require(discount <= subtotal) { "الخصم أكبر من قيمة البنود" }
        val total = CommerceRules.invoiceTotal(subtotal, discount, tax, currency.defaultDecimals)
        require(paid <= total) { "المبلغ المدفوع أكبر من إجمالي الفاتورة" }
        val invoiceId = dao.insertSalesInvoice(
            SalesInvoiceEntity(
                invoiceNumber = "SI-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4)}",
                customerName = customerName.trim(), warehouseId = warehouseId, currencyCode = currencyCode,
                exchangeRateToBase = fx.toPlainString(), subtotalAmount = subtotal.toPlainString(),
                discountAmount = discount.toPlainString(), taxAmount = tax.toPlainString(), totalAmount = total.toPlainString(),
                totalBaseAmount = total.multiply(fx).setScale(base.defaultDecimals, RoundingMode.HALF_UP).toPlainString(),
                paidAmount = paid.toPlainString(), status = if (paid >= total) "PAID" else "POSTED"
            )
        )
        normalized.forEach { (line, qty, unitPrice) ->
            val product = dao.getProduct(line.productId) ?: throw IllegalArgumentException("الصنف غير موجود")
            require(product.valuationCurrencyCode == base.code || currentStock(product.id) == ZERO) {
                "مخزون ${product.name} مقيم بعملة مختلفة عن العملة الأساسية الحالية"
            }
            val unitCost = product.averageCostAmount.bd()
            val lineTotal = qty.multiply(unitPrice).setScale(currency.defaultDecimals, RoundingMode.HALF_UP)
            val revenueBase = lineTotal.multiply(fx).setScale(base.defaultDecimals, RoundingMode.HALF_UP)
            val costTotal = unitCost.multiply(qty).setScale(8, RoundingMode.HALF_EVEN)
            dao.insertSalesLine(
                SalesInvoiceLineEntity(
                    invoiceId = invoiceId, productId = product.id, quantity = qty.toPlainString(),
                    unitPriceAmount = unitPrice.toPlainString(), lineTotalAmount = lineTotal.toPlainString(),
                    revenueBaseAmount = revenueBase.toPlainString(), unitCostBaseAmount = unitCost.toPlainString(),
                    costTotalBaseAmount = costTotal.toPlainString()
                )
            )
            dao.insertMovement(
                StockMovementEntity(
                    productId = product.id, warehouseId = warehouseId, movementType = "SALE",
                    quantity = qty.toPlainString(), unitCostAmount = unitCost.toPlainString(), currencyCode = base.code,
                    sourceType = "SALES_INVOICE", sourceId = invoiceId, note = "فاتورة بيع ${invoiceId}"
                )
            )
        }
        if (paid > ZERO) {
            val account = getOrCreateCashAccount(currencyCode)
            dao.insertCashTransaction(CashTransactionEntity(accountId = account.id, direction = "CREDIT", sourceType = "SALE_RECEIPT", sourceId = invoiceId, amount = paid.toPlainString(), note = "تحصيل فاتورة مبيعات"))
        }
        invoiceId
    }

    suspend fun returnSalesLine(lineId: Long, quantityText: String, note: String = "مرتجع مبيعات"): Long = database.withTransaction {
        val line = dao.getSalesLine(lineId) ?: throw IllegalArgumentException("بند فاتورة البيع غير موجود")
        val invoice = dao.getSalesInvoice(line.invoiceId) ?: throw IllegalStateException("فاتورة البيع غير موجودة")
        val qty = quantityText.bd()
        val sold = line.quantity.bd(); val already = line.returnedQuantity.bd()
        require(qty > ZERO && already + qty <= sold) { "كمية المرتجع تتجاوز الكمية المباعة المتبقية" }
        val rate = invoice.exchangeRateToBase.bd()
        val transactionCurrency = Currency.fromCode(invoice.currencyCode)
        val baseCurrency = currencies.getBaseCurrency()
        val returnAmounts = CommerceRules.allocateSalesReturn(
            quantity = qty,
            lineQuantity = sold,
            lineTotal = line.lineTotalAmount.bd(),
            lineRevenueBase = line.revenueBaseAmount.bd(),
            invoiceSubtotal = invoice.subtotalAmount.bd(),
            invoiceDiscount = invoice.discountAmount.bd(),
            invoiceTax = invoice.taxAmount.bd(),
            exchangeRateToBase = rate,
            transactionCurrencyScale = transactionCurrency.defaultDecimals,
            baseCurrencyScale = baseCurrency.defaultDecimals
        )
        val debtCredit = returnAmounts.customerCredit
        val revenueReturnBase = returnAmounts.revenueBase
        val costRestore = line.unitCostBaseAmount.bd().multiply(qty).setScale(8, RoundingMode.HALF_EVEN)
        val retId = dao.insertSalesReturn(SalesReturnEntity(
            invoiceLineId = lineId, productId = line.productId, warehouseId = invoice.warehouseId,
            quantity = qty.toPlainString(), amount = debtCredit.toPlainString(), currencyCode = invoice.currencyCode,
            exchangeRateToBase = rate.toPlainString(), amountBase = revenueReturnBase.toPlainString(),
            costRestoredBaseAmount = costRestore.toPlainString(), note = note.trim()
        ))
        dao.updateSalesLine(line.copy(returnedQuantity = (already + qty).toPlainString()))
        val product = dao.getProduct(line.productId) ?: throw IllegalStateException("الصنف المرتجع غير موجود")
        dao.insertMovement(StockMovementEntity(
            productId = line.productId, warehouseId = invoice.warehouseId, movementType = "SALE_RETURN",
            quantity = qty.toPlainString(), unitCostAmount = line.unitCostBaseAmount, currencyCode = product.valuationCurrencyCode,
            sourceType = "SALES_RETURN", sourceId = retId, note = note
        ))
        retId
    }

    suspend fun returnPurchaseLine(lineId: Long, quantityText: String, note: String = "مرتجع مشتريات"): Long = database.withTransaction {
        val line = dao.getPurchaseLine(lineId) ?: throw IllegalArgumentException("بند فاتورة الشراء غير موجود")
        val invoice = dao.getPurchaseInvoice(line.invoiceId) ?: throw IllegalStateException("فاتورة الشراء غير موجودة")
        val qty = quantityText.bd(); val original = line.quantity.bd(); val already = line.returnedQuantity.bd()
        require(qty > ZERO && already + qty <= original) { "كمية المرتجع تتجاوز الكمية المشتراة المتبقية" }
        val available = currentStock(line.productId, invoice.warehouseId)
        require(available >= qty) { "لا يمكن إرجاع كمية أكبر من المتوفر في المستودع" }
        val currency = Currency.fromCode(invoice.currencyCode)
        val amount = line.unitCostAmount.bd().multiply(qty).setScale(currency.defaultDecimals, RoundingMode.HALF_UP)
        val amountBase = line.unitCostBaseAmount.bd().multiply(qty).setScale(8, RoundingMode.HALF_EVEN)
        val retId = dao.insertPurchaseReturn(PurchaseReturnEntity(
            invoiceLineId = lineId, productId = line.productId, warehouseId = invoice.warehouseId,
            quantity = qty.toPlainString(), amount = amount.toPlainString(), currencyCode = invoice.currencyCode,
            exchangeRateToBase = invoice.exchangeRateToBase, amountBase = amountBase.toPlainString(),
            costRemovedBaseAmount = amountBase.toPlainString(), note = note.trim()
        ))
        dao.updatePurchaseLine(line.copy(returnedQuantity = (already + qty).toPlainString()))
        dao.insertMovement(StockMovementEntity(
            productId = line.productId, warehouseId = invoice.warehouseId, movementType = "PURCHASE_RETURN",
            quantity = qty.toPlainString(), unitCostAmount = line.unitCostBaseAmount,
            currencyCode = dao.getProduct(line.productId)?.valuationCurrencyCode ?: "SDG",
            sourceType = "PURCHASE_RETURN", sourceId = retId, note = note
        ))
        retId
    }

    suspend fun createExpense(
        category: String,
        description: String,
        amountText: String,
        currencyCode: String,
        exchangeRateToBase: String,
        paid: Boolean,
        cashAccountId: Long? = null
    ): Long = database.withTransaction {
        require(category.isNotBlank() && description.isNotBlank()) { "فئة المصروف ووصفه مطلوبان" }
        val amount = amountText.bd(); require(amount > ZERO) { "قيمة المصروف يجب أن تكون أكبر من الصفر" }
        val base = currencies.getBaseCurrency(); val fx = validateFx(currencyCode, exchangeRateToBase, base.code)
        val account = if (paid) {
            val id = cashAccountId ?: getOrCreateCashAccount(currencyCode).id
            val a = dao.getCashAccount(id) ?: throw IllegalArgumentException("الصندوق غير موجود")
            require(a.active) { "الصندوق غير نشط" }
            require(a.currencyCode == currencyCode) { "عملة الصندوق لا تطابق عملة المصروف" }
            require(cashBalanceInternal(a.id) >= amount) { "رصيد الصندوق غير كافٍ" }
            a
        } else null
        val id = dao.insertExpense(ExpenseEntity(
            category = category.trim(), description = description.trim(), amount = amount.toPlainString(),
            currencyCode = currencyCode, exchangeRateToBase = fx.toPlainString(),
            amountBase = amount.multiply(fx).setScale(base.defaultDecimals, RoundingMode.HALF_UP).toPlainString(),
            paid = paid, cashAccountId = account?.id
        ))
        if (account != null) dao.insertCashTransaction(CashTransactionEntity(accountId = account.id, direction = "DEBIT", sourceType = "EXPENSE", sourceId = id, amount = amount.toPlainString(), note = description.trim()))
        id
    }

    suspend fun payExpense(expenseId: Long, cashAccountId: Long) = database.withTransaction {
        val expense = dao.getExpense(expenseId) ?: throw IllegalArgumentException("المصروف غير موجود")
        require(!expense.paid) { "المصروف مسدد بالفعل" }
        val account = dao.getCashAccount(cashAccountId) ?: throw IllegalArgumentException("الصندوق غير موجود")
        require(account.active) { "الصندوق غير نشط" }
        require(account.currencyCode == expense.currencyCode) { "عملة الصندوق لا تطابق عملة المصروف" }
        val amount = expense.amount.bd()
        require(cashBalanceInternal(account.id) >= amount) { "رصيد الصندوق غير كافٍ" }
        dao.updateExpense(expense.copy(paid = true, cashAccountId = account.id))
        dao.insertCashTransaction(CashTransactionEntity(accountId = account.id, direction = "DEBIT", sourceType = "EXPENSE", sourceId = expense.id, amount = amount.toPlainString(), note = expense.description))
    }

    suspend fun createCashAccount(name: String, currencyCode: String, openingBalanceText: String): Long {
        require(name.isNotBlank()) { "اسم الصندوق مطلوب" }
        val normalizedCurrency = Currency.fromCode(currencyCode).code
        require(currencies.getActiveCurrencies().any { it.code == normalizedCurrency }) {
            "لا يمكن إنشاء صندوق بعملة غير نشطة؛ فعّل العملة أولًا"
        }
        val opening = openingBalanceText.bd(); require(opening >= ZERO) { "الرصيد الافتتاحي لا يمكن أن يكون سالبًا" }
        return dao.insertCashAccount(CashAccountEntity(name = name.trim(), currencyCode = normalizedCurrency, openingBalanceAmount = opening.toPlainString()))
    }

    suspend fun recordCashTransaction(accountId: Long, direction: String, amountText: String, note: String) = database.withTransaction {
        require(direction == "CREDIT" || direction == "DEBIT") { "نوع الحركة النقدية غير صالح" }
        val amount = amountText.bd(); require(amount > ZERO) { "قيمة الحركة يجب أن تكون موجبة" }
        val account = dao.getCashAccount(accountId) ?: throw IllegalArgumentException("الصندوق غير موجود")
        require(account.active) { "الصندوق غير نشط" }
        if (direction == "DEBIT") require(cashBalanceInternal(accountId) >= amount) { "الرصيد النقدي غير كافٍ" }
        dao.insertCashTransaction(CashTransactionEntity(accountId = accountId, direction = direction, amount = amount.toPlainString(), note = note.trim()))
    }

    private suspend fun returnsForSalesInvoice(invoiceId: Long): Pair<BigDecimal, BigDecimal> {
        val rows = dao.getSalesReturns().filter { row -> dao.getSalesLine(row.invoiceLineId)?.invoiceId == invoiceId }
        val credited = rows.fold(ZERO) { total, row -> total + row.amount.bd() }
        val refunded = rows.fold(ZERO) { total, row -> total + row.refundedAmount.bd() }
        return credited to refunded
    }

    private suspend fun returnsForPurchaseInvoice(invoiceId: Long): Pair<BigDecimal, BigDecimal> {
        val rows = dao.getPurchaseReturns().filter { row -> dao.getPurchaseLine(row.invoiceLineId)?.invoiceId == invoiceId }
        val credited = rows.fold(ZERO) { total, row -> total + row.amount.bd() }
        val refunded = rows.fold(ZERO) { total, row -> total + row.refundReceivedAmount.bd() }
        return credited to refunded
    }

    suspend fun recordInvoicePayment(isSale: Boolean, invoiceId: Long, amountText: String, accountId: Long): BigDecimal = database.withTransaction {
        val amount = amountText.bd(); require(amount > ZERO) { "قيمة السداد يجب أن تكون موجبة" }
        val account = dao.getCashAccount(accountId) ?: throw IllegalArgumentException("الصندوق غير موجود")
        require(account.active) { "الصندوق غير نشط" }
        val invoiceCurrency: String
        val outstanding: BigDecimal
        val currentPaid: BigDecimal
        if (isSale) {
            val invoice = dao.getSalesInvoice(invoiceId) ?: throw IllegalArgumentException("فاتورة البيع غير موجودة")
            invoiceCurrency = invoice.currencyCode; currentPaid = invoice.paidAmount.bd()
            val returns = dao.getSalesReturns().filter { ret -> dao.getSalesLine(ret.invoiceLineId)?.invoiceId == invoiceId }
            val returnCredit = returns.fold(ZERO) { acc, ret -> acc + ret.amount.bd() }
            val refundsAlreadyPaid = returns.fold(ZERO) { acc, ret -> acc + ret.refundedAmount.bd() }
            outstanding = CommerceRules.outstandingAfterReturns(invoice.totalAmount.bd(), currentPaid, returnCredit, refundsAlreadyPaid)
        } else {
            val invoice = dao.getPurchaseInvoice(invoiceId) ?: throw IllegalArgumentException("فاتورة الشراء غير موجودة")
            invoiceCurrency = invoice.currencyCode; currentPaid = invoice.paidAmount.bd()
            val returns = dao.getPurchaseReturns().filter { ret -> dao.getPurchaseLine(ret.invoiceLineId)?.invoiceId == invoiceId }
            val returnCredit = returns.fold(ZERO) { acc, ret -> acc + ret.amount.bd() }
            val refundsAlreadyReceived = returns.fold(ZERO) { acc, ret -> acc + ret.refundReceivedAmount.bd() }
            outstanding = CommerceRules.outstandingAfterReturns(invoice.totalAmount.bd(), currentPaid, returnCredit, refundsAlreadyReceived)
        }
        require(account.currencyCode == invoiceCurrency) { "عملة الصندوق لا تطابق عملة الفاتورة" }
        require(amount <= outstanding) { "المبلغ أكبر من المتبقي على الفاتورة (${outstanding.toPlainString()})" }
        val direction = if (isSale) "CREDIT" else "DEBIT"
        if (direction == "DEBIT") require(cashBalanceInternal(account.id) >= amount) { "رصيد الصندوق غير كافٍ" }
        if (isSale) {
            val inv = dao.getSalesInvoice(invoiceId)!!
            val newPaid = currentPaid + amount
            val settledReturnCredit = returnsForSalesInvoice(invoiceId)
            val remainingAfterPayment = CommerceRules.outstandingAfterReturns(
                inv.totalAmount.bd(), newPaid, settledReturnCredit.first, settledReturnCredit.second
            )
            dao.updateSalesInvoice(inv.copy(paidAmount = newPaid.toPlainString(), status = if (remainingAfterPayment.compareTo(ZERO) == 0) "PAID" else "POSTED"))
        } else {
            val inv = dao.getPurchaseInvoice(invoiceId)!!
            val newPaid = currentPaid + amount
            val settledReturnCredit = returnsForPurchaseInvoice(invoiceId)
            val remainingAfterPayment = CommerceRules.outstandingAfterReturns(
                inv.totalAmount.bd(), newPaid, settledReturnCredit.first, settledReturnCredit.second
            )
            dao.updatePurchaseInvoice(inv.copy(paidAmount = newPaid.toPlainString(), status = if (remainingAfterPayment.compareTo(ZERO) == 0) "PAID" else "POSTED"))
        }
        dao.insertCashTransaction(CashTransactionEntity(accountId = account.id, direction = direction, sourceType = if (isSale) "SALE_RECEIPT" else "PURCHASE_PAYMENT", sourceId = invoiceId, amount = amount.toPlainString(), note = "سداد جزئي للفاتورة $invoiceId"))
        outstanding - amount
    }

    /** Pay a customer the credit remaining after a sale return; records a cash debit atomically. */
    suspend fun refundCustomer(salesReturnId: Long, amountText: String, cashAccountId: Long): BigDecimal = database.withTransaction {
        val returnRow = dao.getSalesReturn(salesReturnId) ?: throw IllegalArgumentException("مرتجع المبيعات غير موجود")
        val line = dao.getSalesLine(returnRow.invoiceLineId) ?: throw IllegalStateException("بند الفاتورة غير موجود")
        val invoice = dao.getSalesInvoice(line.invoiceId) ?: throw IllegalStateException("فاتورة المبيعات غير موجودة")
        val amount = amountText.bd(); require(amount > ZERO) { "قيمة الاسترداد يجب أن تكون موجبة" }
        val account = dao.getCashAccount(cashAccountId) ?: throw IllegalArgumentException("الصندوق غير موجود")
        require(account.active && account.currencyCode == invoice.currencyCode) { "اختر صندوقًا نشطًا بعملة الفاتورة" }
        val invoiceLines = dao.getSalesLines(invoice.id).map { it.id }.toSet()
        val allReturns = dao.getSalesReturns().filter { it.invoiceLineId in invoiceLines }
        val totalReturnCredit = allReturns.fold(ZERO) { sum, row -> sum + row.amount.bd() }
        val totalRefunded = allReturns.fold(ZERO) { sum, row -> sum + row.refundedAmount.bd() }
        val invoiceRefundDue = CommerceRules.refundDueAfterReturns(invoice.totalAmount.bd(), invoice.paidAmount.bd(), totalReturnCredit, totalRefunded)
        val rowRemaining = (returnRow.amount.bd() - returnRow.refundedAmount.bd()).max(ZERO)
        require(amount <= invoiceRefundDue && amount <= rowRemaining) {
            "قيمة الاسترداد تتجاوز المستحق للعميل (المستحق ${invoiceRefundDue.toPlainString()})"
        }
        require(cashBalanceInternal(account.id) >= amount) { "رصيد الصندوق غير كافٍ للاسترداد" }
        dao.updateSalesReturn(returnRow.copy(refundedAmount = (returnRow.refundedAmount.bd() + amount).toPlainString()))
        dao.insertCashTransaction(CashTransactionEntity(
            accountId = account.id, direction = "DEBIT", sourceType = "SALES_RETURN_REFUND",
            sourceId = salesReturnId, amount = amount.toPlainString(), note = "استرداد للعميل عن مرتجع المبيعات"
        ))
        invoiceRefundDue - amount
    }

    /** Record a supplier refund received for returned purchases; cash credit and return settlement are atomic. */
    suspend fun receiveSupplierRefund(purchaseReturnId: Long, amountText: String, cashAccountId: Long): BigDecimal = database.withTransaction {
        val returnRow = dao.getPurchaseReturn(purchaseReturnId) ?: throw IllegalArgumentException("مرتجع المشتريات غير موجود")
        val line = dao.getPurchaseLine(returnRow.invoiceLineId) ?: throw IllegalStateException("بند فاتورة الشراء غير موجود")
        val invoice = dao.getPurchaseInvoice(line.invoiceId) ?: throw IllegalStateException("فاتورة المشتريات غير موجودة")
        val amount = amountText.bd(); require(amount > ZERO) { "قيمة الاسترداد يجب أن تكون موجبة" }
        val account = dao.getCashAccount(cashAccountId) ?: throw IllegalArgumentException("الصندوق غير موجود")
        require(account.active && account.currencyCode == invoice.currencyCode) { "اختر صندوقًا نشطًا بعملة الفاتورة" }
        val invoiceLines = dao.getPurchaseLines(invoice.id).map { it.id }.toSet()
        val allReturns = dao.getPurchaseReturns().filter { it.invoiceLineId in invoiceLines }
        val totalReturnCredit = allReturns.fold(ZERO) { sum, row -> sum + row.amount.bd() }
        val totalRefunded = allReturns.fold(ZERO) { sum, row -> sum + row.refundReceivedAmount.bd() }
        val invoiceRefundDue = CommerceRules.refundDueAfterReturns(invoice.totalAmount.bd(), invoice.paidAmount.bd(), totalReturnCredit, totalRefunded)
        val rowRemaining = (returnRow.amount.bd() - returnRow.refundReceivedAmount.bd()).max(ZERO)
        require(amount <= invoiceRefundDue && amount <= rowRemaining) {
            "قيمة المبلغ المستلم تتجاوز مستحقات المتجر من المورد (المستحق ${invoiceRefundDue.toPlainString()})"
        }
        dao.updatePurchaseReturn(returnRow.copy(refundReceivedAmount = (returnRow.refundReceivedAmount.bd() + amount).toPlainString()))
        dao.insertCashTransaction(CashTransactionEntity(
            accountId = account.id, direction = "CREDIT", sourceType = "PURCHASE_RETURN_REFUND",
            sourceId = purchaseReturnId, amount = amount.toPlainString(), note = "استرداد من المورد عن مرتجع المشتريات"
        ))
        invoiceRefundDue - amount
    }

    suspend fun finalizeStocktake(warehouseId: Long, countedQuantities: Map<Long, String>, note: String = "جرد مخزني"): Long = database.withTransaction {
        val warehouse = dao.getWarehouse(warehouseId) ?: throw IllegalArgumentException("المستودع غير موجود")
        val products = countedQuantities.keys.map { id -> dao.getProduct(id) ?: throw IllegalArgumentException("صنف غير موجود: $id") }
        require(countedQuantities.isNotEmpty()) { "أدخل كميات صنف واحد على الأقل" }
        countedQuantities.forEach { (_, value) -> require(value.bd() >= ZERO) { "الكمية الفعلية لا يمكن أن تكون سالبة" } }
        val stocktakeId = dao.insertStocktake(StocktakeEntity(reference = "ST-${System.currentTimeMillis()}", warehouseId = warehouseId, note = note.trim()))
        for (product in products) {
            val counted = countedQuantities.getValue(product.id).bd()
            val expected = currentStock(product.id, warehouseId)
            val difference = counted - expected
            dao.insertStocktakeLine(StocktakeLineEntity(stocktakeId = stocktakeId, productId = product.id, expectedQuantity = expected.toPlainString(), countedQuantity = counted.toPlainString(), differenceQuantity = difference.toPlainString(), note = note.trim()))
            if (difference.compareTo(ZERO) != 0) {
                dao.insertMovement(StockMovementEntity(
                    productId = product.id, warehouseId = warehouse.id,
                    movementType = if (difference > ZERO) "ADJUSTMENT_IN" else "ADJUSTMENT_OUT",
                    quantity = difference.abs().toPlainString(), unitCostAmount = product.averageCostAmount,
                    currencyCode = product.valuationCurrencyCode, sourceType = "STOCKTAKE", sourceId = stocktakeId, note = note.trim()
                ))
            }
        }
        stocktakeId
    }

    suspend fun transferStock(productId: Long, fromWarehouseId: Long, toWarehouseId: Long, quantityText: String) = database.withTransaction {
        require(fromWarehouseId != toWarehouseId) { "اختر مستودعين مختلفين" }
        val qty = quantityText.bd(); require(qty > ZERO) { "كمية النقل يجب أن تكون موجبة" }
        val product = dao.getProduct(productId) ?: throw IllegalArgumentException("الصنف غير موجود")
        require(dao.getWarehouse(fromWarehouseId)?.active == true && dao.getWarehouse(toWarehouseId)?.active == true) { "يجب اختيار مستودعين نشطين" }
        require(currentStock(productId, fromWarehouseId) >= qty) { "المخزون في المستودع المصدر غير كافٍ" }
        val ref = System.currentTimeMillis()
        dao.insertMovement(StockMovementEntity(productId = productId, warehouseId = fromWarehouseId, movementType = "TRANSFER_OUT", quantity = qty.toPlainString(), unitCostAmount = product.averageCostAmount, currencyCode = product.valuationCurrencyCode, sourceType = "STOCK_TRANSFER", sourceId = ref))
        dao.insertMovement(StockMovementEntity(productId = productId, warehouseId = toWarehouseId, movementType = "TRANSFER_IN", quantity = qty.toPlainString(), unitCostAmount = product.averageCostAmount, currencyCode = product.valuationCurrencyCode, sourceType = "STOCK_TRANSFER", sourceId = ref))
    }

    private suspend fun getOrCreateCashAccount(currencyCode: String): CashAccountEntity {
        return dao.getLatestCashAccountForCurrency(currencyCode) ?: run {
            val id = dao.insertCashAccount(CashAccountEntity(name = "صندوق $currencyCode", currencyCode = currencyCode))
            dao.getCashAccount(id) ?: throw IllegalStateException("تعذر إنشاء الصندوق")
        }
    }

    private suspend fun cashBalanceInternal(accountId: Long): BigDecimal {
        val account = dao.getCashAccount(accountId) ?: throw IllegalArgumentException("الصندوق غير موجود")
        val transactions = dao.getCashTransactions().filter { it.accountId == accountId }
        return transactions.fold(account.openingBalanceAmount.bd()) { acc, transaction ->
            val value = transaction.amount.bd()
            if (transaction.direction == "CREDIT") acc + value else acc - value
        }
    }

    fun observeFinancialSummary(): Flow<FinancialSummary> {
        val inventoryFlow = combine(productsFlow, movementsFlow, currencies.baseCurrencyFlow) { products, movements, base ->
            InventoryReportData(products, movements, base.code)
        }
        val salesFlow = combine(salesInvoicesFlow, dao.observeSalesLines(), dao.observeSalesReturns()) { invoices, lines, returns ->
            SalesReportData(invoices, lines, returns)
        }
        val purchaseFlow = combine(purchaseInvoicesFlow, dao.observePurchaseLines(), dao.observePurchaseReturns(), shipmentsFlow) { invoices, lines, returns, shipments ->
            PurchaseReportData(invoices, lines, returns, shipments)
        }
        val financeFlow = combine(expensesFlow, cashAccountsFlow, cashTransactionsFlow) { expenses, accounts, transactions ->
            FinanceReportData(expenses, accounts, transactions)
        }
        return combine(inventoryFlow, salesFlow, purchaseFlow, financeFlow) { inventory, sales, purchases, finance ->
            val products = inventory.products
            val movements = inventory.movements
            val quantities = products.associate { product ->
                product.id to movements.asSequence().filter { it.productId == product.id }
                    .fold(ZERO) { total, movement -> total + signedQuantity(movement) }
            }
            val lowCount = products.count { product -> (quantities[product.id] ?: ZERO) <= product.minimumStock.bd() }
            val totalQty = quantities.values.fold(ZERO, BigDecimal::add)
            val inventoryVal = products.groupBy { it.valuationCurrencyCode }.map { (code, rows) ->
                CurrencyTotal(code, rows.fold(ZERO) { total, product -> total + (quantities[product.id] ?: ZERO).multiply(product.averageCostAmount.bd()) })
            }.sortedBy { it.currencyCode }
            val replacementVal = products.groupBy { it.priceCurrencyCode }.map { (code, rows) ->
                CurrencyTotal(code, rows.fold(ZERO) { total, product -> total + (quantities[product.id] ?: ZERO).multiply(product.replacementCostAmount.bd()) })
            }.sortedBy { it.currencyCode }
            // Only compare values directly when both are denominated in the same currency.
            // This intentionally avoids inventing an exchange rate for unlike units of account.
            val comparableProducts = products.filter { it.valuationCurrencyCode == it.priceCurrencyCode }
            val replacementVariance = comparableProducts.groupBy { it.valuationCurrencyCode }.map { (code, rows) ->
                CurrencyTotal(code, rows.fold(ZERO) { total, product ->
                    val quantity = quantities[product.id] ?: ZERO
                    total + quantity.multiply(product.replacementCostAmount.bd() - product.averageCostAmount.bd())
                })
            }.sortedBy { it.currencyCode }
            val nonComparableReplacementCount = products.count { it.valuationCurrencyCode != it.priceCurrencyCode }

            val salesLineById = sales.lines.associateBy { it.id }
            val linesByInvoice = sales.lines.groupBy { it.invoiceId }
            val netRevenueBeforeReturns = sales.invoices.fold(ZERO) { total, invoice ->
                val invoiceLinesRevenue = linesByInvoice[invoice.id].orEmpty()
                    .fold(ZERO) { sum, line -> sum + line.revenueBaseAmount.bd() }
                val discountBase = invoice.discountAmount.bd().multiply(invoice.exchangeRateToBase.bd())
                total + invoiceLinesRevenue - discountBase
            }
            val returnsRevenue = sales.returns.fold(ZERO) { total, row -> total + row.amountBase.bd() }
            val netRevenue = netRevenueBeforeReturns - returnsRevenue
            val originalCogs = sales.lines.fold(ZERO) { total, line -> total + line.costTotalBaseAmount.bd() }
            val restoredCogs = sales.returns.fold(ZERO) { total, row -> total + row.costRestoredBaseAmount.bd() }
            val cogs = originalCogs - restoredCogs
            val expenseTotal = finance.expenses.fold(ZERO) { total, expense -> total + expense.amountBase.bd() }
            val unpaidExpenses = finance.expenses.filter { !it.paid }.groupBy { it.currencyCode }.map { (code, rows) ->
                CurrencyTotal(code, rows.fold(ZERO) { total, expense -> total + expense.amount.bd() })
            }.filter { it.amount > ZERO }.sortedBy { it.currencyCode }

            val receivable = sales.invoices.groupBy { it.currencyCode }.map { (code, invoices) ->
                val amount = invoices.fold(ZERO) { total, invoice ->
                    val returns = sales.returns.filter { row -> salesLineById[row.invoiceLineId]?.invoiceId == invoice.id }
                    val returnCredit = returns.fold(ZERO) { sum, row -> sum + row.amount.bd() }
                    val refundPaid = returns.fold(ZERO) { sum, row -> sum + row.refundedAmount.bd() }
                    total + CommerceRules.outstandingAfterReturns(invoice.totalAmount.bd(), invoice.paidAmount.bd(), returnCredit, refundPaid)
                }
                CurrencyTotal(code, amount)
            }.filter { it.amount > ZERO }.sortedBy { it.currencyCode }
            val customerRefundDue = sales.invoices.groupBy { it.currencyCode }.map { (code, invoices) ->
                val amount = invoices.fold(ZERO) { total, invoice ->
                    val returns = sales.returns.filter { row -> salesLineById[row.invoiceLineId]?.invoiceId == invoice.id }
                    val returnCredit = returns.fold(ZERO) { sum, row -> sum + row.amount.bd() }
                    val refundPaid = returns.fold(ZERO) { sum, row -> sum + row.refundedAmount.bd() }
                    total + CommerceRules.refundDueAfterReturns(invoice.totalAmount.bd(), invoice.paidAmount.bd(), returnCredit, refundPaid)
                }
                CurrencyTotal(code, amount)
            }.filter { it.amount > ZERO }.sortedBy { it.currencyCode }

            val purchaseLineById = purchases.lines.associateBy { it.id }
            val payable = purchases.invoices.groupBy { it.currencyCode }.map { (code, invoices) ->
                val amount = invoices.fold(ZERO) { total, invoice ->
                    val returns = purchases.returns.filter { row -> purchaseLineById[row.invoiceLineId]?.invoiceId == invoice.id }
                    val returnCredit = returns.fold(ZERO) { sum, row -> sum + row.amount.bd() }
                    val refundReceived = returns.fold(ZERO) { sum, row -> sum + row.refundReceivedAmount.bd() }
                    total + CommerceRules.outstandingAfterReturns(invoice.totalAmount.bd(), invoice.paidAmount.bd(), returnCredit, refundReceived)
                }
                CurrencyTotal(code, amount)
            }.filter { it.amount > ZERO }.sortedBy { it.currencyCode }
            val supplierRefundDue = purchases.invoices.groupBy { it.currencyCode }.map { (code, invoices) ->
                val amount = invoices.fold(ZERO) { total, invoice ->
                    val returns = purchases.returns.filter { row -> purchaseLineById[row.invoiceLineId]?.invoiceId == invoice.id }
                    val returnCredit = returns.fold(ZERO) { sum, row -> sum + row.amount.bd() }
                    val refundReceived = returns.fold(ZERO) { sum, row -> sum + row.refundReceivedAmount.bd() }
                    total + CommerceRules.refundDueAfterReturns(invoice.totalAmount.bd(), invoice.paidAmount.bd(), returnCredit, refundReceived)
                }
                CurrencyTotal(code, amount)
            }.filter { it.amount > ZERO }.sortedBy { it.currencyCode }

            val cash = finance.accounts.map { account ->
                val balance = finance.transactions.asSequence().filter { it.accountId == account.id }
                    .fold(account.openingBalanceAmount.bd()) { amount, row ->
                        if (row.direction == "CREDIT") amount + row.amount.bd() else amount - row.amount.bd()
                    }
                CashBalance(account, balance)
            }
            FinancialSummary(
                baseCurrencyCode = inventory.baseCode,
                productCount = products.count { it.active },
                lowStockCount = lowCount,
                totalStockQuantity = totalQty,
                inventoryValueByCurrency = inventoryVal,
                replacementValueByCurrency = replacementVal,
                replacementCostVarianceByCurrency = replacementVariance,
                nonComparableReplacementProductCount = nonComparableReplacementCount,
                netRevenueBase = netRevenue,
                costOfGoodsSoldBase = cogs,
                grossProfitBase = netRevenue - cogs,
                expensesBase = expenseTotal,
                netProfitBase = netRevenue - cogs - expenseTotal,
                receivableByCurrency = receivable,
                payableByCurrency = payable,
                unpaidExpensesByCurrency = unpaidExpenses,
                customerRefundDueByCurrency = customerRefundDue,
                supplierRefundDueByCurrency = supplierRefundDue,
                cashBalances = cash,
                shipmentCount = purchases.shipments.size,
                purchaseCount = purchases.invoices.size,
                salesCount = sales.invoices.size
            )
        }
    }

    suspend fun getDetailedPayables(): List<CurrencyTotal> {
        val invoices = dao.getPurchaseInvoices()
        val lines = dao.getPurchaseLines().associateBy { it.id }
        val returns = dao.getPurchaseReturns()
        return invoices.groupBy { it.currencyCode }.map { (code, rows) ->
            val amount = rows.fold(ZERO) { acc, invoice ->
                val invoiceReturns = returns.filter { lines[it.invoiceLineId]?.invoiceId == invoice.id }
                val returned = invoiceReturns.fold(ZERO) { a, r -> a + r.amount.bd() }
                val refundReceived = invoiceReturns.fold(ZERO) { a, r -> a + r.refundReceivedAmount.bd() }
                acc + CommerceRules.outstandingAfterReturns(invoice.totalAmount.bd(), invoice.paidAmount.bd(), returned, refundReceived)
            }
            CurrencyTotal(code, amount)
        }.filter { it.amount > ZERO }
    }
}
