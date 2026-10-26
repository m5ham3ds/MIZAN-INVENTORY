package com.sulo.mizan.inventory.diagnostics

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.sulo.mizan.inventory.MainActivity
import com.sulo.mizan.inventory.core.diagnostics.CrashReportStore
import com.sulo.mizan.inventory.core.diagnostics.CrashReporter

/** A small non-Compose launcher gate that can show a saved report before initializing the app UI. */
class CrashGateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pendingReport = CrashReportStore.getPendingReport(this)
        if (pendingReport != null) {
            openCrashReport()
            return
        }

        val interruption = CrashReportStore.consumeInterruptedStartupIfAny(this)
        if (interruption != null) {
            val reportContent = CrashReporter.startupInterruptedReport(this, interruption)
            CrashReportStore.savePendingReport(this, reportContent)
            openCrashReport(reportContent)
            return
        }

        CrashReporter.event(this, "CrashGateActivity.onCreate", "لا يوجد تقرير عطل جديد؛ بدء التشغيل الطبيعي")
        CrashReportStore.beginStartup(this, "CrashGateActivity.onCreate — بدء تشغيل MainActivity")
        try {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } catch (exception: Exception) {
            val report = buildString {
                appendLine("تقرير تشخيص تعطل التطبيق — ميزان المخزون")
                appendLine("نوع التقرير: فشل فتح MainActivity من بوابة التشغيل")
                appendLine("وقت العطل: ${CrashReportStore.formatTimestamp(System.currentTimeMillis())}")
                appendLine("الاستثناء: ${exception.javaClass.name}: ${exception.message}")
                appendLine(exception.stackTraceToString())
            }
            CrashReportStore.savePendingReport(this, report)
            openCrashReport(report)
        }
    }

    private fun openCrashReport(fallbackReport: String? = null) {
        val intent = Intent(this, CrashReportActivity::class.java)
        if (fallbackReport != null) intent.putExtra(CrashReportActivity.EXTRA_FALLBACK_REPORT, fallbackReport)
        startActivity(intent)
        finish()
    }
}
