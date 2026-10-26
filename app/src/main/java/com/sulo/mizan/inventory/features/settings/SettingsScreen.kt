package com.sulo.mizan.inventory.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.preferences.AppSettings
import com.sulo.mizan.inventory.core.preferences.AppSettingsRepository
import com.sulo.mizan.inventory.core.repository.CurrencyRepository
import com.sulo.mizan.inventory.core.ui.MizanPageHeader
import kotlinx.coroutines.launch
import com.sulo.mizan.inventory.core.ui.MizanSectionTabs

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToCurrencies: () -> Unit = {}
) {
    val context = LocalContext.current
    val appSettingsRepo = remember { AppSettingsRepository(context) }
    val currencyRepo = remember { CurrencyRepository(context) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        currencyRepo.ensureInitialized()
    }

    val settings by appSettingsRepo.appSettingsFlow.collectAsState(initial = AppSettings())
    val activeCurrencies by currencyRepo.activeCurrenciesFlow.collectAsState(initial = Currency.DEFAULT_ACTIVE_CURRENCIES)
    val baseCurrency by currencyRepo.baseCurrencyFlow.collectAsState(initial = Currency.DEFAULT_BASE_CURRENCY)
    var selectedSettingsSection by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        MizanPageHeader(
            title = "الإعدادات",
            subtitle = "بيانات المتجر والعملات والتفضيلات ومعلومات النظام.",
            eyebrow = "تخصيص ميزان",
            icon = Icons.Default.Tune
        )
        Spacer(modifier = Modifier.height(12.dp))
        MizanSectionTabs(
            titles = listOf("عام", "العملة", "النسخ الاحتياطي", "معلومات النظام"),
            selectedIndex = selectedSettingsSection,
            onSelect = { selectedSettingsSection = it }
        )
        Spacer(modifier = Modifier.height(12.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {

        if (selectedSettingsSection == 0) {
        // Merchant & Base Info Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "بيانات المتجر والعملة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = settings.merchantName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "العملة الأساسية المعتمدة: ${baseCurrency.displayNameAr} (${baseCurrency.code}) - ${baseCurrency.symbol}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        }

        if (selectedSettingsSection == 1) {
        // Prominent Phase 2 Currency & Exchange Rate Hub Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToCurrencies() }
                .testTag("card_currency_management")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "إدارة العملات وأسعار الصرف (Phase 2)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "العملات النشطة الثلاث، الأسعار اليدوية، وحاسبة التحويل",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "فتح",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeCurrencies.forEach { c ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (c.code == baseCurrency.code) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            contentColor = if (c.code == baseCurrency.code) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = "${c.code} (${c.symbol})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "تحديد العملة الأساسية للمتجر (من العملات النشطة)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                activeCurrencies.forEach { currency ->
                    val isSelected = baseCurrency.code == currency.code
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                scope.launch {
                                    currencyRepo.setBaseCurrency(currency.code)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("currency_option_${currency.code}")
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                scope.launch {
                                    currencyRepo.setBaseCurrency(currency.code)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currency.displayNameAr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "الرمز: ${currency.symbol} | كود ISO: ${currency.code} | الدقة: ${currency.defaultDecimals} منازل",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        }

        if (selectedSettingsSection == 2) {
        Spacer(modifier = Modifier.height(24.dp))

        BackupRestorePanel(modifier = Modifier.padding(bottom = 8.dp))

        }

        if (selectedSettingsSection == 3) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "معلومات النظام والتحقق التقني (System Diagnostics)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DiagnosticRow(title = "اسم التطبيق", value = "ميزان المخزون — Mizan Inventory")
                DiagnosticRow(title = "المراحل النشطة", value = "PHASE 2–8 (Local-first)")
                DiagnosticRow(title = "نمط الدقة المالية", value = "BigDecimal (Strict Zero-Float)")
                DiagnosticRow(title = "العملة الأساسية", value = "${baseCurrency.displayNameAr} (${baseCurrency.code})")
                DiagnosticRow(title = "العملات النشطة", value = activeCurrencies.joinToString(", ") { it.code })
                DiagnosticRow(title = "قاعدة البيانات", value = "Room 2.7.0 (v3: Inventory & Accounting)")
                DiagnosticRow(title = "سجل الترحيل (Migration)", value = "MIGRATION_1_2 + 2_3")
                DiagnosticRow(title = "مستودع التفضيلات", value = "Jetpack DataStore Preferences 1.1.7")
                DiagnosticRow(title = "نظام الواجهات", value = "Jetpack Compose (Material 3)")
                DiagnosticRow(title = "دعم الاتجاه واللغة", value = "Arabic (RTL Enforced)")
                DiagnosticRow(title = "حزمة التطبيق (Package)", value = "com.sulo.mizan.inventory")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        }
        }
    }
}

@Composable
private fun DiagnosticRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
