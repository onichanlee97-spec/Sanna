package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.data.*
import com.example.data.room.AppDatabase
import com.example.data.room.ChatMessageEntity
import com.example.data.room.AgentTaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgentWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val secureKey = SecureStorage.getApiKey(applicationContext)
            val apiKey = if (secureKey.isNotBlank()) secureKey else BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure()
            }

            val db = AppDatabase.getDatabase(applicationContext)
            val prompt = inputData.getString("prompt") ?: "Perform scheduled background autonomous system check."
            
            // Log automated task
            val taskId = "TASK_BG_${System.currentTimeMillis()}"
            db.taskDao().insertTask(
                AgentTaskEntity(taskId, "Scheduled Agent Trigger", "EXECUTING", "BackgroundWorker", System.currentTimeMillis())
            )

            val systemInstruction = Content(
                parts = listOf(Part(text = "You are an autonomous background AI agent companion. Provide a concise, helpful summary or update report."))
            )

            val request = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = systemInstruction
            )

            val response = GeminiFallbackExecutor.generateWithFallback(applicationContext, apiKey, request)
            val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Background check completed."

            val msgId = System.currentTimeMillis().toString()
            db.chatDao().insertMessage(
                ChatMessageEntity(msgId, false, "[Scheduled Background Report]: $reply", null, System.currentTimeMillis())
            )
            db.taskDao().updateTaskStatus(taskId, "DONE")

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
