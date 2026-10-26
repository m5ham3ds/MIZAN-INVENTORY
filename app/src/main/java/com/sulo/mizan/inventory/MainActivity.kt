package com.sulo.mizan.inventory

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sulo.mizan.inventory.core.diagnostics.CrashReportStore
import com.sulo.mizan.inventory.core.diagnostics.CrashReporter
import com.sulo.mizan.inventory.diagnostics.CrashReportActivity
import com.sulo.mizan.inventory.core.navigation.MizanBottomBar
import com.sulo.mizan.inventory.core.navigation.Screen
import com.sulo.mizan.inventory.core.ui.MizanTopAppBar
import com.sulo.mizan.inventory.features.currency.CurrencyScreen
import com.sulo.mizan.inventory.features.dashboard.DashboardScreen
import com.sulo.mizan.inventory.features.inventory.InventoryScreen
import com.sulo.mizan.inventory.features.operations.OperationsScreen
import com.sulo.mizan.inventory.features.reports.ReportsScreen
import com.sulo.mizan.inventory.features.settings.SettingsScreen
import com.sulo.mizan.inventory.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var firstFrameListenerInstalled = false
    private var startupFrameCommitted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Defensive recovery for task restoration paths that may reopen MainActivity without the
        // launcher gate (for example from Recents after the process has died).
        val pending = CrashReportStore.getPendingReport(this)
        if (pending != null) {
            startActivity(Intent(this, CrashReportActivity::class.java))
            finish()
            return
        }
        val interruption = CrashReportStore.consumeInterruptedStartupIfAny(this)
        if (interruption != null) {
            val text = CrashReporter.startupInterruptedReport(this, interruption)
            CrashReportStore.savePendingReport(this, text)
            startActivity(Intent(this, CrashReportActivity::class.java).apply {
                putExtra(CrashReportActivity.EXTRA_FALLBACK_REPORT, text)
            })
            finish()
            return
        }

        CrashReporter.event(this, "MainActivity.onCreate", "بدء تهيئة Compose والواجهة الرئيسية")
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MizanApp(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CrashReporter.event(this, "MainActivity.onResume", "تم استئناف النشاط؛ ما زال انتظار أول إطار مرئي لتأكيد اكتمال البدء")
    }

    override fun onPostResume() {
        super.onPostResume()
        // Mark startup successful only after the window gets a pre-draw opportunity. This avoids
        // clearing the marker too early if initial Compose composition fails immediately.
        if (firstFrameListenerInstalled || startupFrameCommitted) return
        firstFrameListenerInstalled = true
        val decor = window.decorView
        val observer = decor.viewTreeObserver
        observer.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!startupFrameCommitted) {
                    startupFrameCommitted = true
                    if (observer.isAlive) observer.removeOnPreDrawListener(this)
                    CrashReportStore.markStartupComplete(this@MainActivity)
                    CrashReporter.event(this@MainActivity, "MainActivity.firstPreDraw", "وصلت الواجهة إلى أول إطار قبل الرسم")
                }
                return true
            }
        })
    }
}

@Composable
fun MizanApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val currentScreen: Screen = when (currentRoute) {
        Screen.Currencies.route -> Screen.Currencies
        else -> Screen.bottomNavItems.find { it.route == currentRoute } ?: Screen.Dashboard
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(currentRoute) {
        CrashReporter.event(context, "Navigation", "الوجهة الحالية: $currentRoute")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        topBar = {
            MizanTopAppBar(
                title = stringResource(R.string.app_name),
                subtitle = null,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        },
        bottomBar = {
            MizanBottomBar(
                currentRoute = currentRoute,
                onNavigate = { screen: Screen ->
                    if (screen.route != currentRoute) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToSection = { destination: Screen ->
                        navController.navigate(destination.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Inventory.route) {
                InventoryScreen()
            }

            composable(Screen.Operations.route) {
                OperationsScreen()
            }

            composable(Screen.Reports.route) {
                ReportsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToCurrencies = {
                        navController.navigate(Screen.Currencies.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Currencies.route) {
                CurrencyScreen()
            }
        }
    }
}
