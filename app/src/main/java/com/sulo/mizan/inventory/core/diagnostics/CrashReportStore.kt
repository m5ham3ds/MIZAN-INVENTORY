package com.sulo.mizan.inventory.core.diagnostics

import android.content.Context
import android.os.Process
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** A locally stored crash report. The content is plain text so it can be copied and reviewed easily. */
data class SavedCrashReport(
    val id: String,
    val createdAtMillis: Long,
    val content: String,
    val file: File
)

data class StartupInterruption(
    val startedAtMillis: Long,
    val lastStage: String,
    val lastStageAtMillis: Long
)

/**
 * Crash diagnostics storage. Uses app-private files and synchronous preference commits for the
 * few critical markers that must survive process termination. No network access is used.
 */
object CrashReportStore {
    private const val PREFS_NAME = "mizan_crash_diagnostics"
    private const val KEY_PENDING_FILE = "pending_report_file"
    private const val KEY_STARTUP_IN_PROGRESS = "startup_in_progress"
    private const val KEY_STARTUP_STARTED_AT = "startup_started_at"
    private const val KEY_STARTUP_PROCESS_ID = "startup_process_id"
    private const val KEY_LAST_STAGE = "last_stage"
    private const val KEY_LAST_STAGE_AT = "last_stage_at"
    private const val MAX_REPORTS = 5
    private const val REPORTS_DIRECTORY = "crash-reports"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun reportsDirectory(context: Context): File =
        File(context.applicationContext.filesDir, REPORTS_DIRECTORY).apply { mkdirs() }

    fun recordStage(context: Context, stage: String, details: String = "") {
        val safeStage = stage.replace('\n', ' ').take(180)
        val safeDetails = details.replace('\n', ' ').take(300)
        prefs(context).edit()
            .putString(KEY_LAST_STAGE, if (safeDetails.isBlank()) safeStage else "$safeStage — $safeDetails")
            .putLong(KEY_LAST_STAGE_AT, System.currentTimeMillis())
            .commit()
    }

    fun beginStartup(context: Context, stage: String, processId: Int = Process.myPid()) {
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .putBoolean(KEY_STARTUP_IN_PROGRESS, true)
            .putLong(KEY_STARTUP_STARTED_AT, now)
            .putInt(KEY_STARTUP_PROCESS_ID, processId)
            .putString(KEY_LAST_STAGE, stage.take(180))
            .putLong(KEY_LAST_STAGE_AT, now)
            .commit()
    }

