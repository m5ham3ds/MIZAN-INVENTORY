package com.sulo.mizan.inventory.features.inventory

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sulo.mizan.inventory.core.repository.InventoryRepository
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

@Composable
fun InventoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { InventoryRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var notice by remember { mutableStateOf<String?>(null) }

    val warehouses by repository.warehousesFlow.collectAsState(initial = emptyList())
    val products by repository.productsFlow.collectAsState(initial = emptyList())
    var selectedWarehouse by remember { mutableStateOf("1") }
    val balancesFlow = remember(selectedWarehouse) { repository.observeStockBalances(selectedWarehouse.toLongOrNull()) }
    val balances by balancesFlow.collectAsState(initial = emptyList())

    var warehouseName by remember { mutableStateOf("") }
    var warehouseLocation by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("عام") }
    var unit by remember { mutableStateOf("قطعة") }
    var minimumStock by remember { mutableStateOf("0") }
    var openingStock by remember { mutableStateOf("0") }
    var sellingPrice by remember { mutableStateOf("0") }
    var replacementCost by remember { mutableStateOf("0") }
    val stocktakeDraft = remember { mutableStateMapOf<Long, String>() }
    var pricingProductId by remember { mutableStateOf("") }
    var pricingSellingPrice by remember { mutableStateOf("") }
    var pricingReplacementCost by remember { mutableStateOf("") }
    var transferProductId by remember { mutableStateOf("") }
    var transferFromWarehouseId by remember { mutableStateOf("1") }
    var transferToWarehouseId by remember { mutableStateOf("") }
    var transferQuantity by remember { mutableStateOf("1") }

    var selectedSection by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { runCatching { repository.ensureInitialized() }.onFailure { notice = it.message ?: "تعذر تهيئة المخزون" } }
    LaunchedEffect(warehouses) {
        if (warehouses.isNotEmpty() && warehouses.none { it.id.toString() == selectedWarehouse }) selectedWarehouse = warehouses.first().id.toString()
    }
    LaunchedEffect(products) {
        if (products.isNotEmpty() && products.none { it.id.toString() == transferProductId }) transferProductId = products.first().id.toString()
        if (products.isNotEmpty() && products.none { it.id.toString() == pricingProductId }) pricingProductId = products.first().id.toString()
    }
    LaunchedEffect(pricingProductId, products.size) {
        val selected = products.firstOrNull { it.id.toString() == pricingProductId }
        if (selected != null) {
            pricingSellingPrice = selected.sellingPriceAmount
            pricingReplacementCost = selected.replacementCostAmount
        }
    }
    LaunchedEffect(warehouses) {
        if (warehouses.isNotEmpty() && warehouses.none { it.id.toString() == transferFromWarehouseId }) transferFromWarehouseId = warehouses.first().id.toString()
        if (warehouses.size > 1 && (warehouses.none { it.id.toString() == transferToWarehouseId } || transferToWarehouseId == transferFromWarehouseId)) {
            transferToWarehouseId = warehouses.firstOrNull { it.id.toString() != transferFromWarehouseId }?.id?.toString().orEmpty()
        }
    }
    LaunchedEffect(notice) { notice?.let { snackbar.showSnackbar(it); notice = null } }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("inventory_screen")) {
        SnackbarHost(snackbar, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MizanPageHeader(
                title = "إدارة المخزون",
                subtitle = "الأصناف والمستودعات والأرصدة والجرد والتسويات.",
                eyebrow = "المخزون والمستودعات"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MizanMiniStat(label = "الأصناف", value = products.size.toString(), modifier = Modifier.weight(1f))
                    MizanMiniStat(label = "منخفض", value = balances.count { it.isLowStock }.toString(), modifier = Modifier.weight(1f), warning = balances.any { it.isLowStock })
                    MizanMiniStat(label = "المستودعات", value = warehouses.size.toString(), modifier = Modifier.weight(1f))
            }
            MizanSectionTabs(titles = listOf("الأرصدة", "إضافة صنف", "المستودعات", "أدوات المخزون"), selectedIndex = selectedSection, onSelect = { selectedSection = it })
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedSection == 2) {
            item {
                CommerceSection("إنشاء مستودع") {
                    CommerceTextField("اسم المستودع", warehouseName, { warehouseName = it })
                    CommerceTextField("الموقع / ملاحظة", warehouseLocation, { warehouseLocation = it })
                    Spacer(Modifier.height(8.dp))
                    CommerceActionButton("إضافة المستودع") {
                        scope.launch {
                            runCatching { repository.createWarehouse(warehouseName, warehouseLocation) }
                                .onSuccess { warehouseName = ""; warehouseLocation = ""; notice = "تم إنشاء المستودع" }
                                .onFailure { notice = it.message ?: "تعذر إنشاء المستودع" }
                        }
                    }
                    warehouses.forEach { warehouse ->
                        Text("• ${warehouse.name}${if (warehouse.location.isNotBlank()) " — ${warehouse.location}" else ""}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }

            if (selectedSection == 1) {
            item {
                CommerceSection("إضافة صنف") {
                    CommerceTextField("رمز الصنف SKU (فريد)", sku, { sku = it })
                    CommerceTextField("الباركود (اختياري)", barcode, { barcode = it })
                    CommerceTextField("اسم الصنف", productName, { productName = it })
                    CommerceTextField("الفئة", category, { category = it })
                    CommerceTextField("وحدة القياس", unit, { unit = it })
                    CommerceTextField("حد إعادة الطلب", minimumStock, { minimumStock = it }, supportingText = "يمكن إدخال كمية عشرية مثل 2.5")
                    CommerceTextField("الرصيد الافتتاحي", openingStock, { openingStock = it })
                    CommerceTextField("سعر البيع للوحدة (${runCatching { products.firstOrNull()?.priceCurrencyCode ?: "العملة الأساسية" }.getOrDefault("العملة الأساسية")})", sellingPrice, { sellingPrice = it })
                    CommerceTextField("تكلفة الاستبدال الحالية", replacementCost, { replacementCost = it })
                    CommerceDropdown("مستودع الرصيد الافتتاحي", selectedWarehouse, warehouses.map { it.id.toString() to it.name }, { selectedWarehouse = it })
                    Spacer(Modifier.height(8.dp))
                    CommerceActionButton("حفظ الصنف") {
                        scope.launch {
                            runCatching {
                                repository.createProduct(
                                    sku = sku, name = productName, category = category, unitName = unit,
                                    minimumStock = minimumStock, sellingPrice = sellingPrice,
                                    replacementCost = replacementCost, barcode = barcode,
                                    openingQuantity = openingStock, warehouseId = selectedWarehouse.toLongOrNull()
                                )
                            }.onSuccess {
                                sku = ""; barcode = ""; productName = ""; category = "عام"; unit = "قطعة"
                                minimumStock = "0"; openingStock = "0"; sellingPrice = "0"; replacementCost = "0"
                                notice = "تم حفظ الصنف"
                            }.onFailure { notice = it.message ?: "تعذر حفظ الصنف؛ تحقق من عدم تكرار SKU أو الباركود" }
                        }
                    }
                }
            }
            }

            if (selectedSection == 0) {
            item {
                CommerceSection("الأرصدة الحالية") {
                    CommerceDropdown("المستودع", selectedWarehouse, warehouses.map { it.id.toString() to it.name }, { selectedWarehouse = it })
                    Spacer(Modifier.height(6.dp))
                    Text("عدد الأصناف: ${balances.size} | المنخفضة/تحت الحد: ${balances.count { it.isLowStock }}", style = MaterialTheme.typography.labelMedium)
                }
            }
            }

            if (selectedSection == 0) {
            items(balances, key = { it.product.id }) { balance ->
                Surface(color = if (balance.isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(balance.product.name, fontWeight = FontWeight.Bold)
                                Text("SKU: ${balance.product.sku} · ${balance.product.category}", style = MaterialTheme.typography.labelSmall)
                            }
                            Text(balance.quantity.stripTrailingZeros().toPlainString() + " " + balance.product.unitName, fontWeight = FontWeight.Bold)
                        }
                        Text("تكلفة الوحدة: ${balance.product.averageCostAmount} ${balance.product.valuationCurrencyCode} | قيمة المخزون: ${balance.valuationAmount.stripTrailingZeros().toPlainString()} ${balance.product.valuationCurrencyCode}", style = MaterialTheme.typography.bodySmall)
                        Text("حد الطلب: ${balance.product.minimumStock} ${balance.product.unitName} | سعر البيع: ${balance.product.sellingPriceAmount} ${balance.product.priceCurrencyCode}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }

            if (selectedSection == 3) {
            item {
                CommerceSection("تحديث سعر البيع وتكلفة الاستبدال") {
                    CommerceDropdown(
                        "الصنف",
                        pricingProductId,
                        products.map { it.id.toString() to "${it.name} (${it.sku})" },
                        { pricingProductId = it }
                    )
                    val selectedPricingProduct = products.firstOrNull { it.id.toString() == pricingProductId }
                    val priceCurrency = selectedPricingProduct?.priceCurrencyCode ?: "العملة الأساسية"
                    CommerceTextField("سعر البيع للوحدة ($priceCurrency)", pricingSellingPrice, { pricingSellingPrice = it })
                    CommerceTextField("تكلفة الاستبدال للوحدة ($priceCurrency)", pricingReplacementCost, { pricingReplacementCost = it })
                    Text(
                        "التعديل يطبق على السعر الحالي للأصناف والفواتير المستقبلية فقط؛ لا يعيد كتابة الفواتير أو تكاليف المخزون السابقة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    CommerceActionButton("حفظ أسعار الصنف", enabled = selectedPricingProduct != null) {
                        scope.launch {
                            runCatching {
                                repository.updateProductPricing(
                                    productId = pricingProductId.toLongOrNull() ?: error("اختر صنفًا"),
                                    sellingPrice = pricingSellingPrice,
                                    replacementCost = pricingReplacementCost
                                )
                            }.onSuccess { notice = "تم تحديث الأسعار الحالية للصنف" }
                                .onFailure { notice = it.message ?: "تعذر تحديث الأسعار؛ تحقق من القيم" }
                        }
                    }
                }
            }
            }

            if (selectedSection == 3) {
            item {
                CommerceSection("نقل المخزون بين المستودعات") {
                    CommerceDropdown("الصنف", transferProductId, products.map { it.id.toString() to "${it.name} (${it.sku})" }, { transferProductId = it })
                    CommerceDropdown("من المستودع", transferFromWarehouseId, warehouses.filter { it.active }.map { it.id.toString() to it.name }, { transferFromWarehouseId = it })
                    CommerceDropdown("إلى المستودع", transferToWarehouseId, warehouses.filter { it.active && it.id.toString() != transferFromWarehouseId }.map { it.id.toString() to it.name }, { transferToWarehouseId = it })
                    CommerceTextField("الكمية المنقولة", transferQuantity, { transferQuantity = it })
                    Text("يسجل النظام حركتي إخراج وإدخال في معاملة واحدة للحفاظ على إجمالي المخزون.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    CommerceActionButton("ترحيل نقل المخزون", enabled = products.isNotEmpty() && warehouses.count { it.active } > 1) {
                        scope.launch {
                            runCatching {
                                repository.transferStock(
                                    productId = transferProductId.toLongOrNull() ?: error("اختر صنفًا"),
                                    fromWarehouseId = transferFromWarehouseId.toLongOrNull() ?: error("اختر المستودع المصدر"),
                                    toWarehouseId = transferToWarehouseId.toLongOrNull() ?: error("اختر مستودعًا مختلفًا"),
                                    quantityText = transferQuantity
                                )
                            }.onSuccess { transferQuantity = "1"; notice = "تم نقل المخزون بين المستودعين" }
                             .onFailure { notice = it.message ?: "تعذر نقل المخزون" }
                        }
                    }
                }
            }
            }

            if (selectedSection == 3) {
            item {
                CommerceSection("الجرد والتسوية متعددة الأصناف") {
                    Text(
                        "أدخل الكمية الفعلية لكل صنف تم عده في المستودع المحدد. اترك الأصناف غير المعدودة فارغة؛ لن يؤثر الجرد إلا في الأصناف التي أدخلت كمياتها.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (products.isEmpty()) Text("أضف أصنافًا قبل بدء الجرد.")
                    products.forEach { product ->
                        val currentQuantity = balances.firstOrNull { it.product.id == product.id }?.quantity ?: BigDecimal.ZERO
                        Text("${product.name} · رصيد النظام: ${currentQuantity.stripTrailingZeros().toPlainString()} ${product.unitName}", fontWeight = FontWeight.SemiBold)
                        CommerceTextField(
                            "الكمية الفعلية — ${product.sku}",
                            stocktakeDraft[product.id].orEmpty(),
                            { value -> stocktakeDraft[product.id] = value },
                            supportingText = "اترك الحقل فارغًا لتخطي هذا الصنف"
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    CommerceActionButton("حفظ الجرد وتسجيل الفروق", enabled = products.isNotEmpty() && stocktakeDraft.values.any { it.isNotBlank() }) {
                        scope.launch {
                            runCatching {
                                val warehouseId = selectedWarehouse.toLongOrNull() ?: error("اختر مستودعًا")
                                val counted = stocktakeDraft.filterValues { it.isNotBlank() }.mapValues { it.value.trim() }
                                repository.finalizeStocktake(warehouseId, counted, "جرد يدوي")
                            }.onSuccess {
                                stocktakeDraft.clear()
                                notice = "تم حفظ الجرد وتسوية الفروق لكل الأصناف المدخلة في سجل الحركات"
                            }.onFailure { notice = it.message ?: "تعذر حفظ الجرد" }
                        }
                    }
                }
            }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
