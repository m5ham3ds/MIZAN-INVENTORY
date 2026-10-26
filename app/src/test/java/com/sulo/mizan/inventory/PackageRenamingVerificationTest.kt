package com.sulo.mizan.inventory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 2A Automated Acceptance Verification Test:
 * Verifies that the package renaming to "com.sulo.mizan.inventory" is complete,
 * valid, and fully synchronized across Application ID, Context, and Resource references.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PackageRenamingVerificationTest {

    @Test
    fun testTargetApplicationIdMatchesRequirement() {
        assertEquals("com.sulo.mizan.inventory", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun testTargetNamespaceAndContextPackageName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.sulo.mizan.inventory", context.packageName)
    }

    @Test
    fun testRClassResourceAccessUnderNewPackage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ميزان المخزون", appName)
        assertNotNull(R.string.app_name)
    }
}
