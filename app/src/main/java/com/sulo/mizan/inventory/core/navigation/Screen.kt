package com.sulo.mizan.inventory.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Main application navigation destinations for Mizan Inventory.
 */
sealed class Screen(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector,
    val testTag: String
) {
    data object Dashboard : Screen(
        route = "dashboard",
        titleResId = com.sulo.mizan.inventory.R.string.nav_dashboard,
        icon = Icons.Default.Dashboard,
        testTag = "nav_item_dashboard"
    )

    data object Inventory : Screen(
        route = "inventory",
        titleResId = com.sulo.mizan.inventory.R.string.nav_inventory,
        icon = Icons.Default.Inventory2,
        testTag = "nav_item_inventory"
    )

    data object Operations : Screen(
        route = "operations",
        titleResId = com.sulo.mizan.inventory.R.string.nav_operations,
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        testTag = "nav_item_operations"
    )

    data object Reports : Screen(
        route = "reports",
        titleResId = com.sulo.mizan.inventory.R.string.nav_reports,
        icon = Icons.Default.Assessment,
        testTag = "nav_item_reports"
    )

    data object Settings : Screen(
        route = "settings",
        titleResId = com.sulo.mizan.inventory.R.string.nav_settings,
        icon = Icons.Default.Settings,
        testTag = "nav_item_settings"
    )

    data object Currencies : Screen(
        route = "currencies",
        titleResId = com.sulo.mizan.inventory.R.string.nav_currencies,
        icon = Icons.Default.CurrencyExchange,
        testTag = "nav_item_currencies"
    )

    companion object {
        /**
         * Resolve the destinations lazily instead of constructing the list while the Screen
         * companion is initialized. Eagerly referencing nested Kotlin objects here can create a
         * class-initialization cycle when Screen.Dashboard is the first Screen value accessed.
         * In that path, the list can capture a not-yet-initialized object reference (null), and a
         * later `screen.route` call crashes with an NPE during the first Compose composition.
         */
        val bottomNavItems: List<Screen> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            listOf(Dashboard, Inventory, Operations, Reports, Settings)
        }
    }
}
