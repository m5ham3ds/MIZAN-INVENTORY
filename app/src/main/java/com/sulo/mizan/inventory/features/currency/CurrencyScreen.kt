package com.sulo.mizan.inventory.features.currency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sulo.mizan.inventory.core.model.ConversionResult
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.model.ExchangeRate
import com.sulo.mizan.inventory.core.model.Money
import com.sulo.mizan.inventory.core.model.RateType
import com.sulo.mizan.inventory.core.repository.AutomaticExchangeRateUpdater
import com.sulo.mizan.inventory.core.repository.CurrencyRepository
import com.sulo.mizan.inventory.core.repository.MissingExchangeRateException
import com.sulo.mizan.inventory.core.ui.MizanPageHeader
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrencyScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { CurrencyRepository(context) }
    val rateUpdater = remember { AutomaticExchangeRateUpdater(context) }
    val scope = rememberCoroutineScope()
    var isUpdatingRates by remember { mutableStateOf(false) }
    var rateUpdateMessage by remember { mutableStateOf("سيتم تحديث الأسعار المرجعية اليومية فقط؛ لن تتغير أسعار الشراء والبيع اليدوية.") }

    LaunchedEffect(Unit) {
        repository.ensureInitialized()
    }

    val activeCurrencies by repository.activeCurrenciesFlow.collectAsState(initial = Currency.DEFAULT_ACTIVE_CURRENCIES)
    val baseCurrency by repository.baseCurrencyFlow.collectAsState(initial = Currency.DEFAULT_BASE_CURRENCY)
    val historyRates by repository.exchangeRateHistoryFlow.collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("العملات", "أسعار الصرف", "حاسبة التحويل")

    // Swap currency dialog state
    var currencyToSwap by remember { mutableStateOf<Currency?>(null) }
    // Add rate dialog state
    var showAddRateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MizanPageHeader(
            title = "العملات وأسعار الصرف",
            subtitle = "العملة الأساسية: ${baseCurrency.displayNameAr} (${baseCurrency.code}) · العملات النشطة ${activeCurrencies.size}/3",
            eyebrow = "الإعدادات المالية",
            icon = Icons.Default.CurrencyExchange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        if (selectedTabIndex == 1) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("التحديث الآلي للسعر المرجعي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("المصدر: fawazahmed0 Exchange API (تحديث يومي مجاني). هذه أسعار مرجعية إرشادية وليست بالضرورة سعر السوق السوداني؛ لا تُستخدم بدل سعر الشراء أو البيع الفعلي.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = !isUpdatingRates,
                    onClick = {
                        scope.launch {
                            isUpdatingRates = true
                            rateUpdateMessage = "جارٍ جلب آخر الأسعار المرجعية…"
                            runCatching { withContext(Dispatchers.IO) { rateUpdater.updateNow(force = true) } }
                                .onSuccess { count -> rateUpdateMessage = if (count == 0) "الأسعار المرجعية محفوظة بالفعل لهذا اليوم أو لم يطرأ تحديث جديد." else "تم حفظ $count سعرًا مرجعيًا جديدًا. أسعار الشراء والبيع اليدوية لم تتغير." }
                                .onFailure { error -> rateUpdateMessage = "تعذر تحديث الأسعار المرجعية: ${error.message ?: "تحقق من الاتصال"}. لم تُستبدل أسعار الشراء والبيع." }
                            isUpdatingRates = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isUpdatingRates) "جارٍ التحديث…" else "تحديث الأسعار المرجعية الآن") }
                Text(rateUpdateMessage, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> ActiveCurrenciesTab(
                    activeCurrencies = activeCurrencies,
                    baseCurrency = baseCurrency,
                    onSetBase = { code ->
                        scope.launch { repository.setBaseCurrency(code) }
                    },
                    onSwapCurrency = { currency ->
                        currencyToSwap = currency
                    }
                )

                1 -> RatesAndHistoryTab(
                    activeCurrencies = activeCurrencies,
                    historyRates = historyRates,
                    onOpenAddRateDialog = { showAddRateDialog = true }
                )

                2 -> ConversionCalculatorTab(
                    activeCurrencies = activeCurrencies,
                    baseCurrency = baseCurrency,
                    repository = repository,
                    onNavigateToAddRate = {
                        selectedTabIndex = 1
                        showAddRateDialog = true
                    }
                )
            }
        }
    }

    // Dialog: Swap Currency
    currencyToSwap?.let { targetToReplace ->
        SwapCurrencyDialog(
            currentCurrency = targetToReplace,
            activeCurrencies = activeCurrencies,
            onDismiss = { currencyToSwap = null },
            onSelectNewCurrency = { newCurrency ->
                scope.launch {
                    val updatedList = activeCurrencies.map {
                        if (it.code == targetToReplace.code) newCurrency.code else it.code
                    }
                    val newBase = if (baseCurrency.code == targetToReplace.code) newCurrency.code else baseCurrency.code
                    repository.setActiveCurrencies(updatedList, newBase)
                    currencyToSwap = null
                }
            }
        )
    }

    // Dialog: Add Manual Rate
    if (showAddRateDialog) {
        AddExchangeRateDialog(
            activeCurrencies = activeCurrencies,
            onDismiss = { showAddRateDialog = false },
            onSaveRate = { fromCurrency, toCurrency, rate, rateType, note ->
                scope.launch {
                    repository.addExchangeRate(
                        fromCurrency = fromCurrency,
                        toCurrency = toCurrency,
                        rate = rate,
                        rateType = rateType,
                        sourceOrNote = note
                    )
                    showAddRateDialog = false
                }
            }
        )
    }
}

