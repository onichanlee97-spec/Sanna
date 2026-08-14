package com.aistudio.futureagent.agxjyz

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.aistudio.futureagent.agxjyz.worker.SannaBackgroundWorker
import com.aistudio.futureagent.agxjyz.utils.NotificationHelper
import com.aistudio.futureagent.agxjyz.data.room.AppDatabase
import com.aistudio.futureagent.agxjyz.data.room.AgentRepository
import com.aistudio.futureagent.agxjyz.connectivity.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.io.File
import android.content.ComponentCallbacks2

class SannaApplication : Application(), ComponentCallbacks2 {

    private var networkMonitor: NetworkMonitor? = null

    override fun onCreate() {
        super.onCreate()
        try {
            NotificationHelper.createNotificationChannel(this)
            
            networkMonitor = NetworkMonitor(this).apply {
                startMonitoring()
            }
            
            // Background Initialization and Rehydration
            val database = AppDatabase.getDatabase(this)
            val repository = AgentRepository(
                database.chatDao(),
                database.taskDao(),
                database.memoryDao(),
                database.vectorDao(),
                database.approvalDao(),
                database.offlineQueueDao()
            )
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Rehydrate pending approval notifications
                    NotificationHelper.rehydratePendingApprovals(this@SannaApplication, repository)
                    
                    // 2. Establish atomic vector store pruning upon cold boot
                    repository.pruneExpired(System.currentTimeMillis())
                } catch (e: Exception) {
                    // Fail silently for background tasks
                }
            }

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
            // Safe fallback to prevent app crash on startup
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            clearAppCaches()
        }
    }

    private fun clearAppCaches() {
        try {
            val cacheDir = cacheDir
            cacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            // Fail silently
        }
    }
}
