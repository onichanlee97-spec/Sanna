package com.aistudio.futureagent.agxjyz.worker

import android.content.Context
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aistudio.futureagent.agxjyz.data.AutomationEngine
import com.aistudio.futureagent.agxjyz.data.SannaTools
import com.aistudio.futureagent.agxjyz.data.room.AppDatabase
import com.aistudio.futureagent.agxjyz.data.room.AgentTaskEntity
import com.aistudio.futureagent.agxjyz.data.room.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SannaBackgroundWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val rules = AutomationEngine.getRules(applicationContext)
            
            // Query current battery status
            val bm = applicationContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 50
            
            for (rule in rules) {
                if (!rule.isEnabled) continue
                
                var triggered = false
                var triggerLogMessage = ""
                
                when (rule.triggerType) {
                    "BATTERY_LOW" -> {
                        val threshold = rule.conditionValue.toIntOrNull() ?: 20
                        if (batteryLevel <= threshold) {
                            triggered = true
                            triggerLogMessage = "Low Battery Rule Triggered! Current level: $batteryLevel% (Threshold: $threshold%)"
                        }
                    }
                    "INTERVAL" -> {
                        // Intervals always trigger periodically
                        triggered = true
                        triggerLogMessage = "Interval Rule Triggered! Checking background systems."
                    }
                }
                
                if (triggered) {
                    // Log to audit log
                    SannaTools.logActivity(applicationContext, "Background Automation Triggered: ${rule.name}", "SUCCESS")

                    // 1. Log task in DB
                    val taskId = "TASK_AUTO_${System.currentTimeMillis()}"
                    db.taskDao().insertTask(
                        AgentTaskEntity(
                            id = taskId,
                            title = "Rule Triggered: ${rule.name}",
                            status = "DONE",
                            type = "Automation",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    
                    // 2. Insert alert chat message
                    val msgId = System.currentTimeMillis().toString()
                    db.chatDao().insertMessage(
                        ChatMessageEntity(
                            id = msgId,
                            isUser = false,
                            text = "🤖 **[Sanna Engine Automation]** _${rule.name}_\n\n$triggerLogMessage\nExecuting: `${rule.actionPrompt}`",
                            imageBase64 = null,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    
                    // 3. Execute actions (e.g. if rule says toggle Bluetooth OFF, we do so!)
                    if (rule.actionPrompt.contains("Bluetooth", ignoreCase = true) && rule.actionPrompt.contains("OFF", ignoreCase = true)) {
                        SannaTools.toggleBluetooth(applicationContext, false)
                    }
                    if (rule.actionPrompt.contains("volume", ignoreCase = true)) {
                        SannaTools.setVolume(applicationContext, 15)
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
