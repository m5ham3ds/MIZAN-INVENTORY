package com.sulo.mizan.inventory.core.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RateSyncScheduler {
    private const val UNIQUE_PERIODIC = "mizan-daily-reference-rates"
    private const val UNIQUE_INITIAL = "mizan-initial-reference-rate-sync"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val initial = OneTimeWorkRequestBuilder<ReferenceRateSyncWorker>().setConstraints(constraints).build()
        val daily = PeriodicWorkRequestBuilder<ReferenceRateSyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_INITIAL, ExistingWorkPolicy.KEEP, initial)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, daily)
    }
}
