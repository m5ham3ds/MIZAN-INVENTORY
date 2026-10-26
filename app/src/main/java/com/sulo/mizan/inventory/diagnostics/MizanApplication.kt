package com.sulo.mizan.inventory.diagnostics

import android.app.Application
import com.sulo.mizan.inventory.core.diagnostics.CrashReporter
import com.sulo.mizan.inventory.core.work.RateSyncScheduler

/** Installs local crash capture before activities and Compose content are initialized. */
class MizanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        runCatching { RateSyncScheduler.schedule(this) }
    }
}
