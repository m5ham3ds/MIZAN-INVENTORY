package com.sulo.mizan.inventory.features.reports

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
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
import com.sulo.mizan.inventory.core.repository.FinancialSummary
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
fun ReportsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { InventoryRepository(context) }
    val currencyRepository = remember { CurrencyRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var notice by remember { mutableStateOf<String?>(null) }
    val summary by repository.observeFinancialSummary().collectAsState(initial = FinancialSummary(
        baseCurrencyCode = "SDG", productCount = 0, lowStockCount = 0, totalStockQuantity = BigDecimal.ZERO,
        inventoryValueByCurrency = emptyList(), replacementValueByCurrency = emptyList(), replacementCostVarianceByCurrency = emptyList(),
        nonComparableReplacementProductCount = 0, netRevenueBase = BigDecimal.ZERO,
        costOfGoodsSoldBase = BigDecimal.ZERO, grossProfitBase = BigDecimal.ZERO, expensesBase = BigDecimal.ZERO,
        netProfitBase = BigDecimal.ZERO, receivableByCurrency = emptyList(), payableByCurrency = emptyList(),
        unpaidExpensesByCurrency = emptyList(), customerRefundDueByCurrency = emptyList(), supplierRefundDueByCurrency = emptyList(),
        cashBalances = emptyList(), shipmentCount = 0, purchaseCount = 0, salesCount = 0
    ))
    val currencies by currencyRepository.activeCurrenciesFlow.collectAsState(initial = Currency.DEFAULT_ACTIVE_CURRENCIES)
    val baseCurrency by currencyRepository.baseCurrencyFlow.collectAsState(initial = Currency.DEFAULT_BASE_CURRENCY)
    val accounts by repository.cashAccountsFlow.collectAsState(initial = emptyList())
    val expenses by repository.expensesFlow.collectAsState(initial = emptyList())
    val sales by repository.salesInvoicesFlow.collectAsState(initial = emptyList())
    val purchases by repository.purchaseInvoicesFlow.collectAsState(initial = emptyList())
    val salesLines by repository.salesInvoiceLinesFlow.collectAsState(initial = emptyList())
    val purchaseLines by repository.purchaseInvoiceLinesFlow.collectAsState(initial = emptyList())
    val salesReturns by repository.salesReturnsFlow.collectAsState(initial = emptyList())
    val purchaseReturns by repository.purchaseReturnsFlow.collectAsState(initial = emptyList())

    var expenseCategory by remember { mutableStateOf("تشغيل") }
    var expenseDescription by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("0") }
    var expenseCurrency by remember { mutableStateOf("SDG") }
    var expenseFx by remember { mutableStateOf("1") }
    var expensePaid by remember { mutableStateOf(false) }
    var expenseAccountId by remember { mutableStateOf("") }

    var accountName by remember { mutableStateOf("") }
    var accountCurrency by remember { mutableStateOf("SDG") }
    var accountOpening by remember { mutableStateOf("0") }

    var cashAccountId by remember { mutableStateOf("") }
    var cashDirection by remember { mutableStateOf("CREDIT") }
    var cashAmount by remember { mutableStateOf("") }
    var cashNote by remember { mutableStateOf("") }

    var isSaleDebt by remember { mutableStateOf(true) }
    var invoiceId by remember { mutableStateOf("") }
    var paymentAccountId by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("") }
    val customerRefundAmounts = remember { mutableStateMapOf<Long, String>() }
    val customerRefundAccounts = remember { mutableStateMapOf<Long, String>() }
    val supplierRefundAmounts = remember { mutableStateMapOf<Long, String>() }
    val supplierRefundAccounts = remember { mutableStateMapOf<Long, String>() }

    var selectedSection by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { repository.ensureInitialized() }
    LaunchedEffect(currencies, baseCurrency) {
        if (currencies.none { it.code == expenseCurrency }) { expenseCurrency = baseCurrency.code; expenseFx = "1" }
        if (currencies.none { it.code == accountCurrency }) accountCurrency = baseCurrency.code
    }
    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty()) {
            if (accounts.none { it.id.toString() == cashAccountId }) cashAccountId = accounts.first().id.toString()
            if (accounts.none { it.id.toString() == expenseAccountId }) expenseAccountId = accounts.first().id.toString()
            if (accounts.none { it.id.toString() == paymentAccountId }) paymentAccountId = accounts.first().id.toString()
        }
    }
    LaunchedEffect(sales, purchases, isSaleDebt) {
        val selectedInvoiceIds = if (isSaleDebt) sales.map { it.id.toString() } else purchases.map { it.id.toString() }
        if (invoiceId !in selectedInvoiceIds) invoiceId = selectedInvoiceIds.firstOrNull().orEmpty()
    }
    LaunchedEffect(notice) { notice?.let { snackbar.showSnackbar(it); notice = null } }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("reports_screen")) {
        SnackbarHost(snackbar, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MizanPageHeader(
                title = "التقارير والتحليلات",
                subtitle = "صورة موحدة للأرباح والمخزون والديون والصندوق وحركة النشاط.",
                eyebrow = "المؤشرات المالية",
                icon = Icons.Default.Assessment
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MizanMiniStat(label = "صافي الربح", value = summary.netProfitBase.stripTrailingZeros().toPlainString() + " " + summary.baseCurrencyCode, modifier = Modifier.weight(1f))
                    MizanMiniStat(label = "مخزون منخفض", value = summary.lowStockCount.toString(), modifier = Modifier.weight(1f), warning = summary.lowStockCount > 0)
                    MizanMiniStat(label = "الذمم", value = (summary.receivableByCurrency.size + summary.payableByCurrency.size).toString(), modifier = Modifier.weight(1f))
            }
            MizanSectionTabs(titles = listOf("الملخص", "الذمم والمرتجعات", "الصناديق", "المصروفات"), selectedIndex = selectedSection, onSelect = { selectedSection = it })
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedSection == 0) {
            item {
                CommerceSection("ملخص مالي (${summary.baseCurrencyCode})") {
                    Metric("عدد الأصناف النشطة", summary.productCount.toString())
                    Metric("أصناف عند حد إعادة الطلب أو تحته", summary.lowStockCount.toString())
                    Metric("كمية المخزون الكلية", summary.totalStockQuantity.stripTrailingZeros().toPlainString())
                    Metric("صافي المبيعات قبل COGS", amount(summary.netRevenueBase, summary.baseCurrencyCode))
                    Metric("تكلفة البضاعة المباعة COGS", amount(summary.costOfGoodsSoldBase, summary.baseCurrencyCode))
                    Metric("مجمل الربح", amount(summary.grossProfitBase, summary.baseCurrencyCode))
                    Metric("المصروفات المسجلة", amount(summary.expensesBase, summary.baseCurrencyCode))
                    Metric("صافي الربح التشغيلي التقديري", amount(summary.netProfitBase, summary.baseCurrencyCode))
                    Text("صافي الربح هنا = صافي الإيراد − تكلفة المبيعات − المصروفات المسجلة؛ لا يشمل ضريبة القيمة المضافة كإيراد ولا تقييم الأصول غير المخزنية.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            }

            if (selectedSection == 0) {
            item {
                CommerceSection("تقييم المخزون وتكلفة الاستبدال") {
                    Text("القيمة الدفترية وفق متوسط التكلفة المرجّح:", fontWeight = FontWeight.SemiBold)
                    summary.inventoryValueByCurrency.forEach { Metric(it.currencyCode, it.amount.stripTrailingZeros().toPlainString()) }
                    Spacer(Modifier.height(6.dp))
                    Text("قيمة الاستبدال التقديرية وفق تكلفة الاستبدال المدخلة للصنف:", fontWeight = FontWeight.SemiBold)
                    summary.replacementValueByCurrency.forEach { Metric(it.currencyCode, it.amount.stripTrailingZeros().toPlainString()) }
                    Spacer(Modifier.height(6.dp))
                    Text("فرق الاستبدال عن القيمة الدفترية (موجب = تكلفة إحلال أعلى):", fontWeight = FontWeight.SemiBold)
                    if (summary.replacementCostVarianceByCurrency.isEmpty()) {
                        Text("لا توجد أصناف بعملتي تقييم متطابقتين للمقارنة المباشرة.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        summary.replacementCostVarianceByCurrency.forEach { Metric(it.currencyCode, it.amount.stripTrailingZeros().toPlainString()) }
                    }
                    if (summary.nonComparableReplacementProductCount > 0) {
                        Text("تم استبعاد ${summary.nonComparableReplacementProductCount} صنفًا من مقارنة الفرق لأن عملة التكلفة الدفترية تختلف عن عملة تكلفة الاستبدال.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            }

            if (selectedSection == 1) {
            item {
                CommerceSection("الذمم المدينة — العملاء") {
                    if (summary.receivableByCurrency.isEmpty()) Text("لا توجد ذمم مدينة مفتوحة.")
                    summary.receivableByCurrency.forEach { Metric(it.currencyCode, it.amount.stripTrailingZeros().toPlainString()) }
                }
            }
            }

            if (selectedSection == 1) {
            item {
                CommerceSection("تفاصيل الذمم المدينة المفتوحة") {
                    val openItems = sales.mapNotNull { invoice ->
                        val lineIds = salesLines.filter { it.invoiceId == invoice.id }.map { it.id }.toSet()
                        val relatedReturns = salesReturns.filter { it.invoiceLineId in lineIds }
                        val returned = relatedReturns.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount.toBigDecimal() }
                        val refunded = relatedReturns.fold(BigDecimal.ZERO) { sum, row -> sum + row.refundedAmount.toBigDecimal() }
                        val outstanding = (invoice.totalAmount.toBigDecimal() - invoice.paidAmount.toBigDecimal() - returned + refunded).max(BigDecimal.ZERO)
                        if (outstanding > BigDecimal.ZERO) Triple(invoice, returned, outstanding) else null
                    }
                    if (openItems.isEmpty()) Text("لا توجد فواتير عملاء بمبالغ مستحقة.")
                    openItems.take(30).forEach { (invoice, returned, outstanding) ->
                        Metric("${invoice.customerName} · ${invoice.invoiceNumber} (${invoice.currencyCode})", outstanding.stripTrailingZeros().toPlainString())
                    }
                }
            }
            }

            if (selectedSection == 1) {
            item {
                CommerceSection("الذمم الدائنة — الموردون") {
                    if (summary.payableByCurrency.isEmpty()) Text("لا توجد ذمم دائنة مفتوحة.")
                    summary.payableByCurrency.forEach { Metric(it.currencyCode, it.amount.stripTrailingZeros().toPlainString()) }
                    Spacer(Modifier.height(6.dp))
                    Text("مصروفات مؤجلة غير مسددة:", fontWeight = FontWeight.SemiBold)
                    if (summary.unpaidExpensesByCurrency.isEmpty()) Text("لا توجد مصروفات مؤجلة غير مسددة.", style = MaterialTheme.typography.bodySmall)
                    summary.unpaidExpensesByCurrency.forEach { Metric(it.currencyCode, it.amount.stripTrailingZeros().toPlainString()) }
                }
            }
            }

            if (selectedSection == 1) {
            item {
                CommerceSection("تفاصيل الذمم الدائنة المفتوحة") {
                    val openItems = purchases.mapNotNull { invoice ->
                        val lineIds = purchaseLines.filter { it.invoiceId == invoice.id }.map { it.id }.toSet()
                        val relatedReturns = purchaseReturns.filter { it.invoiceLineId in lineIds }
                        val returned = relatedReturns.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount.toBigDecimal() }
                        val refunded = relatedReturns.fold(BigDecimal.ZERO) { sum, row -> sum + row.refundReceivedAmount.toBigDecimal() }
                        val outstanding = (invoice.totalAmount.toBigDecimal() - invoice.paidAmount.toBigDecimal() - returned + refunded).max(BigDecimal.ZERO)
                        if (outstanding > BigDecimal.ZERO) Triple(invoice, returned, outstanding) else null
                    }
                    if (openItems.isEmpty()) Text("لا توجد فواتير موردين بمبالغ مستحقة.")
                    openItems.take(30).forEach { (invoice, returned, outstanding) ->
                        Metric("${invoice.supplierName} · ${invoice.invoiceNumber} (${invoice.currencyCode})", outstanding.stripTrailingZeros().toPlainString())
                    }
                }
            }
            }

            if (selectedSection == 1) {
            item {
                CommerceSection("استردادات مستحقة للعملاء") {
                    if (summary.customerRefundDueByCurrency.isEmpty()) Text("لا توجد استردادات مستحقة للعملاء.")
                    summary.customerRefundDueByCurrency.forEach { Metric(it.currencyCode, it.amount.stripTrailingZeros().toPlainString()) }
                    salesReturns.forEach { returnRow ->
                        val line = salesLines.firstOrNull { it.id == returnRow.invoiceLineId } ?: return@forEach
                        val invoice = sales.firstOrNull { it.id == line.invoiceId } ?: return@forEach
                        val invoiceLineIds = salesLines.filter { it.invoiceId == invoice.id }.map { it.id }.toSet()
                        val related = salesReturns.filter { it.invoiceLineId in invoiceLineIds }
                        val totalCredits = related.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount.toBigDecimal() }
                        val alreadyRefunded = related.fold(BigDecimal.ZERO) { sum, row -> sum + row.refundedAmount.toBigDecimal() }
                        val invoiceDue = (invoice.paidAmount.toBigDecimal() + totalCredits - invoice.totalAmount.toBigDecimal() - alreadyRefunded).max(BigDecimal.ZERO)
                        val rowDue = (returnRow.amount.toBigDecimal() - returnRow.refundedAmount.toBigDecimal()).max(BigDecimal.ZERO)
                        val refundable = minOf(invoiceDue, rowDue)
                        if (refundable > BigDecimal.ZERO) {
                            Text("${invoice.customerName} · مرتجع #${returnRow.id} · المتاح للاسترداد ${refundable.stripTrailingZeros().toPlainString()} ${invoice.currencyCode}", style = MaterialTheme.typography.bodySmall)
                            val matchingAccounts = accounts.filter { it.currencyCode == invoice.currencyCode }
                            val selectedAccount = customerRefundAccounts[returnRow.id]?.takeIf { id -> matchingAccounts.any { it.id.toString() == id } }
                                ?: matchingAccounts.firstOrNull()?.id?.toString().orEmpty()
                            CommerceDropdown("صندوق الاسترداد (${invoice.currencyCode})", selectedAccount, matchingAccounts.map { it.id.toString() to it.name }, { customerRefundAccounts[returnRow.id] = it })
                            CommerceTextField("مبلغ الاسترداد للعميل", customerRefundAmounts[returnRow.id] ?: refundable.toPlainString(), { customerRefundAmounts[returnRow.id] = it })
                            CommerceActionButton("صرف استرداد العميل", enabled = matchingAccounts.isNotEmpty()) {
                                scope.launch {
                                    runCatching { repository.refundCustomer(returnRow.id, customerRefundAmounts[returnRow.id] ?: refundable.toPlainString(), selectedAccount.toLongOrNull() ?: error("أنشئ صندوقًا مطابق العملة")) }
                                        .onSuccess { customerRefundAmounts.remove(returnRow.id); notice = "تم صرف استرداد العميل وتسجيل حركة الصندوق" }
                                        .onFailure { notice = it.message ?: "تعذر صرف استرداد العميل" }
                                }
                            }
                        }
                    }
                }
            }
            }

            if (selectedSection == 1) {
            item {
                CommerceSection("استردادات مستحقة من الموردين") {
                    if (summary.supplierRefundDueByCurrency.isEmpty()) Text("لا توجد استردادات مستحقة من الموردين.")
                    summary.supplierRefundDueByCurrency.forEach { Metric(it.currencyCode, it.amount.stripTrailingZeros().toPlainString()) }
                    purchaseReturns.forEach { returnRow ->
                        val line = purchaseLines.firstOrNull { it.id == returnRow.invoiceLineId } ?: return@forEach
                        val invoice = purchases.firstOrNull { it.id == line.invoiceId } ?: return@forEach
                        val invoiceLineIds = purchaseLines.filter { it.invoiceId == invoice.id }.map { it.id }.toSet()
                        val related = purchaseReturns.filter { it.invoiceLineId in invoiceLineIds }
                        val totalCredits = related.fold(BigDecimal.ZERO) { sum, row -> sum + row.amount.toBigDecimal() }
                        val alreadyReceived = related.fold(BigDecimal.ZERO) { sum, row -> sum + row.refundReceivedAmount.toBigDecimal() }
                        val invoiceDue = (invoice.paidAmount.toBigDecimal() + totalCredits - invoice.totalAmount.toBigDecimal() - alreadyReceived).max(BigDecimal.ZERO)
                        val rowDue = (returnRow.amount.toBigDecimal() - returnRow.refundReceivedAmount.toBigDecimal()).max(BigDecimal.ZERO)
                        val receivable = minOf(invoiceDue, rowDue)
                        if (receivable > BigDecimal.ZERO) {
                            Text("${invoice.supplierName} · مرتجع #${returnRow.id} · المستحق للمتجر ${receivable.stripTrailingZeros().toPlainString()} ${invoice.currencyCode}", style = MaterialTheme.typography.bodySmall)
                            val matchingAccounts = accounts.filter { it.currencyCode == invoice.currencyCode }
                            val selectedAccount = supplierRefundAccounts[returnRow.id]?.takeIf { id -> matchingAccounts.any { it.id.toString() == id } }
                                ?: matchingAccounts.firstOrNull()?.id?.toString().orEmpty()
                            CommerceDropdown("صندوق الاستلام (${invoice.currencyCode})", selectedAccount, matchingAccounts.map { it.id.toString() to it.name }, { supplierRefundAccounts[returnRow.id] = it })
                            CommerceTextField("المبلغ المستلم من المورد", supplierRefundAmounts[returnRow.id] ?: receivable.toPlainString(), { supplierRefundAmounts[returnRow.id] = it })
                            CommerceActionButton("تسجيل استرداد المورد", enabled = matchingAccounts.isNotEmpty()) {
                                scope.launch {
                                    runCatching { repository.receiveSupplierRefund(returnRow.id, supplierRefundAmounts[returnRow.id] ?: receivable.toPlainString(), selectedAccount.toLongOrNull() ?: error("أنشئ صندوقًا مطابق العملة")) }
                                        .onSuccess { supplierRefundAmounts.remove(returnRow.id); notice = "تم تسجيل استرداد المورد في الصندوق" }
                                        .onFailure { notice = it.message ?: "تعذر تسجيل استرداد المورد" }
                                }
                            }
                        }
                    }
                }
            }
            }

            if (selectedSection == 2) {
            item {
                CommerceSection("أرصدة الصناديق والحسابات النقدية") {
                    summary.cashBalances.forEach { balance ->
                        Metric("${balance.account.name} (${balance.account.currencyCode})", balance.amount.stripTrailingZeros().toPlainString())
                    }
                    CommerceTextField("اسم صندوق جديد", accountName, { accountName = it })
                    CommerceDropdown("عملة الصندوق", accountCurrency, currencies.map { it.code to "${it.code} — ${it.displayNameAr}" }, { accountCurrency = it })
                    CommerceTextField("الرصيد الافتتاحي", accountOpening, { accountOpening = it }, supportingText = "أدخل النقد الموجود فعليًا عند بدء استخدام النظام")
                    CommerceActionButton("إضافة صندوق برصيد افتتاحي") {
                        scope.launch {
                            runCatching { repository.createCashAccount(accountName, accountCurrency, accountOpening) }
                                .onSuccess { accountName = ""; accountOpening = "0"; notice = "تم إنشاء الصندوق" }
                                .onFailure { notice = it.message ?: "تعذر إنشاء الصندوق؛ الاسم والعملة يجب أن يكونا فريدين" }
                        }
                    }
                }
            }
            }

            if (selectedSection == 2) {
            item {
                CommerceSection("حركة نقدية يدوية") {
                    CommerceDropdown("الصندوق", cashAccountId, accounts.map { it.id.toString() to "${it.name} (${it.currencyCode})" }, { cashAccountId = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = cashDirection == "CREDIT", onClick = { cashDirection = "CREDIT" }, label = { Text("قبض / إيداع") })
                        FilterChip(selected = cashDirection == "DEBIT", onClick = { cashDirection = "DEBIT" }, label = { Text("صرف / سحب") })
                    }
                    CommerceTextField("المبلغ", cashAmount, { cashAmount = it })
                    CommerceTextField("البيان", cashNote, { cashNote = it })
                    CommerceActionButton("تسجيل الحركة النقدية") {
                        scope.launch {
                            runCatching { repository.recordCashTransaction(cashAccountId.toLongOrNull() ?: error("اختر صندوقًا"), cashDirection, cashAmount, cashNote) }
                                .onSuccess { cashAmount = ""; cashNote = ""; notice = "تم تسجيل الحركة النقدية" }
                                .onFailure { notice = it.message ?: "تعذر تسجيل الحركة" }
                        }
                    }
                }
            }
            }

            if (selectedSection == 1) {
            item {
                CommerceSection("تسوية فاتورة / تحصيل دين") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isSaleDebt, onClick = { isSaleDebt = true; invoiceId = sales.firstOrNull()?.id?.toString().orEmpty() }, label = { Text("عميل — قبض") })
                        FilterChip(selected = !isSaleDebt, onClick = { isSaleDebt = false; invoiceId = purchases.firstOrNull()?.id?.toString().orEmpty() }, label = { Text("مورد — صرف") })
                    }
                    val debtInvoiceOptions = if (isSaleDebt) {
                        sales.map { invoice ->
                            invoice.id.toString() to "${invoice.invoiceNumber} · ${invoice.customerName} · ${invoice.totalAmount} ${invoice.currencyCode}"
                        }
                    } else {
                        purchases.map { invoice ->
                            invoice.id.toString() to "${invoice.invoiceNumber} · ${invoice.supplierName} · ${invoice.totalAmount} ${invoice.currencyCode}"
                        }
                    }
                    CommerceDropdown("الفاتورة", invoiceId, debtInvoiceOptions, { invoiceId = it })
                    CommerceDropdown("الصندوق المطابق للعملة", paymentAccountId, accounts.map { it.id.toString() to "${it.name} (${it.currencyCode})" }, { paymentAccountId = it })
                    CommerceTextField("قيمة السداد الجزئي", paymentAmount, { paymentAmount = it })
                    Text("سيفرض النظام تطابق عملة الصندوق والفاتورة والتحقق من الرصيد قبل حفظ التسوية.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CommerceActionButton("ترحيل السداد") {
                        scope.launch {
                            runCatching {
                                repository.recordInvoicePayment(isSaleDebt, invoiceId.toLongOrNull() ?: error("اختر فاتورة"), paymentAmount, paymentAccountId.toLongOrNull() ?: error("اختر صندوقًا"))
                            }.onSuccess { paymentAmount = ""; notice = "تم تسجيل السداد وتحديث رصيد الفاتورة والصندوق" }
                                .onFailure { notice = it.message ?: "تعذر تسجيل السداد" }
                        }
                    }
                }
            }
            }

            if (selectedSection == 3) {
            item {
                CommerceSection("إضافة مصروف") {
                    CommerceTextField("فئة المصروف", expenseCategory, { expenseCategory = it })
                    CommerceTextField("الوصف", expenseDescription, { expenseDescription = it })
                    CommerceDropdown("العملة", expenseCurrency, currencies.map { it.code to "${it.code} — ${it.displayNameAr}" }, {
                        expenseCurrency = it; expenseFx = if (it == baseCurrency.code) "1" else ""
                    })
                    CommerceTextField("سعر التحويل إلى ${baseCurrency.code}", expenseFx, { expenseFx = it })
                    CommerceTextField("المبلغ", expenseAmount, { expenseAmount = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !expensePaid, onClick = { expensePaid = false }, label = { Text("آجل / غير مسدد") })
                        FilterChip(selected = expensePaid, onClick = { expensePaid = true }, label = { Text("مسدد من الصندوق") })
                    }
                    if (expensePaid) CommerceDropdown("الصندوق", expenseAccountId, accounts.map { it.id.toString() to "${it.name} (${it.currencyCode})" }, { expenseAccountId = it })
                    CommerceActionButton("حفظ المصروف") {
                        scope.launch {
                            runCatching {
                                repository.createExpense(
                                    expenseCategory, expenseDescription, expenseAmount, expenseCurrency, expenseFx,
                                    expensePaid, if (expensePaid) expenseAccountId.toLongOrNull() else null
                                )
                            }.onSuccess { expenseDescription = ""; expenseAmount = "0"; notice = "تم حفظ المصروف" }
                                .onFailure { notice = it.message ?: "تعذر حفظ المصروف" }
                        }
                    }
                    Text("المصروفات المؤجلة تُسجّل ضمن المصروفات وتُسدد لاحقًا من خلال الصندوق؛ لا يُسمح برصيد نقدي سالب.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            }

            if (selectedSection == 3) {
            item { Text("سجل المصروفات (${expenses.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            }

            if (selectedSection == 3) {
            items(expenses.take(25), key = { "e-${it.id}" }) { expense ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${expense.description} · ${expense.category}", fontWeight = FontWeight.Bold)
                        Text("${expense.amount} ${expense.currencyCode} (${amount(expense.amountBase.toBigDecimal(), baseCurrency.code)}) · ${if (expense.paid) "مسدد" else "غير مسدد"}", style = MaterialTheme.typography.bodySmall)
                        if (!expense.paid && accounts.any { it.currencyCode == expense.currencyCode }) {
                            val account = accounts.first { it.currencyCode == expense.currencyCode }
                            CommerceActionButton("سداد المصروف من ${account.name}") {
                                scope.launch {
                                    runCatching { repository.payExpense(expense.id, account.id) }
                                        .onSuccess { notice = "تم سداد المصروف" }
                                        .onFailure { notice = it.message ?: "تعذر سداد المصروف" }
                                }
                            }
                        }
                    }
                }
            }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun amount(value: BigDecimal, currencyCode: String): String = "${value.setScale(2, java.math.RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString()} $currencyCode"
