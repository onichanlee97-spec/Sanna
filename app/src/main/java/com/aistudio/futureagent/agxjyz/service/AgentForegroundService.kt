package com.aistudio.futureagent.agxjyz.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aistudio.futureagent.agxjyz.MainActivity
import com.aistudio.futureagent.agxjyz.R
import com.aistudio.futureagent.agxjyz.security.AuditLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AgentForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "AgentForegroundChannel"
        const val NOTIFICATION_ID = 1337
        const val EXTRA_TASK_NAME = "taskName"

        fun start(context: Context, taskName: String = "Executing AI Agent Task") {
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                putExtra(EXTRA_TASK_NAME, taskName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskName = intent?.getStringExtra(EXTRA_TASK_NAME) ?: "Executing AI Agent Task"

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sanna AI Agent Active")
            .setContentText(taskName)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        AuditLogger.log(this, "FOREGROUND_TASK_START", "Started: $taskName")
        executeAgentTaskBackground(taskName)

        return START_NOT_STICKY
    }

    private fun executeAgentTaskBackground(taskName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Simulate or execute resilient long-running agent workflow
                delay(3500)
                AuditLogger.log(
                    applicationContext,
                    "FOREGROUND_TASK_COMPLETE",
                    "Finished: $taskName"
                )
            } catch (e: Exception) {
                AuditLogger.log(
                    applicationContext,
                    "FOREGROUND_TASK_ERROR",
                    "Error executing $taskName: ${e.message}"
                )
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Agent Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays ongoing Sanna AI agent background tasks and operations"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