@Composable
private fun ActiveCurrenciesTab(
    activeCurrencies: List<Currency>,
    baseCurrency: Currency,
    onSetBase: (String) -> Unit,
    onSwapCurrency: (Currency) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tab_active_currencies")
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "يدعم النظام 3 عملات نشطة كحد أقصى. يمكنك تغيير أي عملة وتعيين العملة الأساسية المستخدمة في التقارير والحسابات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "العملات النشطة الحالية (${activeCurrencies.size}/3)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        activeCurrencies.forEachIndexed { index, currency ->
            val isBase = currency.code == baseCurrency.code
            ActiveCurrencyCard(
                currency = currency,
                orderIndex = index + 1,
                isBase = isBase,
                onSetAsBase = { onSetBase(currency.code) },
                onChangeCurrency = { onSwapCurrency(currency) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "قواعد حماية السجلات التاريخية",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• استبدال أو تعطيل أي عملة لا يحذف أسعار صرفها المسجلة سابقاً من قاعدة البيانات.\n" +
                            "• يتم الاحتفاظ بالسجلات بالدقة العشرية المعتمدة لكل عملة لضمان صحة التدقيق المالي المستقبلي.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActiveCurrencyCard(
    currency: Currency,
    orderIndex: Int,
    isBase: Boolean,
    onSetAsBase: () -> Unit,
    onChangeCurrency: () -> Unit
) {
    val borderColor = if (isBase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val borderWidth = if (isBase) 2.dp else 1.dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("currency_card_${currency.code}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBase) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currency.symbol,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isBase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currency.displayNameAr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${currency.code})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (currency.displayNameEn.isNotBlank()) {
                            Text(
                                text = currency.displayNameEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isBase) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "العملة الأساسية",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "عملة نشطة #$orderIndex",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "رمز ISO: ${currency.code} | المنازل العشرية: ${currency.defaultDecimals}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    if (!isBase) {
                        OutlinedButton(
                            onClick = onSetAsBase,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(text = "تعيين كأساسية", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Button(
                        onClick = onChangeCurrency,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "تغيير", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RatesAndHistoryTab(
    activeCurrencies: List<Currency>,
    historyRates: List<ExchangeRate>,
    onOpenAddRateDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("tab_rates_history")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "أسعار الصرف اليدوية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "سجل التحديثات اليدوية بدون اتصال بالإنترنت",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onOpenAddRateDialog,
                modifier = Modifier.testTag("btn_add_rate")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "إضافة سعر")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (historyRates.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد أسعار صرف مسجلة حتى الآن",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اضغط على زر (إضافة سعر) لإدخال سعر الصرف اليدوي لعملياتك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "سجل أسعار الصرف التاريخية (${historyRates.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(historyRates) { rate ->
                    ExchangeRateHistoryCard(rate = rate)
                }
            }
        }
    }
}

@Composable
private fun ExchangeRateHistoryCard(rate: ExchangeRate) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(rate.timestamp) { dateFormat.format(Date(rate.timestamp)) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rate.fromCurrency.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = rate.toCurrency.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (rate.rateType) {
                        RateType.REFERENCE -> MaterialTheme.colorScheme.secondaryContainer
                        RateType.BUY -> Color(0xFFDCFCE7) // Soft emerald
                        RateType.SELL -> Color(0xFFFEF3C7) // Soft amber
                        RateType.TRANSACTIONAL -> Color(0xFFE0E7FF) // Soft indigo
                    }
                ) {
                    Text(
                        text = rate.rateType.displayNameAr,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (rate.rateType) {
                            RateType.REFERENCE -> MaterialTheme.colorScheme.onSecondaryContainer
                            RateType.BUY -> Color(0xFF166534)
                            RateType.SELL -> Color(0xFF92400E)
                            RateType.TRANSACTIONAL -> Color(0xFF3730A3)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1 ${rate.fromCurrency.code} = ${rate.rate.stripTrailingZeros().toPlainString()} ${rate.toCurrency.code}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (rate.sourceOrNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "المصدر / ملاحظة: ${rate.sourceOrNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConversionCalculatorTab(
    activeCurrencies: List<Currency>,
    baseCurrency: Currency,
    repository: CurrencyRepository,
    onNavigateToAddRate: () -> Unit
) {
    var amountInput by remember { mutableStateOf("100") }
    var fromCurrency by remember(activeCurrencies) {
        mutableStateOf(activeCurrencies.find { it.code == "USD" } ?: activeCurrencies.firstOrNull() ?: Currency.USD)
    }
    var toCurrency by remember(activeCurrencies) {
        mutableStateOf(activeCurrencies.find { it.code == "SDG" } ?: activeCurrencies.getOrNull(1) ?: Currency.SDG)
    }
    var selectedRateType by remember { mutableStateOf(RateType.REFERENCE) }

    var conversionResult by remember { mutableStateOf<ConversionResult?>(null) }
    var conversionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun performCalculation() {
        conversionError = null
        conversionResult = null
        val amount = try {
            val trimmed = amountInput.trim()
            if (trimmed.isEmpty()) BigDecimal.ZERO else BigDecimal(trimmed)
        } catch (_: Exception) {
            conversionError = "يرجى إدخال مبلغ رقمي صحيح"
            return
        }

        if (amount < BigDecimal.ZERO) {
            conversionError = "لا يمكن تحويل مبلغ سالب"
            return
        }

        scope.launch {
            try {
                val result = repository.convertCurrency(
                    money = Money(amount, fromCurrency),
                    toCurrency = toCurrency,
                    preferredRateType = selectedRateType
                )
                conversionResult = result
            } catch (e: MissingExchangeRateException) {
                conversionError = e.message
            } catch (e: Exception) {
                conversionError = "خطأ في التحويل: ${e.message}"
            }
        }
    }

    LaunchedEffect(fromCurrency, toCurrency, selectedRateType) {
        performCalculation()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tab_calculator")
    ) {
        Text(
            text = "حاسبة التحويل المالي الدقيق",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "حساب فوري يعتمد على الأسعار اليدوية المسجلة وسياسة التحويل المباشر أو التقاطعي",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Amount Field
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        performCalculation()
                    },
                    label = { Text("المبلغ المراد تحويله") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = { Text(fromCurrency.symbol, modifier = Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Currencies Selectors Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "من عملة:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        CurrencySelectDropdown(
                            currencies = activeCurrencies,
                            selected = fromCurrency,
                            onSelect = { fromCurrency = it }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 18.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                val temp = fromCurrency
                                fromCurrency = toCurrency
                                toCurrency = temp
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "تبديل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "إلى عملة:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        CurrencySelectDropdown(
                            currencies = activeCurrencies,
                            selected = toCurrency,
                            onSelect = { toCurrency = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rate Type selector
                Text(text = "نوع سعر الصرف:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(RateType.REFERENCE, RateType.BUY, RateType.SELL).forEach { rType ->
                        FilterChip(
                            selected = selectedRateType == rType,
                            onClick = { selectedRateType = rType },
                            label = { Text(rType.displayNameAr, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { performCalculation() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Calculate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعادة حساب التحويل")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result or Error Card
        conversionResult?.let { res ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conversion_result_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "المبلغ المحوّل الناتج:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = res.targetMoney.formatted(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "تفاصيل المسار المالي:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = res.calculationDetailsAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (res.isCrossRate) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ملاحظة: تم تطبيق تحويل تقاطعي دقيق لحساب السعر عبر عملة وسيطة لعدم وجود سعر صرف مباشر مسجل.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        conversionError?.let { err ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conversion_error_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تعذر إجراء التحويل",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateToAddRate,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة سعر صرف لهذا المسار الآن")
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySelectDropdown(
    currencies: List<Currency>,
    selected: Currency,
    onSelect: (Currency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selected.code} (${selected.symbol})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("اختر العملة", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    currencies.forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(currency)
                                    expanded = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currency.code == selected.code,
                                onClick = {
                                    onSelect(currency)
                                    expanded = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currency.displayNameAr} (${currency.code}) - ${currency.symbol}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun SwapCurrencyDialog(
    currentCurrency: Currency,
    activeCurrencies: List<Currency>,
    onDismiss: () -> Unit,
    onSelectNewCurrency: (Currency) -> Unit
) {
    val activeCodes = remember(activeCurrencies) { activeCurrencies.map { it.code }.toSet() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "استبدال العملة: ${currentCurrency.displayNameAr} (${currentCurrency.code})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                item {
                    Text(
                        text = "اختر العملة البديلة من قائمة العملات المدعومة (الحد الأقصى 3 عملات نشطة):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(Currency.ALL_SUPPORTED_CURRENCIES) { candidate ->
                    val isAlreadyActive = activeCodes.contains(candidate.code)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isAlreadyActive) {
                                onSelectNewCurrency(candidate)
                            }
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                            .background(if (isAlreadyActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = candidate.displayNameAr,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isAlreadyActive) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${candidate.code})",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "الرمز: ${candidate.symbol} | المنازل: ${candidate.defaultDecimals} | ${candidate.displayNameEn}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        if (isAlreadyActive) {
                            Text(
                                text = "نشطة حالياً",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            TextButton(onClick = { onSelectNewCurrency(candidate) }) {
                                Text("اختيار")
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun AddExchangeRateDialog(
    activeCurrencies: List<Currency>,
    onDismiss: () -> Unit,
    onSaveRate: (from: Currency, to: Currency, rate: BigDecimal, type: RateType, note: String) -> Unit
) {
    var fromCurrency by remember { mutableStateOf(activeCurrencies.firstOrNull() ?: Currency.USD) }
    var toCurrency by remember {
        mutableStateOf(activeCurrencies.find { it.code != fromCurrency.code } ?: Currency.SDG)
    }
    var rateInput by remember { mutableStateOf("") }
    var selectedRateType by remember { mutableStateOf(RateType.REFERENCE) }
    var sourceOrNote by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validateAndSave() {
        errorMessage = null
        if (fromCurrency.code == toCurrency.code) {
            errorMessage = "لا يمكن ربط العملة بنفسها في سعر صرف يدوي"
            return
        }

        val parsedRate = try {
            val trimmed = rateInput.trim()
            if (trimmed.isEmpty()) null else BigDecimal(trimmed)
        } catch (_: Exception) {
            null
        }

        if (parsedRate == null || parsedRate <= BigDecimal.ZERO) {
            errorMessage = "يجب إدخال سعر صرف رقمي موجب تماماً (أكبر من الصفر)"
            return
        }

        onSaveRate(fromCurrency, toCurrency, parsedRate, selectedRateType, sourceOrNote)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تسجيل سعر صرف يدوي جديد",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "حدد اتجاه السعر وقيمته بدقة. اتجاه السعر يحدد كم وحدة من العملة الهدف تعادل وحدة واحدة من العملة المصدر.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Direction selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "العملة المصدر (1 وحدة):", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        CurrencySelectDropdown(
                            currencies = activeCurrencies,
                            selected = fromCurrency,
                            onSelect = {
                                fromCurrency = it
                                if (toCurrency.code == it.code) {
                                    toCurrency = activeCurrencies.find { c -> c.code != it.code } ?: it
                                }
                            }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "العملة الهدف:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        CurrencySelectDropdown(
                            currencies = activeCurrencies.filter { it.code != fromCurrency.code },
                            selected = toCurrency,
                            onSelect = { toCurrency = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rate Input
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = {
                        rateInput = it
                        errorMessage = null
                    },
                    label = { Text("قيمة سعر الصرف") },
                    placeholder = { Text("مثال: 2500.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rate_value"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Direction clarification banner
                if (rateInput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "المعنى: 1 ${fromCurrency.code} = $rateInput ${toCurrency.code}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rate Type
                Text(text = "نوع السعر المنظوري:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Column {
                    RateType.entries.forEach { rType ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedRateType = rType }
                                .padding(vertical = 3.dp)
                        ) {
                            RadioButton(
                                selected = selectedRateType == rType,
                                onClick = { selectedRateType = rType }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = rType.displayNameAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (selectedRateType == rType) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = rType.descriptionAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Source or Note
                OutlinedTextField(
                    value = sourceOrNote,
                    onValueChange = { sourceOrNote = it },
                    label = { Text("المصدر أو الملاحظة (اختياري)") },
                    placeholder = { Text("مثال: بنك الخرطوم، صرافة السوق الموازي") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndSave() },
                modifier = Modifier.testTag("btn_confirm_save_rate")
            ) {
                Text("حفظ السعر")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
