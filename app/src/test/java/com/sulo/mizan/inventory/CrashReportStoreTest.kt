package com.sulo.mizan.inventory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sulo.mizan.inventory.core.diagnostics.CrashReportStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashReportStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mizan_crash_diagnostics", Context.MODE_PRIVATE)
            .edit().clear().commit()
        File(context.filesDir, "crash-reports").deleteRecursively()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("mizan_crash_diagnostics", Context.MODE_PRIVATE)
            .edit().clear().commit()
        File(context.filesDir, "crash-reports").deleteRecursively()
    }

    @Test
    fun `saved report is persisted and exposed as pending`() {
        val saved = CrashReportStore.savePendingReport(context, "FATAL EXCEPTION\nCaused by: sample")

        assertNotNull(saved)
        val pending = CrashReportStore.getPendingReport(context)
        assertNotNull(pending)
        assertEquals("FATAL EXCEPTION\nCaused by: sample", pending?.content)
        assertTrue(pending?.file?.isFile == true)
    }

    @Test
    fun `interrupted startup marker is consumed once`() {
        CrashReportStore.beginStartup(context, "MainActivity.onCreate", processId = -1)

        val interruption = CrashReportStore.consumeInterruptedStartupIfAny(context)
        assertNotNull(interruption)
        assertEquals("MainActivity.onCreate", interruption?.lastStage)
        assertNull(CrashReportStore.consumeInterruptedStartupIfAny(context))
    }

    @Test
    fun `current process startup marker is not treated as previous crash`() {
        CrashReportStore.beginStartup(context, "MainActivity.onCreate")

        assertNull(CrashReportStore.consumeInterruptedStartupIfAny(context))
    }

    @Test
    fun `acknowledging report clears pending pointer but preserves report file`() {
        val saved = CrashReportStore.savePendingReport(context, "Saved crash report")
        assertNotNull(saved)

        CrashReportStore.acknowledgeAndBeginStartup(context)

        assertNull(CrashReportStore.getPendingReport(context))
        val archived = CrashReportStore.getLatestSavedReport(context)
        assertNotNull(archived)
        assertEquals("Saved crash report", archived?.content)
        assertTrue(archived?.file?.isFile == true)
    }
}
