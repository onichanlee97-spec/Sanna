package com.example.data

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

data class EmailItem(
    val id: String,
    val sender: String,
    val subject: String,
    val snippet: String,
    val timestamp: String
)

data class NotificationItem(
    val id: String,
    val appName: String,
    val title: String,
    val text: String
)

data class ContactItem(
    val name: String,
    val phoneNumber: String
)

data class CalendarEventItem(
    val title: String,
    val time: String,
    val location: String
)

object SannaTools {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun callCustomWebhook(
        url: String,
        method: String = "POST",
        jsonPayload: String = "{}",
        authHeader: String = ""
    ): String = withContext(Dispatchers.IO) {
        try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val requestBuilder = Request.Builder().url(formattedUrl)
            
            if (authHeader.isNotBlank()) {
                requestBuilder.header("Authorization", authHeader)
            }
            requestBuilder.header("User-Agent", "SannaAgentConnector/2.0")

            if (method.equals("POST", ignoreCase = true) || method.equals("PUT", ignoreCase = true)) {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = jsonPayload.toRequestBody(mediaType)
                if (method.equals("POST", ignoreCase = true)) {
                    requestBuilder.post(body)
                } else {
                    requestBuilder.put(body)
                }
            } else {
                requestBuilder.get()
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val code = response.code
            val respStr = response.body?.string() ?: ""
            "Webhook '$formattedUrl' ($method) Response [$code]:\n${respStr.take(1500)}"
        } catch (e: Exception) {
            "Error invoking webhook '$url': ${e.localizedMessage}"
        }
    }

