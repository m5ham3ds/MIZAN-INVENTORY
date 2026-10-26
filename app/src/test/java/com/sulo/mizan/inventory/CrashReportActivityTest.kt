package com.sulo.mizan.inventory

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.sulo.mizan.inventory.diagnostics.CrashReportActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashReportActivityTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mizan_crash_diagnostics", Context.MODE_PRIVATE)
            .edit().clear().commit()
        File(context.filesDir, "crash-reports").deleteRecursively()
    }

    @Test
    fun `screen displays report and exactly the two requested buttons`() {
        val marker = "TEST_FATAL_EXCEPTION_STACK_TRACE"
        val intent = Intent(context, CrashReportActivity::class.java)
            .putExtra(CrashReportActivity.EXTRA_FALLBACK_REPORT, marker)

        val activity = Robolectric.buildActivity(CrashReportActivity::class.java, intent)
            .setup()
            .get()
        val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content)
        val views = mutableListOf<View>()
        collectViews(contentRoot, views)

        assertTrue("The diagnostic report text should be displayed", views.filterIsInstance<TextView>().any { it.text.contains(marker) })
        val buttons = views.filterIsInstance<Button>()
        assertEquals(2, buttons.size)
        assertTrue(buttons.any { it.text.toString() == "نسخ التقرير والسجلات" })
        assertTrue(buttons.any { it.text.toString() == "إعادة فتح البرنامج" })
    }

    @Test
    fun `copy button copies the full displayed report`() {
        val marker = "FULL_STACK_TRACE_MARKER_123"
        val intent = Intent(context, CrashReportActivity::class.java)
            .putExtra(CrashReportActivity.EXTRA_FALLBACK_REPORT, "تقرير اختبار\n$marker")
        val activity = Robolectric.buildActivity(CrashReportActivity::class.java, intent)
            .setup()
            .get()
        val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content)
        val views = mutableListOf<View>()
        collectViews(contentRoot, views)
        val copyButton = views.filterIsInstance<Button>().single { it.text.toString() == "نسخ التقرير والسجلات" }

        copyButton.performClick()

        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        assertNotNull(clip)
        assertTrue(clip!!.getItemAt(0).text.toString().contains(marker))
    }

    private fun collectViews(view: View, destination: MutableList<View>) {
        destination += view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) collectViews(view.getChildAt(index), destination)
        }
    }
}
