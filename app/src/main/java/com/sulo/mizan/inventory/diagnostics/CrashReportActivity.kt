package com.sulo.mizan.inventory.diagnostics

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.sulo.mizan.inventory.MainActivity
import com.sulo.mizan.inventory.core.diagnostics.CrashReportStore

/**
 * Intentionally uses platform Views rather than Compose so the diagnostics page is less dependent
 * on the UI stack that may have caused the original startup problem.
 */
class CrashReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "تقرير تشخيص تعطل التطبيق"

        val report = CrashReportStore.getPendingReport(this)
        val reportText = report?.content
            ?: intent.getStringExtra(EXTRA_FALLBACK_REPORT)
            ?: CrashReportStore.getLatestSavedReport(this)?.content
            ?: buildString {
                appendLine("تقرير تشخيص تعطل التطبيق — ميزان المخزون")
                appendLine()
                appendLine("لا يوجد تقرير محفوظ متاح للعرض.")
                appendLine("إذا استمرت المشكلة، افتح التطبيق مجددًا بعد حدوث العطل، ثم انسخ التقرير فور ظهوره.")
            }

        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            layoutDirection = ViewGroup.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(0xFFF6F8FC.toInt())
        }

        val heading = TextView(this).apply {
            text = "تقرير تشخيص تعطل التطبيق"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF172033.toInt())
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(
            heading,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val subheading = TextView(this).apply {
            text = "راجع التفاصيل أو انسخ التقرير كاملًا لإرساله إلى المطوّر. يتضمن Stack Trace وسجل التطبيق الداخلي، وليس Logcat النظام الكامل."
            textSize = 14f
            setTextColor(0xFF475569.toInt())
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(
            subheading,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val scrollView = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
        }
        val reportView = TextView(this).apply {
            text = reportText
            textSize = 12.5f
            typeface = Typeface.create("monospace", Typeface.NORMAL)
            setTextColor(0xFF111827.toInt())
            setTextIsSelectable(true)
            textDirection = TextView.TEXT_DIRECTION_FIRST_STRONG
            layoutDirection = ViewGroup.LAYOUT_DIRECTION_LTR
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        // ScrollView inherits from FrameLayout, so its child requires FrameLayout.LayoutParams.
        scrollView.addView(
            reportView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val copyButton = Button(this).apply {
            text = "نسخ التقرير والسجلات"
            isAllCaps = false
            setOnClickListener {
                try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("Mizan Inventory Crash Report", reportText)
                    )
                    Toast.makeText(
                        this@CrashReportActivity,
                        "تم نسخ التقرير والسجلات",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (exception: Exception) {
                    Toast.makeText(
                        this@CrashReportActivity,
                        "تعذر نسخ التقرير: ${exception.message ?: "خطأ غير معروف"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        root.addView(
            copyButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )

        val reopenButton = Button(this).apply {
            text = "إعادة فتح البرنامج"
            isAllCaps = false
            setOnClickListener {
                CrashReportStore.acknowledgeAndBeginStartup(this@CrashReportActivity)
                val intent = Intent(this@CrashReportActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                try {
                    startActivity(intent)
                    finish()
                } catch (exception: Exception) {
                    Toast.makeText(
                        this@CrashReportActivity,
                        "تعذرت إعادة فتح البرنامج: ${exception.message ?: "خطأ غير معروف"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        root.addView(
            reopenButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
            }
        )

        setContentView(root)
    }

    companion object {
        const val EXTRA_FALLBACK_REPORT = "com.sulo.mizan.inventory.extra.FALLBACK_CRASH_REPORT"
    }
}
