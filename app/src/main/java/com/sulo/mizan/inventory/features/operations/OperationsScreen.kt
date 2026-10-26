package com.sulo.mizan.inventory.features.operations

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.repository.CurrencyRepository
import com.sulo.mizan.inventory.core.repository.InventoryRepository
import com.sulo.mizan.inventory.core.repository.PurchaseLineDraft
import com.sulo.mizan.inventory.core.repository.SaleLineDraft
import com.sulo.mizan.inventory.core.ui.CommerceActionButton
import com.sulo.mizan.inventory.core.ui.CommerceDropdown
import com.sulo.mizan.inventory.core.ui.CommerceSection
import com.sulo.mizan.inventory.core.ui.MizanPageHeader
import com.sulo.mizan.inventory.core.ui.CommerceTextField
import kotlinx.coroutines.launch
import java.math.BigDecimal
import androidx.compose.foundation.layout.PaddingValues
import com.sulo.mizan.inventory.core.ui.MizanMiniStat
import com.sulo.mizan.inventory.core.ui.MizanSectionTabs

data class DraftInvoiceLine(val productId: Long, val productName: String, val quantity: String, val unitPrice: String)

@Composable
fun OperationsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { InventoryRepository(context) }
    val currencyRepository = remember { CurrencyRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var notice by remember { mutableStateOf<String?>(null) }

    val products by repository.productsFlow.collectAsState(initial = emptyList())
    val warehouses by repository.warehousesFlow.collectAsState(initial = emptyList())
    val currencies by currencyRepository.activeCurrenciesFlow.collectAsState(initial = Currency.DEFAULT_ACTIVE_CURRENCIES)
    val base by currencyRepository.baseCurrencyFlow.collectAsState(initial = Currency.DEFAULT_BASE_CURRENCY)
    val purchases by repository.purchaseInvoicesFlow.collectAsState(initial = emptyList())
    val sales by repository.salesInvoicesFlow.collectAsState(initial = emptyList())
    val purchaseLines by repository.purchaseInvoiceLinesFlow.collectAsState(initial = emptyList())
    val salesLines by repository.salesInvoiceLinesFlow.collectAsState(initial = emptyList())
    val salesReturns by repository.salesReturnsFlow.collectAsState(initial = emptyList())
    val purchaseReturns by repository.purchaseReturnsFlow.collectAsState(initial = emptyList())
    val salesReturnQuantities = remember { mutableStateMapOf<Long, String>() }
    val purchaseReturnQuantities = remember { mutableStateMapOf<Long, String>() }
    val movements by repository.movementsFlow.collectAsState(initial = emptyList())
    val shipments by repository.shipmentsFlow.collectAsState(initial = emptyList())

    var isPurchase by remember { mutableStateOf(true) }
    var selectedProductId by remember { mutableStateOf("") }
    var selectedWarehouseId by remember { mutableStateOf("1") }
    var selectedCurrencyCode by remember { mutableStateOf("SDG") }
    var partyName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("0") }
    var exchangeRate by remember { mutableStateOf("1") }
    var shipping by remember { mutableStateOf("0") }
    var shipmentReference by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("0") }
    var tax by remember { mutableStateOf("0") }
    var paid by remember { mutableStateOf("0") }
    val draftLines = remember { mutableStateListOf<DraftInvoiceLine>() }

    var selectedSection by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { repository.ensureInitialized() }
    LaunchedEffect(products) { if (selectedProductId.isBlank() && products.isNotEmpty()) selectedProductId = products.first().id.toString() }
    LaunchedEffect(warehouses) { if (warehouses.isNotEmpty() && warehouses.none { it.id.toString() == selectedWarehouseId }) selectedWarehouseId = warehouses.first().id.toString() }
    LaunchedEffect(currencies, base) { if (currencies.none { it.code == selectedCurrencyCode }) { selectedCurrencyCode = base.code; exchangeRate = "1" } }
    LaunchedEffect(notice) { notice?.let { snackbar.showSnackbar(it); notice = null } }

    val selectedCurrency = currencies.firstOrNull { it.code == selectedCurrencyCode } ?: base
    val fxValid = selectedCurrencyCode != base.code || exchangeRate.toBigDecimalOrNull()?.compareTo(BigDecimal.ONE) == 0

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("operations_screen")) {
        SnackbarHost(snackbar, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MizanPageHeader(
                title = "المبيعات والمشتريات",
                subtitle = "أنشئ الفواتير، أدر المرتجعات، وتابع أثر كل عملية على المخزون والرصيد.",
                eyebrow = "العمليات التجارية",
                icon = Icons.AutoMirrored.Filled.ReceiptLong
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MizanMiniStat(label = "شراء", value = purchases.size.toString(), modifier = Modifier.weight(1f))
                    MizanMiniStat(label = "بيع", value = sales.size.toString(), modifier = Modifier.weight(1f))
                    MizanMiniStat(label = "بنود الفاتورة", value = draftLines.size.toString(), modifier = Modifier.weight(1f))
            }
            MizanSectionTabs(titles = listOf("فاتورة جديدة", "سجل الفواتير", "المرتجعات", "الشحنات"), selectedIndex = selectedSection, onSelect = { selectedSection = it })
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedSection == 0) {
            item {
                CommerceSection("إنشاء فاتورة") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isPurchase, onClick = { isPurchase = true }, label = { Text("مشتريات") })
                        FilterChip(selected = !isPurchase, onClick = { isPurchase = false }, label = { Text("مبيعات") })
                    }
                    CommerceTextField(if (isPurchase) "اسم المورد" else "اسم العميل", partyName, { partyName = it })
                    CommerceDropdown("المستودع", selectedWarehouseId, warehouses.map { it.id.toString() to it.name }, { selectedWarehouseId = it })
                    CommerceDropdown("عملة الفاتورة", selectedCurrencyCode, currencies.map { it.code to "${it.code} — ${it.displayNameAr}" }, {
                        selectedCurrencyCode = it
                        exchangeRate = if (it == base.code) "1" else ""
                    })
                    CommerceTextField("سعر العملة إلى ${base.code}", exchangeRate, { exchangeRate = it }, supportingText = if (selectedCurrencyCode == base.code) "العملة الأساسية: يجب أن يكون السعر 1" else "أدخل السعر الفعلي للمعاملة يدويًا. السعر المرجعي الآلي لا يُستخدم تلقائيًا.")
                    CommerceDropdown("الصنف", selectedProductId, products.map { it.id.toString() to "${it.name} (${it.sku})" }, { selectedProductId = it })
                    CommerceTextField("الكمية", quantity, { quantity = it })
                    CommerceTextField(if (isPurchase) "تكلفة الوحدة بعملة الفاتورة" else "سعر الوحدة بعملة الفاتورة", unitPrice, { unitPrice = it })
                    Spacer(Modifier.height(6.dp))
                    CommerceActionButton("إضافة بند إلى الفاتورة") {
                        runCatching {
                            val id = selectedProductId.toLongOrNull() ?: error("اختر صنفًا")
                            val product = products.firstOrNull { it.id == id } ?: error("الصنف غير موجود")
                            val q = quantity.toBigDecimalOrNull() ?: error("الكمية غير صحيحة")
                            val price = unitPrice.toBigDecimalOrNull() ?: error("السعر غير صحيح")
                            require(q > BigDecimal.ZERO && price >= BigDecimal.ZERO) { "أدخل كمية موجبة وسعرًا غير سالب" }
                            draftLines.add(DraftInvoiceLine(id, product.name, q.toPlainString(), price.toPlainString()))
                            quantity = "1"; unitPrice = "0"
                            notice = "تمت إضافة البند"
                        }.onFailure { notice = it.message ?: "تعذر إضافة البند" }
                    }
                    if (draftLines.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("بنود الفاتورة (${draftLines.size})", fontWeight = FontWeight.Bold)
                        draftLines.forEachIndexed { index, line ->
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("${line.productName}: ${line.quantity} × ${line.unitPrice}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                androidx.compose.material3.TextButton(onClick = { draftLines.removeAt(index) }) { Text("حذف") }
                            }
                        }
                    }
                    if (isPurchase) {
                        CommerceTextField("تكاليف الشحن والجمارك", shipping, { shipping = it })
                        CommerceTextField("رقم الشحنة (اختياري)", shipmentReference, { shipmentReference = it })
                    } else {
                        CommerceTextField("الخصم", discount, { discount = it })
                        CommerceTextField("الضريبة", tax, { tax = it })
                    }
                    CommerceTextField("المبلغ المدفوع الآن", paid, { paid = it })
                    Spacer(Modifier.height(8.dp))
                    CommerceActionButton(if (isPurchase) "ترحيل فاتورة المشتريات" else "ترحيل فاتورة المبيعات", enabled = draftLines.isNotEmpty() && fxValid) {
                        scope.launch {
                            runCatching {
                                val warehouseId = selectedWarehouseId.toLongOrNull() ?: error("اختر مستودعًا")
                                if (isPurchase) {
                                    repository.createPurchaseInvoice(
                                        supplierName = partyName, warehouseId = warehouseId, currencyCode = selectedCurrencyCode,
                                        exchangeRateToBase = exchangeRate, lines = draftLines.map { PurchaseLineDraft(it.productId, it.quantity, it.unitPrice) },
                                        shippingAmount = shipping, paidAmount = paid, shipmentReference = shipmentReference
                                    )
                                } else {
                                    repository.createSalesInvoice(
                                        customerName = partyName, warehouseId = warehouseId, currencyCode = selectedCurrencyCode,
                                        exchangeRateToBase = exchangeRate, lines = draftLines.map { SaleLineDraft(it.productId, it.quantity, it.unitPrice) },
                                        discountAmount = discount, taxAmount = tax, paidAmount = paid
                                    )
                                }
                            }.onSuccess {
                                draftLines.clear(); partyName = ""; shipping = "0"; shipmentReference = ""; discount = "0"; tax = "0"; paid = "0"
                                notice = if (isPurchase) "تم ترحيل فاتورة الشراء وتحديث تكلفة المخزون" else "تم ترحيل فاتورة البيع وخصم المخزون"
                            }.onFailure { notice = it.message ?: "فشل ترحيل الفاتورة؛ لم يتم حفظ أي جزء منها" }
                        }
                    }
                }
            }
            }

            if (selectedSection == 1) {
            item { Text("فواتير المشتريات (${purchases.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            }

            if (selectedSection == 1) {
            items(purchases.take(20), key = { "p-${it.id}" }) { invoice ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${invoice.invoiceNumber} · ${invoice.supplierName}", fontWeight = FontWeight.Bold)
                        val lineIds = purchaseLines.filter { it.invoiceId == invoice.id }.map { it.id }.toSet()
                        val invoiceReturns = purchaseReturns.filter { it.invoiceLineId in lineIds }
                        val returned = invoiceReturns.fold(BigDecimal.ZERO) { acc, row -> acc + row.amount.toBigDecimal() }
                        val refundReceived = invoiceReturns.fold(BigDecimal.ZERO) { acc, row -> acc + row.refundReceivedAmount.toBigDecimal() }
                        val outstanding = (invoice.totalAmount.toBigDecimal() - invoice.paidAmount.toBigDecimal() - returned + refundReceived).max(BigDecimal.ZERO)
                        Text("الإجمالي ${invoice.totalAmount} ${invoice.currencyCode} · المدفوع ${invoice.paidAmount} · المرتجع ${returned.stripTrailingZeros().toPlainString()} · مسترد نقدًا ${refundReceived.stripTrailingZeros().toPlainString()} · المتبقي ${outstanding.stripTrailingZeros().toPlainString()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }

            if (selectedSection == 1) {
            item { Text("فواتير المبيعات (${sales.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            }

            if (selectedSection == 1) {
            items(sales.take(20), key = { "s-${it.id}" }) { invoice ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${invoice.invoiceNumber} · ${invoice.customerName}", fontWeight = FontWeight.Bold)
                        val lineIds = salesLines.filter { it.invoiceId == invoice.id }.map { it.id }.toSet()
                        val invoiceReturns = salesReturns.filter { it.invoiceLineId in lineIds }
                        val returned = invoiceReturns.fold(BigDecimal.ZERO) { acc, row -> acc + row.amount.toBigDecimal() }
                        val refunded = invoiceReturns.fold(BigDecimal.ZERO) { acc, row -> acc + row.refundedAmount.toBigDecimal() }
                        val outstanding = (invoice.totalAmount.toBigDecimal() - invoice.paidAmount.toBigDecimal() - returned + refunded).max(BigDecimal.ZERO)
                        Text("الإجمالي ${invoice.totalAmount} ${invoice.currencyCode} · المدفوع ${invoice.paidAmount} · المرتجع ${returned.stripTrailingZeros().toPlainString()} · مسترد نقدًا ${refunded.stripTrailingZeros().toPlainString()} · المتبقي ${outstanding.stripTrailingZeros().toPlainString()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }

            if (selectedSection == 2) {
            item { Text("مرتجعات المبيعات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            }

            if (selectedSection == 2) {
            items(salesLines.filter { it.returnedQuantity.toBigDecimal() < it.quantity.toBigDecimal() }.take(20), key = { "sr-${it.id}" }) { line ->
                val product = products.firstOrNull { it.id == line.productId }
                val remaining = line.quantity.toBigDecimal() - line.returnedQuantity.toBigDecimal()
                Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${product?.name ?: "صنف"} · الكمية المتاحة للمرتجع $remaining", fontWeight = FontWeight.SemiBold)
                        CommerceTextField("كمية المرتجع", salesReturnQuantities[line.id] ?: remaining.toPlainString(), { salesReturnQuantities[line.id] = it })
                        CommerceActionButton("تسجيل مرتجع المبيعات", enabled = remaining > BigDecimal.ZERO) {
                            scope.launch {
                                runCatching { repository.returnSalesLine(line.id, salesReturnQuantities[line.id] ?: remaining.toPlainString()) }
                                    .onSuccess { salesReturnQuantities.remove(line.id); notice = "تم تسجيل مرتجع المبيعات وإعادة الكمية إلى المخزون" }
                                    .onFailure { notice = it.message ?: "تعذر تسجيل المرتجع" }
                            }
                        }
                    }
                }
            }
            }

            if (selectedSection == 2) {
            item { Text("مرتجعات المشتريات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            }

            if (selectedSection == 2) {
            items(purchaseLines.filter { it.returnedQuantity.toBigDecimal() < it.quantity.toBigDecimal() }.take(20), key = { "pr-${it.id}" }) { line ->
                val product = products.firstOrNull { it.id == line.productId }
                val remaining = line.quantity.toBigDecimal() - line.returnedQuantity.toBigDecimal()
                Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${product?.name ?: "صنف"} · الكمية المتاحة للإرجاع $remaining", fontWeight = FontWeight.SemiBold)
                        CommerceTextField("كمية الإرجاع للمورد", purchaseReturnQuantities[line.id] ?: remaining.toPlainString(), { purchaseReturnQuantities[line.id] = it })
                        CommerceActionButton("تسجيل مرتجع المشتريات", enabled = remaining > BigDecimal.ZERO) {
                            scope.launch {
                                runCatching { repository.returnPurchaseLine(line.id, purchaseReturnQuantities[line.id] ?: remaining.toPlainString()) }
                                    .onSuccess { purchaseReturnQuantities.remove(line.id); notice = "تم تسجيل مرتجع المشتريات وخصم المخزون" }
                                    .onFailure { notice = it.message ?: "تعذر تسجيل المرتجع" }
                            }
                        }
                    }
                }
            }
            }

            if (selectedSection == 3) {
            item {
                Text("الشحنات (${shipments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                shipments.take(10).forEach { shipment ->
                    Text("${shipment.trackingNumber} · ${shipment.supplierName} · ${shipment.costAmount} ${shipment.currencyCode} · ${shipment.status}", style = MaterialTheme.typography.bodySmall)
                }
                Text("حركات المخزون المسجلة: ${movements.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
