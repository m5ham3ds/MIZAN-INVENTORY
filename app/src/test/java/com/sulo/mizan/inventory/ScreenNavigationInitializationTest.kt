package com.sulo.mizan.inventory

import com.sulo.mizan.inventory.core.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression tests for the startup NPE caused by eager Screen object-list initialization. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenNavigationInitializationTest {

    @Test
    fun `bottom navigation contains initialized screens and valid routes`() {
        // Access Dashboard before the menu to model the startup access order in MainActivity.
        assertEquals("dashboard", Screen.Dashboard.route)

        val items = Screen.bottomNavItems
        assertEquals(5, items.size)
        assertSame(Screen.Dashboard, items[0])
        assertSame(Screen.Inventory, items[1])
        assertSame(Screen.Operations, items[2])
        assertSame(Screen.Reports, items[3])
        assertSame(Screen.Settings, items[4])

        items.forEach { screen ->
            assertNotNull("A bottom navigation Screen must never be null", screen)
            assertTrue("Every route must be non-blank", screen.route.isNotBlank())
        }
        assertEquals(items.size, items.map { it.route }.toSet().size)
    }

    @Test
    fun `currency screen remains separate from bottom navigation and has a route`() {
        assertEquals("currencies", Screen.Currencies.route)
        assertEquals(5, Screen.bottomNavItems.size)
    }
}
