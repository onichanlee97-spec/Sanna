package com.aistudio.futureagent.agxjyz.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aistudio.futureagent.agxjyz.MainActivity
import com.aistudio.futureagent.agxjyz.R
import com.aistudio.futureagent.agxjyz.data.room.AgentRepository
import com.aistudio.futureagent.agxjyz.data.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ApprovalActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val approvalId = intent.getStringExtra("approval_id") ?: return
        val approved = intent.getBooleanExtra("approved", false)
        
        val database = AppDatabase.getDatabase(context)
        val repository = AgentRepository(
            database.chatDao(),
            database.taskDao(),
            database.memoryDao(),
            database.vectorDao(),
            database.approvalDao(),
            database.offlineQueueDao()
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            repository.updateApprovalStatus(approvalId, if (approved) "APPROVED" else "DENIED", "Operator_Notification")
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(approvalId.hashCode())
        }
    }
}

object NotificationHelper {
    private const val CHANNEL_ID = "agent_approvals"
    private const val CHANNEL_NAME = "Agent Authorizations"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "High-priority notifications for security guardrail approvals"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun cancelNotification(context: Context, approvalId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(approvalId.hashCode())
    }

    fun postApprovalNotification(context: Context, approvalId: String, actionName: String, riskLevel: String) {
        val approveIntent = Intent(context, ApprovalActionReceiver::class.java).apply {
            putExtra("approval_id", approvalId)
            putExtra("approved", true)
        }
        val approvePendingIntent = PendingIntent.getBroadcast(
            context,
            approvalId.hashCode() + 1,
            approveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val denyIntent = Intent(context, ApprovalActionReceiver::class.java).apply {
            putExtra("approval_id", approvalId)
            putExtra("approved", false)
        }
        val denyPendingIntent = PendingIntent.getBroadcast(
            context,
            approvalId.hashCode() + 2,
            denyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🛡️ Security Authorization Required")
            .setContentText("Action: $actionName (Risk: $riskLevel)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "AUTHORIZE", approvePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DENY", denyPendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(approvalId.hashCode(), builder.build())
    }

    fun rehydratePendingApprovals(context: Context, repository: AgentRepository) {
        CoroutineScope(Dispatchers.IO).launch {
            val list = repository.allApprovals.first()
            list.forEach { approval ->
                if (approval.status == "PENDING") {
                    postApprovalNotification(context, approval.id, approval.actionName, approval.riskLevel)
                }
            }
        }
    }
}