    fun markStartupComplete(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_STARTUP_IN_PROGRESS, false)
            .remove(KEY_STARTUP_STARTED_AT)
            .remove(KEY_STARTUP_PROCESS_ID)
            .putString(KEY_LAST_STAGE, "تم عرض الواجهة الرئيسية")
            .putLong(KEY_LAST_STAGE_AT, System.currentTimeMillis())
            .commit()
    }

    /**
     * Returns a marker only when the previous process began startup but never reached the main
     * activity's onResume and no captured crash report is pending. This is evidence of an
     * interrupted startup, not proof of a Java/Kotlin exception.
     */
    fun consumeInterruptedStartupIfAny(context: Context): StartupInterruption? {
        val preferences = prefs(context)
        if (preferences.getString(KEY_PENDING_FILE, null) != null) return null
        if (!preferences.getBoolean(KEY_STARTUP_IN_PROGRESS, false)) return null
        // A marker created by the current process represents this launch, not a previous failure.
        val startupProcessId = preferences.getInt(KEY_STARTUP_PROCESS_ID, -1)
        if (startupProcessId == Process.myPid()) return null

        val interruption = StartupInterruption(
            startedAtMillis = preferences.getLong(KEY_STARTUP_STARTED_AT, 0L),
            lastStage = preferences.getString(KEY_LAST_STAGE, "غير معروف") ?: "غير معروف",
            lastStageAtMillis = preferences.getLong(KEY_LAST_STAGE_AT, 0L)
        )
        preferences.edit()
            .putBoolean(KEY_STARTUP_IN_PROGRESS, false)
            .remove(KEY_STARTUP_STARTED_AT)
            .remove(KEY_STARTUP_PROCESS_ID)
            .commit()
        return interruption
    }

    /** Saves and marks a report pending. Returns null if durable storage fails. */
    fun savePendingReport(context: Context, content: String): SavedCrashReport? {
        return runCatching {
            val now = System.currentTimeMillis()
            val id = "${now}_${UUID.randomUUID().toString().take(8)}"
            val directory = reportsDirectory(context)
            check(directory.isDirectory) { "تعذر إنشاء مجلد تقارير الأعطال" }
            val target = File(directory, "crash_$id.txt")
            val temporary = File(directory, "crash_$id.tmp")
            FileOutputStream(temporary).use { output ->
                output.write(content.toByteArray(StandardCharsets.UTF_8))
                output.flush()
                output.fd.sync()
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }

            val persisted = prefs(context).edit()
                .putString(KEY_PENDING_FILE, target.name)
                .putBoolean(KEY_STARTUP_IN_PROGRESS, false)
                .remove(KEY_STARTUP_STARTED_AT)
                .remove(KEY_STARTUP_PROCESS_ID)
                .commit()
            check(persisted) { "تعذر حفظ مرجع تقرير العطل" }
            pruneOldReports(context, target.name)
            SavedCrashReport(id, now, content, target)
        }.getOrNull()
    }

    fun getPendingReport(context: Context): SavedCrashReport? {
        val preferences = prefs(context)
        val fileName = preferences.getString(KEY_PENDING_FILE, null) ?: return null
        val file = File(reportsDirectory(context), fileName)
        if (!file.isFile) {
            preferences.edit().remove(KEY_PENDING_FILE).commit()
            return null
        }
        val content = runCatching { file.readText(StandardCharsets.UTF_8) }.getOrNull()
        if (content == null) {
            preferences.edit().remove(KEY_PENDING_FILE).commit()
            return null
        }
        val id = file.name.removePrefix("crash_").removeSuffix(".txt")
        val timestamp = id.substringBefore('_').toLongOrNull() ?: file.lastModified()
        return SavedCrashReport(id, timestamp, content, file)
    }

    fun getLatestSavedReport(context: Context): SavedCrashReport? {
        val pending = getPendingReport(context)
        if (pending != null) return pending
        val file = reportsDirectory(context).listFiles { candidate ->
            candidate.isFile && candidate.name.startsWith("crash_") && candidate.name.endsWith(".txt")
        }?.maxByOrNull { it.lastModified() } ?: return null
        val content = runCatching { file.readText(StandardCharsets.UTF_8) }.getOrNull() ?: return null
        val id = file.name.removePrefix("crash_").removeSuffix(".txt")
        val timestamp = id.substringBefore('_').toLongOrNull() ?: file.lastModified()
        return SavedCrashReport(id, timestamp, content, file)
    }

    /** Acknowledges the report without deleting it, then marks the next main launch as in progress. */
    fun acknowledgeAndBeginStartup(context: Context) {
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .remove(KEY_PENDING_FILE)
            .putBoolean(KEY_STARTUP_IN_PROGRESS, true)
            .putLong(KEY_STARTUP_STARTED_AT, now)
            .putInt(KEY_STARTUP_PROCESS_ID, Process.myPid())
            .putString(KEY_LAST_STAGE, "CrashReportActivity — المستخدم طلب إعادة فتح البرنامج")
            .putLong(KEY_LAST_STAGE_AT, now)
            .commit()
    }

    fun hasPendingReport(context: Context): Boolean = getPendingReport(context) != null

    fun formatTimestamp(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "غير متوفر"
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.ROOT).format(Date(timestampMillis))
    }

    private fun pruneOldReports(context: Context, newestFileName: String) {
        val files = reportsDirectory(context).listFiles { candidate ->
            candidate.isFile && candidate.name.startsWith("crash_") && candidate.name.endsWith(".txt")
        }?.sortedByDescending { it.lastModified() } ?: return
        val protectedName = prefs(context).getString(KEY_PENDING_FILE, newestFileName)
        files.drop(MAX_REPORTS).forEach { file ->
            if (file.name != protectedName && file.name != newestFileName) file.delete()
        }
    }
}
