package com.aistudio.futureagent.agxjyz

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.aistudio.futureagent.agxjyz.worker.SannaBackgroundWorker
import java.util.concurrent.TimeUnit

class SannaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<SannaBackgroundWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SannaBackgroundTelemetry",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
