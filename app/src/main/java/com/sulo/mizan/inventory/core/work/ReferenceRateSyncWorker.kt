package com.sulo.mizan.inventory.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sulo.mizan.inventory.core.repository.AutomaticExchangeRateUpdater

class ReferenceRateSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        AutomaticExchangeRateUpdater(applicationContext).updateNow(force = false)
        Result.success()
    } catch (error: Exception) {
        Result.retry()
    }
}
