package com.sulo.mizan.inventory.core.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.sulo.mizan.inventory.core.database.*
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.model.RateType
import com.sulo.mizan.inventory.core.preferences.AppSettings
import com.sulo.mizan.inventory.core.preferences.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

private const val BACKUP_SCHEMA_VERSION = 3
private const val MAX_BACKUP_CHARS = 50 * 1024 * 1024

/** JSON-safe record for exchange rates, whose domain entity uses BigDecimal and RateType. */
data class ExchangeRateBackup(
    val id: Long,
    val fromCurrencyCode: String,
    val toCurrencyCode: String,
    val rate: String,
    val rateTypeCode: String,
    val timestamp: Long,
    val sourceOrNote: String
)

data class MizanBackupEnvelope(
    val schemaVersion: Int,
    val appId: String,
    val exportedAt: Long,
    val appSettings: AppSettings,
    val systemConfig: List<SystemConfigEntity>,
    val currencies: List<CurrencyConfigEntity>,
    val exchangeRates: List<ExchangeRateBackup>,
    val warehouses: List<WarehouseEntity>,
    val products: List<ProductEntity>,
    val stockMovements: List<StockMovementEntity>,
    val shipments: List<ShipmentEntity>,
    val purchaseInvoices: List<PurchaseInvoiceEntity>,
    val purchaseInvoiceLines: List<PurchaseInvoiceLineEntity>,
    val salesInvoices: List<SalesInvoiceEntity>,
    val salesInvoiceLines: List<SalesInvoiceLineEntity>,
    val salesReturns: List<SalesReturnEntity>,
    val purchaseReturns: List<PurchaseReturnEntity>,
    val expenses: List<ExpenseEntity>,
    val cashAccounts: List<CashAccountEntity>,
    val cashTransactions: List<CashTransactionEntity>,
    val stocktakes: List<StocktakeEntity>,
    val stocktakeLines: List<StocktakeLineEntity>
)

