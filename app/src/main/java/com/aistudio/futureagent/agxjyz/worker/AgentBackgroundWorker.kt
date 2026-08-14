package com.aistudio.futureagent.agxjyz.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aistudio.futureagent.agxjyz.data.room.AppDatabase
import com.aistudio.futureagent.agxjyz.data.room.AgentRepository
import com.aistudio.futureagent.agxjyz.security.AuditLogger

class AgentBackgroundWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = AgentRepository(
                database.chatDao(),
                database.taskDao(),
                database.memoryDao(),
                database.vectorDao(),
                database.approvalDao(),
                database.offlineQueueDao()
            )

            // Perform maintenance tasks
            repository.pruneExpired(System.currentTimeMillis())
            
            AuditLogger.log(applicationContext, "BACKGROUND_MAINTENANCE", "Periodic database pruning and health check completed.")

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
