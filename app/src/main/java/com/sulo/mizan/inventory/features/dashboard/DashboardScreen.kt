package com.sulo.mizan.inventory.features.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sulo.mizan.inventory.R
import com.sulo.mizan.inventory.core.model.Currency
import com.sulo.mizan.inventory.core.model.CurrencyMismatchException
import com.sulo.mizan.inventory.core.model.Money
import com.sulo.mizan.inventory.core.repository.FinancialSummary
import com.sulo.mizan.inventory.core.repository.InventoryRepository
import java.math.BigDecimal
import com.sulo.mizan.inventory.core.navigation.Screen
import com.sulo.mizan.inventory.core.ui.StatusBadge

@Composable
fun DashboardScreen(
    onNavigateToSection: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val inventoryRepository = remember { InventoryRepository(context) }
    val liveSummary by inventoryRepository.observeFinancialSummary().collectAsState(initial = null)
    LaunchedEffect(Unit) { runCatching { inventoryRepository.ensureInitialized() } }

    var testResultText by remember {
        val m1 = Money.of("125000.50", Currency.SDG)
        val m2 = Money.of("74999.50", Currency.SDG)
        val sum = m1 + m2
        mutableStateOf("عملية دقيقة: ${m1.formatted()} + ${m2.formatted()} = ${sum.formatted()}")
    }
    var testResultSuccess by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("dashboard_screen")
    ) {
        // Compact dashboard heading
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("لوحة المتجر", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("نظرة سريعة على المخزون والحركة المالية", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusBadge(label = "جاهز", icon = Icons.Default.CheckCircle)
        }
        Spacer(Modifier.height(16.dp))

        // Primary workflows are placed first, in a balanced two-column grid.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardQuickTile("المخزون", "الأصناف والأرصدة", Icons.Default.Inventory2, "module_card_inventory", Modifier.weight(1f)) { onNavigateToSection(Screen.Inventory) }
            DashboardQuickTile("الفواتير", "المبيعات والمشتريات", Icons.AutoMirrored.Filled.ReceiptLong, "module_card_operations", Modifier.weight(1f)) { onNavigateToSection(Screen.Operations) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardQuickTile("التقارير", "الأرباح والذمم", Icons.Default.Assessment, "module_card_reports", Modifier.weight(1f)) { onNavigateToSection(Screen.Reports) }
            DashboardQuickTile("العملات", "أسعار الصرف", Icons.Default.CurrencyExchange, "module_card_currencies", Modifier.weight(1f)) { onNavigateToSection(Screen.Currencies) }
        }
        Spacer(Modifier.height(18.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth().testTag("live_business_summary")
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ملخص نشاط المتجر", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    StatusBadge(label = "مباشر", icon = Icons.Default.CheckCircle)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatTile("الأصناف", (liveSummary?.productCount ?: 0).toString(), Modifier.weight(1f))
                    DashboardStatTile("تنبيهات المخزون", (liveSummary?.lowStockCount ?: 0).toString(), Modifier.weight(1f), warning = (liveSummary?.lowStockCount ?: 0) > 0)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatTile("فواتير الشراء", (liveSummary?.purchaseCount ?: 0).toString(), Modifier.weight(1f))
                    DashboardStatTile("فواتير البيع", (liveSummary?.salesCount ?: 0).toString(), Modifier.weight(1f))
                }
                val baseCode = liveSummary?.baseCurrencyCode ?: "SDG"
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                Text("المؤشرات المالية · $baseCode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                DashboardFinancialLine("مجمل الربح", "${(liveSummary?.grossProfitBase ?: BigDecimal.ZERO).stripTrailingZeros().toPlainString()} $baseCode")
                DashboardFinancialLine("تكلفة البضاعة المباعة", "${(liveSummary?.costOfGoodsSoldBase ?: BigDecimal.ZERO).stripTrailingZeros().toPlainString()} $baseCode")
                DashboardFinancialLine("المصروفات", "${(liveSummary?.expensesBase ?: BigDecimal.ZERO).stripTrailingZeros().toPlainString()} $baseCode")
                DashboardFinancialLine(
                    "صافي الربح التقديري",
                    "${(liveSummary?.netProfitBase ?: BigDecimal.ZERO).stripTrailingZeros().toPlainString()} $baseCode",
                    emphasize = true
                )
                val receivablesText = liveSummary?.receivableByCurrency.orEmpty().joinToString { "${it.amount.stripTrailingZeros().toPlainString()} ${it.currencyCode}" }.ifBlank { "لا توجد ذمم مدينة" }
                val payablesText = liveSummary?.payableByCurrency.orEmpty().joinToString { "${it.amount.stripTrailingZeros().toPlainString()} ${it.currencyCode}" }.ifBlank { "لا توجد ذمم دائنة" }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DashboardFinancialLine("المستحق من العملاء", receivablesText)
                        DashboardFinancialLine("المستحق للموردين", payablesText)
                        DashboardFinancialLine("الشحنات المسجلة", (liveSummary?.shipmentCount ?: 0).toString())
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // A single low-noise system status strip keeps focus on business activity.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("حسابات دقيقة وحفظ محلي", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("تُحفظ الحركات محليًا وتُحسب القيم المالية باستخدام دقة عشرية.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Live Financial Engine Demonstration Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("financial_engine_tester")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "التحقق من دقة الحسابات",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "فحص مالي",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "تحقق من دقة الجمع المالي ومنع جمع مبالغ بعملات مختلفة دون سعر صرف.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (testResultSuccess) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = testResultText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (testResultSuccess) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val a = Money.of("0.10000000", Currency.SAR)
                            val b = Money.of("0.20000000", Currency.SAR)
                            val sum = a + b
                            testResultText = "جمع دقيق: 0.10 + 0.20 = ${sum.amount.toPlainString()} SAR (بدون خطأ 0.30000000000000004)"
                            testResultSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_precision_button")
                    ) {
                        Text("اختبار دقة الكسور")
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val yer = Money.of("10000.00", Currency.SDG)
                                val usd = Money.of("50.00", Currency.USD)
                                val ignored = yer + usd
                                testResultText = "فشل الاختبار: سُمح بجمع عملتين مختلفتين!"
                                testResultSuccess = false
                            } catch (e: CurrencyMismatchException) {
                                testResultText = "حماية ناجحة: رُفض الجمع بين YER و USD بدون سعر صرف! (${e.message?.take(55)}...)"
                                testResultSuccess = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_currency_protection_button")
                    ) {
                        Text("اختبار حماية العملات")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DashboardQuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(118.dp).testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DashboardStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    warning: Boolean = false
) {
    val container = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val valueColor = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(5.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun DashboardFinancialLine(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PillarItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    phase: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = phase,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