/** Full, versioned, portable JSON backup through Android's Storage Access Framework. */
class MizanBackupService(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val commerce = database.commerceDao()
    private val currencyDao = database.currencyConfigDao()
    private val exchangeDao = database.exchangeRateDao()
    private val systemDao = database.systemConfigDao()
    private val settings = AppSettingsRepository(appContext)
    private val adapter: JsonAdapter<MizanBackupEnvelope> = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(MizanBackupEnvelope::class.java)

    suspend fun exportTo(uri: Uri): Unit = withContext(Dispatchers.IO) {
        val exportedSettings = settings.appSettingsFlow.first()
        val envelope = database.withTransaction {
                MizanBackupEnvelope(
                schemaVersion = BACKUP_SCHEMA_VERSION,
                appId = "com.sulo.mizan.inventory",
                exportedAt = System.currentTimeMillis(),
                appSettings = exportedSettings,
                systemConfig = systemDao.getAll(),
                currencies = currencyDao.getAllCurrencies(),
                exchangeRates = exchangeDao.getAllHistoryDirect().map { rate ->
                    ExchangeRateBackup(
                        id = rate.id,
                        fromCurrencyCode = rate.fromCurrencyCode,
                        toCurrencyCode = rate.toCurrencyCode,
                        rate = rate.rate.toPlainString(),
                        rateTypeCode = rate.rateType.code,
                        timestamp = rate.timestamp,
                        sourceOrNote = rate.sourceOrNote
                    )
                },
                warehouses = commerce.getWarehouses(),
                products = commerce.getProducts(),
                stockMovements = commerce.getMovements(),
                shipments = commerce.getShipments(),
                purchaseInvoices = commerce.getPurchaseInvoices(),
                purchaseInvoiceLines = commerce.getPurchaseLines(),
                salesInvoices = commerce.getSalesInvoices(),
                salesInvoiceLines = commerce.getSalesLines(),
                salesReturns = commerce.getSalesReturns(),
                purchaseReturns = commerce.getPurchaseReturns(),
                expenses = commerce.getExpenses(),
                cashAccounts = commerce.getCashAccounts(),
                cashTransactions = commerce.getCashTransactions(),
                stocktakes = commerce.getStocktakes(),
                stocktakeLines = commerce.getStocktakeLines()
            )
        }
        val json = adapter.toJson(envelope)
        require(json.length <= MAX_BACKUP_CHARS) { "حجم النسخة الاحتياطية أكبر من الحد المسموح (50 ميغابايت)" }
        val output = appContext.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("تعذر فتح ملف النسخة الاحتياطية للكتابة")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
    }

    /** Validate the entire portable dataset before deleting any current table contents. */
    private fun validateBackup(backup: MizanBackupEnvelope) {
        val zero = java.math.BigDecimal.ZERO
        fun decimal(value: String, field: String): java.math.BigDecimal =
            value.toBigDecimalOrNull() ?: throw IllegalArgumentException("قيمة رقمية غير صالحة في $field")
        fun nonNegative(value: String, field: String): java.math.BigDecimal = decimal(value, field).also {
            require(it >= zero) { "لا يجوز أن تكون $field سالبة" }
        }
        fun positive(value: String, field: String): java.math.BigDecimal = decimal(value, field).also {
            require(it > zero) { "يجب أن تكون $field أكبر من الصفر" }
        }
        fun unique(values: List<String>, label: String) {
            require(values.size == values.distinct().size) { "تحتوي النسخة على قيم مكررة في $label" }
        }
        fun validId(id: Long, label: String) { require(id > 0) { "معرّف غير صالح في $label" } }

        require(backup.currencies.isNotEmpty()) { "لا تتضمن النسخة إعدادات العملات" }
        unique(backup.currencies.map { it.code.trim().uppercase() }, "العملات")
        require(backup.currencies.all { it.code.isNotBlank() && it.code == it.code.trim().uppercase() && it.decimals in 0..8 }) { "رمز أو دقة عملة في النسخة الاحتياطية غير صالحين" }
        require(backup.currencies.all { row -> Currency.ALL_SUPPORTED_CURRENCIES.any { it.code == row.code && it.defaultDecimals == row.decimals } }) {
            "تتضمن النسخة رمز عملة غير مدعوم أو دقة لا تطابق إعداد التطبيق"
        }
        require(backup.currencies.count { it.isBase } == 1 && backup.currencies.count { it.isBase && it.isActive } == 1) { "يجب أن تحتوي النسخة على عملة أساسية واحدة نشطة فقط" }
        require(backup.currencies.count { it.isActive } in 1..3) { "عدد العملات النشطة في النسخة غير صالح" }
        val currencyCodes = backup.currencies.map { it.code }.toSet()
        require(backup.systemConfig.map { it.key }.distinct().size == backup.systemConfig.size) { "تحتوي إعدادات النظام على مفاتيح مكررة" }
        unique(backup.exchangeRates.map { it.id.toString() }, "معرّفات أسعار الصرف")
        val validRateTypes = setOf("REFERENCE", "BUY", "SELL", "TRANSACTIONAL")
        backup.exchangeRates.forEach { rate ->
            validId(rate.id, "سعر الصرف")
            require(rate.fromCurrencyCode in currencyCodes && rate.toCurrencyCode in currencyCodes && rate.fromCurrencyCode != rate.toCurrencyCode) { "زوج عملات غير صالح في سجل أسعار الصرف" }
            positive(rate.rate, "سعر الصرف")
            require(rate.rateTypeCode.uppercase() in validRateTypes) { "نوع سعر صرف غير معروف" }
        }

        require(backup.warehouses.isNotEmpty()) { "يجب أن تحتوي النسخة على مستودع واحد على الأقل" }
        unique(backup.warehouses.map { it.id.toString() }, "معرّفات المستودعات")
        backup.warehouses.forEach { validId(it.id, "المستودع"); require(it.name.isNotBlank()) { "اسم مستودع فارغ" } }
        val warehouseIds = backup.warehouses.map { it.id }.toSet()
        unique(backup.products.map { it.id.toString() }, "معرّفات الأصناف")
        unique(backup.products.map { it.sku.trim().uppercase() }, "رموز الأصناف SKU")
        unique(backup.products.mapNotNull { it.barcode?.trim()?.takeIf(String::isNotEmpty) }, "الباركود")
        backup.products.forEach { product ->
            validId(product.id, "الصنف")
            require(product.sku.isNotBlank() && product.name.isNotBlank() && product.unitName.isNotBlank()) { "تحتوي النسخة على صنف بلا رمز أو اسم أو وحدة" }
            nonNegative(product.minimumStock, "حد إعادة الطلب")
            nonNegative(product.averageCostAmount, "متوسط التكلفة")
            nonNegative(product.sellingPriceAmount, "سعر البيع")
            nonNegative(product.replacementCostAmount, "تكلفة الاستبدال")
            require(product.valuationCurrencyCode in currencyCodes && product.priceCurrencyCode in currencyCodes) { "عملة تقييم أو تسعير غير موجودة ضمن إعدادات النسخة" }
        }
        val productIds = backup.products.map { it.id }.toSet()
        val movementTypes = setOf("OPENING", "PURCHASE", "SALE", "SALE_RETURN", "PURCHASE_RETURN", "ADJUSTMENT_IN", "ADJUSTMENT_OUT", "TRANSFER_IN", "TRANSFER_OUT")
        unique(backup.stockMovements.map { it.id.toString() }, "معرّفات حركات المخزون")
        backup.stockMovements.forEach { row ->
            validId(row.id, "حركة المخزون")
            require(row.productId in productIds && row.warehouseId in warehouseIds) { "حركة مخزون تشير إلى صنف أو مستودع غير موجود" }
            positive(row.quantity, "كمية حركة المخزون")
            nonNegative(row.unitCostAmount, "تكلفة حركة المخزون")
            require(row.currencyCode in currencyCodes && row.movementType in movementTypes) { "نوع حركة أو عملة غير صالحة" }
        }
        val warehouseStock = mutableMapOf<Pair<Long, Long>, java.math.BigDecimal>()
        backup.stockMovements.forEach { row ->
            val key = row.productId to row.warehouseId
            val q = decimal(row.quantity, "كمية الحركة")
            val signed = if (row.movementType in setOf("OPENING", "PURCHASE", "SALE_RETURN", "ADJUSTMENT_IN", "TRANSFER_IN")) q else q.negate()
            warehouseStock[key] = (warehouseStock[key] ?: zero) + signed
        }
        require(warehouseStock.values.all { it >= zero }) { "تتضمن النسخة مستودعًا برصيد مخزون نهائي سالب" }

        unique(backup.shipments.map { it.id.toString() }, "معرّفات الشحنات")
        unique(backup.purchaseInvoices.map { it.id.toString() }, "معرّفات فواتير الشراء")
        unique(backup.purchaseInvoices.map { it.invoiceNumber }, "أرقام فواتير الشراء")
        val shipmentIds = backup.shipments.map { it.id }.toSet()
        backup.shipments.forEach { row ->
            validId(row.id, "الشحنة"); require(row.trackingNumber.isNotBlank()) { "رقم شحنة فارغ" }
            nonNegative(row.costAmount, "تكلفة الشحنة"); nonNegative(row.costBaseAmount, "تكلفة الشحنة بالعملة الأساسية")
            positive(row.exchangeRateToBase, "سعر تحويل الشحنة")
            require(row.currencyCode in currencyCodes) { "عملة شحنة غير موجودة" }
        }
        backup.purchaseInvoices.forEach { row ->
            validId(row.id, "فاتورة الشراء"); require(row.supplierName.isNotBlank() && row.warehouseId in warehouseIds) { "فاتورة شراء بلا مورد أو مستودع صالح" }
            require(row.shipmentId == null || row.shipmentId in shipmentIds) { "فاتورة شراء تشير إلى شحنة غير موجودة" }
            require(row.currencyCode in currencyCodes) { "عملة فاتورة شراء غير موجودة" }
            positive(row.exchangeRateToBase, "سعر تحويل فاتورة الشراء")
            nonNegative(row.subtotalAmount, "مجموع فاتورة الشراء"); nonNegative(row.shippingAmount, "شحن فاتورة الشراء")
            nonNegative(row.totalAmount, "إجمالي فاتورة الشراء"); nonNegative(row.totalBaseAmount, "إجمالي فاتورة الشراء بالعملة الأساسية")
            val subtotal = decimal(row.subtotalAmount, "مجموع فاتورة الشراء")
            val shipping = decimal(row.shippingAmount, "شحن فاتورة الشراء")
            val expectedTotal = (subtotal + shipping).setScale(Currency.fromCode(row.currencyCode).defaultDecimals, java.math.RoundingMode.HALF_UP)
            require(expectedTotal.compareTo(decimal(row.totalAmount, "إجمالي فاتورة الشراء")) == 0) { "إجمالي فاتورة الشراء لا يطابق المجموع والشحن" }
            val paid = nonNegative(row.paidAmount, "مدفوع فاتورة الشراء")
            require(paid <= decimal(row.totalAmount, "إجمالي فاتورة الشراء")) { "المدفوع يتجاوز إجمالي فاتورة شراء" }
        }
        val purchaseIds = backup.purchaseInvoices.map { it.id }.toSet()
        unique(backup.purchaseInvoiceLines.map { it.id.toString() }, "معرّفات بنود الشراء")
        backup.purchaseInvoiceLines.forEach { row ->
            validId(row.id, "بند شراء"); require(row.invoiceId in purchaseIds && row.productId in productIds) { "بند شراء يشير إلى فاتورة أو صنف غير موجود" }
            val quantity = positive(row.quantity, "كمية بند الشراء")
            nonNegative(row.unitCostAmount, "تكلفة وحدة الشراء"); nonNegative(row.lineTotalAmount, "إجمالي بند الشراء"); nonNegative(row.unitCostBaseAmount, "تكلفة الوحدة الواصلة")
            val returned = nonNegative(row.returnedQuantity, "كمية مرتجع الشراء")
            require(returned <= quantity) { "كمية المرتجع تتجاوز كمية بند الشراء" }
        }
        unique(backup.salesInvoices.map { it.id.toString() }, "معرّفات فواتير البيع")
        unique(backup.salesInvoices.map { it.invoiceNumber }, "أرقام فواتير البيع")
        backup.salesInvoices.forEach { row ->
            validId(row.id, "فاتورة البيع"); require(row.customerName.isNotBlank() && row.warehouseId in warehouseIds && row.currencyCode in currencyCodes) { "فاتورة بيع بلا عميل أو مستودع أو عملة صالحة" }
            positive(row.exchangeRateToBase, "سعر تحويل فاتورة البيع")
            nonNegative(row.subtotalAmount, "مجموع فاتورة البيع"); nonNegative(row.discountAmount, "خصم فاتورة البيع")
            nonNegative(row.taxAmount, "ضريبة فاتورة البيع"); nonNegative(row.totalAmount, "إجمالي فاتورة البيع"); nonNegative(row.totalBaseAmount, "إجمالي فاتورة البيع بالعملة الأساسية")
            val subtotal = decimal(row.subtotalAmount, "مجموع فاتورة البيع")
            val discount = decimal(row.discountAmount, "خصم فاتورة البيع")
            val tax = decimal(row.taxAmount, "ضريبة فاتورة البيع")
            require(discount <= subtotal) { "خصم فاتورة البيع يتجاوز المجموع" }
            require(subtotal.compareTo(zero) != 0 || (discount.compareTo(zero) == 0 && tax.compareTo(zero) == 0)) { "فاتورة بيع بصافي بنود صفري لا يجوز أن تتضمن خصمًا أو ضريبة" }
            val expectedTotal = (subtotal - discount + tax).setScale(Currency.fromCode(row.currencyCode).defaultDecimals, java.math.RoundingMode.HALF_UP)
            require(expectedTotal.compareTo(decimal(row.totalAmount, "إجمالي فاتورة البيع")) == 0) { "إجمالي فاتورة البيع لا يطابق المجموع والخصم والضريبة" }
            val paid = nonNegative(row.paidAmount, "مدفوع فاتورة البيع")
            require(paid <= decimal(row.totalAmount, "إجمالي فاتورة البيع")) { "المدفوع يتجاوز إجمالي فاتورة بيع" }
        }
        val salesIds = backup.salesInvoices.map { it.id }.toSet()
        val salesInvoiceById = backup.salesInvoices.associateBy { it.id }
        unique(backup.salesInvoiceLines.map { it.id.toString() }, "معرّفات بنود البيع")
        backup.salesInvoiceLines.forEach { row ->
            validId(row.id, "بند بيع"); require(row.invoiceId in salesIds && row.productId in productIds) { "بند بيع يشير إلى فاتورة أو صنف غير موجود" }
            val quantity = positive(row.quantity, "كمية بند البيع")
            nonNegative(row.unitPriceAmount, "سعر وحدة البيع"); nonNegative(row.lineTotalAmount, "إجمالي بند البيع")
            nonNegative(row.revenueBaseAmount, "إيراد بند البيع بالعملة الأساسية"); nonNegative(row.unitCostBaseAmount, "تكلفة وحدة البيع")
            nonNegative(row.costTotalBaseAmount, "تكلفة بند البيع")
            require(nonNegative(row.returnedQuantity, "كمية مرتجع البيع") <= quantity) { "كمية المرتجع تتجاوز كمية بند البيع" }
        }
        val salesLineById = backup.salesInvoiceLines.associateBy { it.id }
        val purchaseLineById = backup.purchaseInvoiceLines.associateBy { it.id }
        val purchaseInvoiceById = backup.purchaseInvoices.associateBy { it.id }

        // Protect restores against internally inconsistent invoice headers/lines.
        backup.purchaseInvoices.forEach { invoice ->
            val lines = backup.purchaseInvoiceLines.filter { it.invoiceId == invoice.id }
            require(lines.isNotEmpty()) { "فاتورة شراء بلا بنود" }
            val currencyScale = Currency.fromCode(invoice.currencyCode).defaultDecimals
            val calculatedSubtotal = lines.fold(zero) { sum, line ->
                val quantity = decimal(line.quantity, "كمية بند الشراء")
                val unitCost = decimal(line.unitCostAmount, "تكلفة وحدة الشراء")
                val expectedLineTotal = quantity.multiply(unitCost).setScale(currencyScale, java.math.RoundingMode.HALF_UP)
                require(expectedLineTotal.compareTo(decimal(line.lineTotalAmount, "إجمالي بند الشراء")) == 0) {
                    "إجمالي أحد بنود الشراء لا يطابق الكمية وتكلفة الوحدة"
                }
                sum + quantity.multiply(unitCost)
            }.setScale(currencyScale, java.math.RoundingMode.HALF_UP)
            require(calculatedSubtotal.compareTo(decimal(invoice.subtotalAmount, "مجموع فاتورة الشراء")) == 0) {
                "مجموع بنود الشراء لا يطابق مجموع الفاتورة"
            }
        }
        backup.salesInvoices.forEach { invoice ->
            val lines = backup.salesInvoiceLines.filter { it.invoiceId == invoice.id }
            require(lines.isNotEmpty()) { "فاتورة بيع بلا بنود" }
            val currencyScale = Currency.fromCode(invoice.currencyCode).defaultDecimals
            val calculatedSubtotal = lines.fold(zero) { sum, line ->
                val quantity = decimal(line.quantity, "كمية بند البيع")
                val unitPrice = decimal(line.unitPriceAmount, "سعر وحدة البيع")
                val expectedLineTotal = quantity.multiply(unitPrice).setScale(currencyScale, java.math.RoundingMode.HALF_UP)
                require(expectedLineTotal.compareTo(decimal(line.lineTotalAmount, "إجمالي بند البيع")) == 0) {
                    "إجمالي أحد بنود البيع لا يطابق الكمية وسعر الوحدة"
                }
                sum + quantity.multiply(unitPrice)
            }.setScale(currencyScale, java.math.RoundingMode.HALF_UP)
            require(calculatedSubtotal.compareTo(decimal(invoice.subtotalAmount, "مجموع فاتورة البيع")) == 0) {
                "مجموع بنود البيع لا يطابق مجموع الفاتورة"
            }
        }

        unique(backup.salesReturns.map { it.id.toString() }, "معرّفات مرتجعات البيع")
        backup.salesReturns.forEach { row ->
            validId(row.id, "مرتجع بيع")
            val line = salesLineById[row.invoiceLineId] ?: throw IllegalArgumentException("مرتجع بيع يشير إلى بند غير موجود")
            val invoice = salesInvoiceById[line.invoiceId] ?: throw IllegalArgumentException("مرتجع بيع يشير إلى فاتورة غير موجودة")
            require(row.productId == line.productId && row.warehouseId == invoice.warehouseId && row.currencyCode == invoice.currencyCode) { "بيانات مرتجع بيع لا تطابق بند الفاتورة أو مستودعها أو عملتها" }
            positive(row.quantity, "كمية مرتجع البيع"); nonNegative(row.amount, "قيمة مرتجع البيع")
            nonNegative(row.amountBase, "قيمة مرتجع البيع بالعملة الأساسية"); nonNegative(row.costRestoredBaseAmount, "تكلفة مستردة من مرتجع البيع")
            positive(row.exchangeRateToBase, "سعر تحويل مرتجع البيع")
            val refunded = nonNegative(row.refundedAmount, "المبلغ المسترد للعميل")
            require(refunded <= decimal(row.amount, "قيمة مرتجع البيع")) { "المبلغ المسترد للعميل يتجاوز قيمة المرتجع" }
        }
        val returnedSalesQty = backup.salesReturns.groupBy { it.invoiceLineId }.mapValues { (_, rows) -> rows.fold(zero) { sum, row -> sum + decimal(row.quantity, "كمية مرتجع البيع") } }
        // Compare every line, including lines with no return records; otherwise a backup could
        // claim a non-zero returnedQuantity while omitting the corresponding return transaction.
        salesLineById.forEach { (lineId, line) ->
            val recordedReturns = returnedSalesQty[lineId] ?: zero
            require(recordedReturns.compareTo(decimal(line.returnedQuantity, "كمية مرتجع البيع المسجلة")) == 0) {
                "كمية المرتجعات لا تطابق الكمية المرتجعة المسجلة على بند البيع"
            }
        }
        unique(backup.purchaseReturns.map { it.id.toString() }, "معرّفات مرتجعات الشراء")
        backup.purchaseReturns.forEach { row ->
            validId(row.id, "مرتجع شراء")
            val line = purchaseLineById[row.invoiceLineId] ?: throw IllegalArgumentException("مرتجع شراء يشير إلى بند غير موجود")
            val invoice = purchaseInvoiceById[line.invoiceId] ?: throw IllegalArgumentException("مرتجع شراء يشير إلى فاتورة غير موجودة")
            require(row.productId == line.productId && row.warehouseId == invoice.warehouseId && row.currencyCode == invoice.currencyCode) { "بيانات مرتجع شراء لا تطابق بند الفاتورة أو مستودعها أو عملتها" }
            positive(row.quantity, "كمية مرتجع الشراء"); nonNegative(row.amount, "قيمة مرتجع الشراء")
            nonNegative(row.amountBase, "قيمة مرتجع الشراء بالعملة الأساسية"); nonNegative(row.costRemovedBaseAmount, "تكلفة مخزون مرتجع الشراء")
            positive(row.exchangeRateToBase, "سعر تحويل مرتجع الشراء")
            val received = nonNegative(row.refundReceivedAmount, "المبلغ المسترد من المورد")
            require(received <= decimal(row.amount, "قيمة مرتجع الشراء")) { "المبلغ المسترد من المورد يتجاوز قيمة المرتجع" }
        }
        val returnedPurchaseQty = backup.purchaseReturns.groupBy { it.invoiceLineId }.mapValues { (_, rows) -> rows.fold(zero) { sum, row -> sum + decimal(row.quantity, "كمية مرتجع الشراء") } }
        purchaseLineById.forEach { (lineId, line) ->
            val recordedReturns = returnedPurchaseQty[lineId] ?: zero
            require(recordedReturns.compareTo(decimal(line.returnedQuantity, "كمية مرتجع الشراء المسجلة")) == 0) {
                "كمية المرتجعات لا تطابق الكمية المرتجعة المسجلة على بند الشراء"
            }
        }

        require(backup.cashAccounts.isNotEmpty()) { "يجب أن تحتوي النسخة على صندوق نقدي واحد على الأقل" }
        unique(backup.cashAccounts.map { it.id.toString() }, "معرّفات الصناديق")
        unique(backup.cashAccounts.map { it.name.trim().lowercase() + "|" + it.currencyCode }, "أسماء الصناديق وعملاتها")
        backup.cashAccounts.forEach { row -> validId(row.id, "الصندوق"); require(row.name.isNotBlank() && row.currencyCode in currencyCodes) { "بيانات صندوق غير صالحة" }; nonNegative(row.openingBalanceAmount, "الرصيد الافتتاحي للصندوق") }
        val accountIds = backup.cashAccounts.map { it.id }.toSet()
        unique(backup.cashTransactions.map { it.id.toString() }, "معرّفات الحركات النقدية")
        backup.cashTransactions.forEach { row ->
            validId(row.id, "حركة نقدية"); require(row.accountId in accountIds) { "حركة نقدية تشير إلى صندوق غير موجود" }
            require(row.direction == "CREDIT" || row.direction == "DEBIT") { "اتجاه حركة نقدية غير صالح" }
            positive(row.amount, "قيمة الحركة النقدية")
        }
        backup.salesReturns.forEach { row ->
            val settledCash = backup.cashTransactions.filter { it.sourceType == "SALES_RETURN_REFUND" && it.sourceId == row.id && it.direction == "DEBIT" }
                .fold(zero) { total, transaction -> total + decimal(transaction.amount, "استرداد نقدي للعميل") }
            require(settledCash.compareTo(decimal(row.refundedAmount, "مبلغ استرداد العميل")) == 0) { "تسوية مرتجع البيع لا تطابق حركة الصندوق" }
        }
        backup.purchaseReturns.forEach { row ->
            val settledCash = backup.cashTransactions.filter { it.sourceType == "PURCHASE_RETURN_REFUND" && it.sourceId == row.id && it.direction == "CREDIT" }
                .fold(zero) { total, transaction -> total + decimal(transaction.amount, "استرداد نقدي من المورد") }
            require(settledCash.compareTo(decimal(row.refundReceivedAmount, "مبلغ استرداد المورد")) == 0) { "تسوية مرتجع الشراء لا تطابق حركة الصندوق" }
        }
        backup.cashAccounts.forEach { account ->
            val net = backup.cashTransactions.filter { it.accountId == account.id }.fold(decimal(account.openingBalanceAmount, "الرصيد الافتتاحي")) { balance, row ->
                if (row.direction == "CREDIT") balance + decimal(row.amount, "الحركة النقدية") else balance - decimal(row.amount, "الحركة النقدية")
            }
            require(net >= zero) { "رصيد الصندوق ${account.name} سالب في النسخة" }
        }
        unique(backup.expenses.map { it.id.toString() }, "معرّفات المصروفات")
        backup.expenses.forEach { row ->
            validId(row.id, "مصروف"); require(row.category.isNotBlank() && row.description.isNotBlank() && row.currencyCode in currencyCodes) { "بيانات مصروف غير صالحة" }
            positive(row.amount, "قيمة المصروف"); positive(row.exchangeRateToBase, "سعر تحويل المصروف"); nonNegative(row.amountBase, "قيمة المصروف بالعملة الأساسية")
            require(row.cashAccountId == null || row.cashAccountId in accountIds) { "مصروف يشير إلى صندوق غير موجود" }
        }
        unique(backup.stocktakes.map { it.id.toString() }, "معرّفات الجرد")
        val stocktakeIds = backup.stocktakes.map { it.id }.toSet()
        backup.stocktakes.forEach { row -> validId(row.id, "جرد"); require(row.warehouseId in warehouseIds) { "جرد يشير إلى مستودع غير موجود" } }
        unique(backup.stocktakeLines.map { it.id.toString() }, "معرّفات بنود الجرد")
        unique(backup.stocktakeLines.map { "${it.stocktakeId}|${it.productId}" }, "الأصناف داخل الجرد الواحد")
        backup.stocktakeLines.forEach { row ->
            validId(row.id, "بند جرد"); require(row.stocktakeId in stocktakeIds && row.productId in productIds) { "بند جرد يشير إلى عنصر غير موجود" }
            val expected = nonNegative(row.expectedQuantity, "الكمية المتوقعة")
            val counted = nonNegative(row.countedQuantity, "الكمية المعدودة")
            require(decimal(row.differenceQuantity, "فرق الجرد").compareTo(counted - expected) == 0) { "فرق الجرد لا يطابق الكمية المعدودة والمتوقعة" }
        }
    }

    suspend fun restoreFrom(uri: Uri): Unit = withContext(Dispatchers.IO) {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("تعذر فتح ملف النسخة الاحتياطية")
        val json = input.bufferedReader(Charsets.UTF_8).use { reader ->
            val output = StringBuilder()
            val buffer = CharArray(8 * 1024)
            var total = 0
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_BACKUP_CHARS) { "ملف النسخة الاحتياطية أكبر من 50 ميغابايت" }
                output.append(buffer, 0, count)
            }
            output.toString()
        }
        val backup = adapter.fromJson(json) ?: throw IllegalArgumentException("ملف النسخة الاحتياطية فارغ أو غير صالح")
        require(backup.schemaVersion in 1..BACKUP_SCHEMA_VERSION) { "إصدار النسخة الاحتياطية غير مدعوم: ${backup.schemaVersion}" }
        require(backup.appId == "com.sulo.mizan.inventory") { "هذا الملف ليس نسخة احتياطية من ميزان المخزون" }
        validateBackup(backup)

        database.withTransaction {
            // Clear tables in reverse logical dependency order, then restore explicit IDs.
            commerce.clearStocktakeLines()
            commerce.clearStocktakes()
            commerce.clearCashTransactions()
            commerce.clearExpenses()
            commerce.clearSalesReturns()
            commerce.clearPurchaseReturns()
            commerce.clearSalesLines()
            commerce.clearSalesInvoices()
            commerce.clearPurchaseLines()
            commerce.clearPurchaseInvoices()
            commerce.clearShipments()
            commerce.clearMovements()
            commerce.clearProducts()
            commerce.clearWarehouses()
            commerce.clearCashAccounts()
            exchangeDao.clearAllRates()
            currencyDao.clearAllCurrencies()
            systemDao.clearAll()

            systemDao.setAll(backup.systemConfig)
            currencyDao.insertOrUpdateAll(backup.currencies)
            backup.exchangeRates.forEach { row ->
                exchangeDao.insertRate(
                    ExchangeRateEntity(
                        id = row.id,
                        fromCurrencyCode = row.fromCurrencyCode,
                        toCurrencyCode = row.toCurrencyCode,
                        rate = row.rate.toBigDecimal(),
                        rateType = RateType.fromCode(row.rateTypeCode),
                        timestamp = row.timestamp,
                        sourceOrNote = row.sourceOrNote
                    )
                )
            }
            commerce.restoreWarehouses(backup.warehouses)
            commerce.restoreProducts(backup.products)
            commerce.restoreMovements(backup.stockMovements)
            commerce.restoreShipments(backup.shipments)
            commerce.restorePurchaseInvoices(backup.purchaseInvoices)
            commerce.restorePurchaseLines(backup.purchaseInvoiceLines)
            commerce.restoreSalesInvoices(backup.salesInvoices)
            commerce.restoreSalesLines(backup.salesInvoiceLines)
            commerce.restoreSalesReturns(backup.salesReturns)
            commerce.restorePurchaseReturns(backup.purchaseReturns)
            commerce.restoreExpenses(backup.expenses)
            commerce.restoreCashAccounts(backup.cashAccounts)
            commerce.restoreCashTransactions(backup.cashTransactions)
            commerce.restoreStocktakes(backup.stocktakes)
            commerce.restoreStocktakeLines(backup.stocktakeLines)
        }
        settings.setMerchantName(backup.appSettings.merchantName)
        settings.setThemeMode(backup.appSettings.themeMode)
    }

}