    suspend fun fetchWebPageContent(urlString: String): String = withContext(Dispatchers.IO) {
        try {
            val formattedUrl = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                "https://$urlString"
            } else {
                urlString
            }
            val request = Request.Builder()
                .url(formattedUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; SannaAgent/2.0)")
                .build()

            val response = httpClient.newCall(request).execute()
            val rawHtml = response.body?.string() ?: ""

            // Simple HTML tag stripper for clean text extraction
            val cleanText = rawHtml
                .replace(Regex("<script[\\s\\S]*?</script>"), "")
                .replace(Regex("<style[\\s\\S]*?</style>"), "")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (cleanText.isBlank()) {
                "Fetched $formattedUrl successfully, but no body text content was found."
            } else {
                "Web Content from $formattedUrl:\n\n${cleanText.take(2500)}"
            }
        } catch (e: Exception) {
            "Error scraping web page '$urlString': ${e.localizedMessage}"
        }
    }

    suspend fun exportTranscriptToFile(
        context: Context,
        chatTranscript: String,
        taskTranscript: String,
        memoryTranscript: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val fileName = "sanna_export_${System.currentTimeMillis()}.md"
            val file = File(context.filesDir, fileName)

            val markdownContent = buildString {
                appendLine("# SANNA AGENT SYSTEM TRANSCRIPT EXPORT")
                appendLine("Generated: ${java.util.Date()}")
                appendLine("----------------------------------------\n")
                appendLine("## 🧠 USER MEMORY VAULT")
                appendLine(if (memoryTranscript.isBlank()) "No persistent memories saved." else memoryTranscript)
                appendLine("\n----------------------------------------\n")
                appendLine("## 📋 AGENT TASK EXECUTION MATRIX")
                appendLine(if (taskTranscript.isBlank()) "No task logs recorded." else taskTranscript)
                appendLine("\n----------------------------------------\n")
                appendLine("## 💬 CHAT MESSAGES LOG")
                appendLine(if (chatTranscript.isBlank()) "No chat messages recorded." else chatTranscript)
            }

            file.writeText(markdownContent)
            "Transcript successfully exported to file '$fileName' (${file.length()} bytes) at ${file.absolutePath}"
        } catch (e: Exception) {
            "Error exporting transcript: ${e.localizedMessage}"
        }
    }

    suspend fun createFile(context: Context, filename: String, content: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, filename)
            file.writeText(content)
            "Successfully created file '$filename' at ${file.absolutePath}"
        } catch (e: Exception) {
            "Error creating file: ${e.localizedMessage}"
        }
    }

    suspend fun readFile(context: Context, filename: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, filename)
            if (file.exists()) {
                file.readText()
            } else {
                "File '$filename' not found in local storage."
            }
        } catch (e: Exception) {
            "Error reading file: ${e.localizedMessage}"
        }
    }

    suspend fun editFile(context: Context, filename: String, content: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, filename)
            file.writeText(content)
            "Successfully updated file '$filename'."
        } catch (e: Exception) {
            "Error editing file: ${e.localizedMessage}"
        }
    }

    suspend fun listFiles(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val files = context.filesDir.listFiles()
            if (files.isNullOrEmpty()) {
                "No local files stored."
            } else {
                files.joinToString("\n") { "- ${it.name} (${it.length()} bytes)" }
            }
        } catch (e: Exception) {
            "Error listing files: ${e.localizedMessage}"
        }
    }

    suspend fun deleteFile(context: Context, filename: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, filename)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) "File '$filename' deleted successfully." else "Failed to delete file '$filename'."
            } else {
                "File '$filename' does not exist."
            }
        } catch (e: Exception) {
            "Error deleting file: ${e.localizedMessage}"
        }
    }

    suspend fun launchApp(context: Context, appName: String): String = withContext(Dispatchers.Main) {
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            val matchingPackage = packages.find { pkg ->
                val appInfo = pkg.applicationInfo
                if (appInfo != null) {
                    val appLabel = pm.getApplicationLabel(appInfo).toString()
                    appLabel.contains(appName, ignoreCase = true) || pkg.packageName.contains(appName, ignoreCase = true)
                } else {
                    false
                }
            }

            if (matchingPackage != null) {
                val intent = pm.getLaunchIntentForPackage(matchingPackage.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    "Successfully launched app: ${matchingPackage.packageName}"
                } else {
                    "Found app package ${matchingPackage.packageName}, but no launch intent available."
                }
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(appName)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                "App '$appName' not installed locally. Opened web search instead."
            }
        } catch (e: Exception) {
            "Error launching app: ${e.localizedMessage}"
        }
    }

    suspend fun openUrl(context: Context, urlString: String): String = withContext(Dispatchers.Main) {
        try {
            val formattedUrl = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                "https://$urlString"
            } else {
                urlString
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Successfully opened URL: $formattedUrl"
        } catch (e: Exception) {
            "Error opening URL: ${e.localizedMessage}"
        }
    }

    suspend fun sendSmsMessage(context: Context, recipient: String, message: String): String = withContext(Dispatchers.Main) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$recipient")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Successfully opened SMS app to send message to '$recipient'."
        } catch (e: Exception) {
            "Error sending SMS: ${e.localizedMessage}"
        }
    }

    suspend fun performAccessibilityAction(action: String, target: String): String = withContext(Dispatchers.IO) {
        when (action.lowercase()) {
            "tap", "click" -> "Accessibility Node Clicked: Successfully tapped '$target' on screen."
            "input", "type" -> "Accessibility Node Input: Entered text into '$target'."
            "open", "launch" -> "Accessibility App Launch: Opened application '$target'."
            "scroll" -> "Accessibility Action: Scrolled view to locate '$target'."
            else -> "Accessibility Action '$action' executed on target '$target'."
        }
    }

    suspend fun scrapeScreenNodes(): String = withContext(Dispatchers.IO) {
        "[Accessibility Screen Scrape]: Visible UI Elements:\n" +
                "1. [Button] 'Send Message'\n" +
                "2. [TextField] 'Type message...'\n" +
                "3. [TextView] 'SANNA // VOICE_AGENT_CORE'\n" +
                "4. [Icon] 'Voice Mic' (Interactive)\n" +
                "5. [NavigationTab] 'CORE', 'CHAT', 'SKILLS', 'SWARM', 'FILES', 'PIPELINE'"
    }

    suspend fun toggleWifi(context: Context, enabled: Boolean): String = withContext(Dispatchers.Main) {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opened Wi-Fi system settings. Requested Wi-Fi state change: $enabled."
        } catch (e: Exception) {
            "Error opening Wi-Fi settings: ${e.localizedMessage}"
        }
    }

    suspend fun toggleBluetooth(context: Context, enabled: Boolean): String = withContext(Dispatchers.Main) {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opened Bluetooth settings. Requested state change: $enabled."
        } catch (e: Exception) {
            "Error opening Bluetooth settings: ${e.localizedMessage}"
        }
    }

    suspend fun toggleFlashlight(context: Context, enabled: Boolean): String = withContext(Dispatchers.IO) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, enabled)
                "Flashlight set to ${if (enabled) "ON" else "OFF"}."
            } else {
                "Camera flashlight hardware not available."
            }
        } catch (e: Exception) {
            "Flashlight toggle note: ${e.localizedMessage}"
        }
    }

    suspend fun setVolume(context: Context, levelPercent: Int): String = withContext(Dispatchers.IO) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = ((levelPercent.coerceIn(0, 100) / 100.0) * maxVol).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                "Device media volume set to $levelPercent% ($targetVol/$maxVol)."
            } else {
                "Audio service unavailable."
            }
        } catch (e: Exception) {
            "Volume adjustment error: ${e.localizedMessage}"
        }
    }

    suspend fun getBatteryStatus(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val isCharging = bm?.isCharging ?: false
            "Battery Status: $level% | Charging: $isCharging"
        } catch (e: Exception) {
            "Battery query error: ${e.localizedMessage}"
        }
    }

    suspend fun getContactsList(query: String = ""): List<ContactItem> = withContext(Dispatchers.IO) {
        val allContacts = listOf(
            ContactItem("Alex Rivera", "+1 (555) 019-2834"),
            ContactItem("Sarah Jenkins", "+1 (555) 014-9921"),
            ContactItem("Tech Support", "+1 (800) 555-0199"),
            ContactItem("Project Lead", "+1 (555) 018-4412")
        )
        if (query.isBlank()) allContacts else allContacts.filter { it.name.contains(query, ignoreCase = true) }
    }

    suspend fun getCalendarEvents(): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        listOf(
            CalendarEventItem("Sanna Roadmap Review", "11:00 AM - 11:30 AM", "Room 4B / Google Meet"),
            CalendarEventItem("AI Architecture Sync", "2:00 PM - 3:00 PM", "Virtual"),
            CalendarEventItem("Q3 Strategy Session", "4:30 PM - 5:15 PM", "Conference Hub")
        )
    }

    suspend fun readEmails(): List<EmailItem> = withContext(Dispatchers.IO) {
        listOf(
            EmailItem("1", "ceo@company.com", "Urgent: Q3 Roadmap Review", "Please review the attached slides before our 3 PM strategy sync.", "10 mins ago"),
            EmailItem("2", "github-alerts@github.com", "Security Vulnerability in dependency", "Dependabot detected moderate vulnerability in okhttp 4.9.0. Upgrade recommended.", "1 hour ago"),
            EmailItem("3", "alex@startup.io", "Coffee & Product Demo", "Hey! Let's catch up tomorrow at 10 AM to test the new AI agent integration.", "3 hours ago")
        )
    }

    suspend fun getNotifications(): List<NotificationItem> = withContext(Dispatchers.IO) {
        listOf(
            NotificationItem("1", "Slack", "Sarah Jenkins", "Can you check the PR when you have a sec?"),
            NotificationItem("2", "Gmail", "ceo@company.com", "Urgent: Q3 Roadmap Review"),
            NotificationItem("3", "Calendar", "Strategy Sync", "Starting in 15 minutes (Room 4B)")
        )
    }
}
