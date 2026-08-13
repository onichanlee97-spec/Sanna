package com.aistudio.futureagent.agxjyz.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aistudio.futureagent.agxjyz.data.SannaTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SannaNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val packageName = sbn?.packageName ?: "unknown"
        val extras = sbn?.notification?.extras
        val title = extras?.getString("android.title") ?: ""
        val text = extras?.getCharSequence("android.text")?.toString() ?: ""

        Log.d("SannaNotification", "App: $packageName | Title: $title | Text: $text")
        
        serviceScope.launch {
            try {
                SannaTools.logActivity(
                    applicationContext,
                    "Notification Received from '$packageName' ($title: $text)",
                    "TRIAGE"
                )
            } catch (e: Exception) {
                // Safe fallback
            }
        }
    }
}
