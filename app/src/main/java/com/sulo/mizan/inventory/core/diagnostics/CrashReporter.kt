package com.sulo.mizan.inventory.core.diagnostics

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/** Installs a conservative uncaught-exception handler and keeps a small in-memory breadcrumb ring. */
object CrashReporter {
    private const val MAX_EVENTS = 200
    private val events = ArrayDeque<String>()
    private val handlingCrash = AtomicBoolean(false)
    @Volatile private var installed = false

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (previousHandler !is MizanUncaughtExceptionHandler) {
            Thread.setDefaultUncaughtExceptionHandler(
                MizanUncaughtExceptionHandler(context.applicationContext, previousHandler)
            )
        }
        installed = true
    }

    fun event(context: Context, stage: String, details: String = "") {
        val time = CrashReportStore.formatTimestamp(System.currentTimeMillis())
        val safeDetails = details.replace('\n', ' ').take(300)
        val line = if (safeDetails.isBlank()) "$time | $stage" else "$time | $stage | $safeDetails"
        synchronized(events) {
            events.addLast(line)
            while (events.size > MAX_EVENTS) events.removeFirst()
        }
        runCatching { CrashReportStore.recordStage(context, stage, safeDetails) }
    }

    fun startupInterruptedReport(context: Context, interruption: StartupInterruption): String {
        val now = System.currentTimeMillis()
        val formattedStart = CrashReportStore.formatTimestamp(interruption.startedAtMillis)
        val formattedStage = CrashReportStore.formatTimestamp(interruption.lastStageAtMillis)
        return buildString {
            appendLine("تقرير تشخيص تعطل التطبيق — ميزان المخزون")
            appendLine("============================================================")
            appendLine("نوع التقرير: انقطاع غير مفسر أثناء بدء التشغيل")
            appendLine("معرّف التقرير: startup-interrupted-$now")
            appendLine("وقت إنشاء التقرير: ${CrashReportStore.formatTimestamp(now)}")
            appendLine("وقت بداية التشغيل السابق: $formattedStart")
            appendLine("آخر مرحلة مسجلة: ${interruption.lastStage}")
            appendLine("وقت آخر مرحلة مسجلة: $formattedStage")
            appendLine()
            appendLine("تنبيه مهم:")
            appendLine("لم يتم التقاط Stack Trace لاستثناء Java/Kotlin في التشغيل السابق.")
            appendLine("يدل هذا التقرير فقط على أن تشغيلًا سابقًا لم يسجل الوصول إلى واجهة التطبيق الرئيسية.")
            appendLine("لا يثبت وحده أن السبب كان استثناءً؛ قد تشمل الأسباب قتل العملية أو عطلًا أصليًا أو إيقافًا قسريًا.")
            appendLine()
            appendDeviceAndAppDetails(this, context)
            appendLine("\nآخر أحداث متاحة في العملية الحالية:")
            appendLine("لا توجد أحداث ذاكرة سابقة؛ فقد انتهت العملية السابقة دون سجل استثناء قابل للاستعادة.")
        }
    }

    private fun createUncaughtReport(context: Context, thread: Thread, throwable: Throwable): String {
        val now = System.currentTimeMillis()
        val stackTrace = StringWriter().also { writer ->
            PrintWriter(writer).use { printer -> throwable.printStackTrace(printer) }
        }.toString()
        val eventSnapshot = synchronized(events) { events.toList() }
        return buildString {
            appendLine("تقرير تشخيص تعطل التطبيق — ميزان المخزون")
            appendLine("============================================================")
            appendLine("نوع التقرير: استثناء غير معالج")
            appendLine("معرّف التقرير: crash-$now")
            appendLine("وقت وقوع العطل: ${CrashReportStore.formatTimestamp(now)}")
            appendLine("الخيط: ${thread.name}")
            appendLine("فئة الاستثناء: ${throwable.javaClass.name}")
            appendLine("رسالة الاستثناء: ${throwable.message ?: "لا توجد رسالة استثناء"}")
            appendLine()
            appendDeviceAndAppDetails(this, context)
            appendLine("\nنطاق السجلات:")
            appendLine("يتضمن التقرير Stack Trace والاستثناءات التابعة وآخر أحداث التشخيص التي سجلها التطبيق.")
            appendLine("لا يتضمن Logcat الكامل للنظام؛ التطبيقات العادية لا تملك صلاحية قراءة جميع سجلات النظام على أجهزة Android غير المهيأة للتطوير.")
            appendLine("\nStack Trace الكامل (يشمل الأسباب والاستثناءات الإضافية التي يطبعها Android/JVM):")
            appendLine(stackTrace.ifBlank { "لم يتوفر Stack Trace." })
            appendLine("\nآخر أحداث التشخيص المسجلة (من الأقدم إلى الأحدث):")
            if (eventSnapshot.isEmpty()) appendLine("لا توجد أحداث تشخيص مسجلة.")
            else eventSnapshot.forEach(::appendLine)
        }
    }

    private fun appendDeviceAndAppDetails(builder: StringBuilder, context: Context) {
        val packageInfo: PackageInfo? = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val versionCode = packageInfo?.let { info ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode.toString()
            } else {
                legacyVersionCode(info)
            }
        } ?: "غير متوفر"
        builder.appendLine("معلومات التطبيق:")
        builder.appendLine("- الاسم: ميزان المخزون")
        builder.appendLine("- Package: ${context.packageName}")
        builder.appendLine("- Version name: ${packageInfo?.versionName ?: "غير متوفر"}")
        builder.appendLine("- Version code: $versionCode")
        builder.appendLine()
        builder.appendLine("معلومات الجهاز:")
        builder.appendLine("- Manufacturer: ${Build.MANUFACTURER}")
        builder.appendLine("- Model: ${Build.MODEL}")
        builder.appendLine("- Device: ${Build.DEVICE}")
        builder.appendLine("- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        builder.appendLine("- Build fingerprint: ${Build.FINGERPRINT}")
    }

    @Suppress("DEPRECATION")
    private fun legacyVersionCode(packageInfo: PackageInfo): String = packageInfo.versionCode.toString()

    private class MizanUncaughtExceptionHandler(
        private val context: Context,
        private val delegate: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            if (CrashReporter.handlingCrash.compareAndSet(false, true)) {
                try {
                    val report = CrashReporter.createUncaughtReport(context, thread, throwable)
                    CrashReportStore.savePendingReport(context, report)
                } catch (_: Throwable) {
                    // Never throw from a crash handler; allow the platform's original handler to run.
                }
            }
            if (delegate != null && delegate !== this) {
                delegate.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }
}
