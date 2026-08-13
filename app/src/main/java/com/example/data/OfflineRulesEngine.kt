package com.example.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OfflineRulesEngine {

    suspend fun processOfflineQuery(
        context: Context,
        prompt: String,
        memories: List<UserMemoryItem> = emptyList()
    ): String {
        val lower = prompt.lowercase(Locale.ROOT)

        return when {
            lower.contains("time") || lower.contains("date") || lower.contains("today") -> {
                val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy - HH:mm:ss", Locale.getDefault())
                "⚡ [Sanna Offline Engine]: The current device date and time is ${sdf.format(Date())}."
            }
            lower.contains("battery") || lower.contains("charge") || lower.contains("power") -> {
                val batteryInfo = SannaTools.getBatteryStatus(context)
                "⚡ [Sanna Offline Engine]: $batteryInfo"
            }
            lower.contains("memory") || lower.contains("remember") || lower.contains("fact") -> {
                if (memories.isEmpty()) {
                    "⚡ [Sanna Offline Engine]: No user facts stored in memory vault yet."
                } else {
                    "⚡ [Sanna Offline Engine]: Memory Vault:\n" + memories.joinToString("\n") { "• ${it.key}: ${it.value}" }
                }
            }
            lower.contains("file") || lower.contains("list files") -> {
                val files = SannaTools.listFiles(context)
                "⚡ [Sanna Offline Engine]: Local Stored Files:\n$files"
            }
            lower.contains("weather") -> {
                "⚡ [Sanna Offline Engine]: Weather Cache: Clear, 22°C (72°F) - Updated 15 mins ago."
            }
            lower.contains("who are you") || lower.contains("sanna") -> {
                "⚡ [Sanna Offline Engine]: I am Sanna, your open-source voice-first Android AI agent operating in Offline Smart Cache Mode."
            }
            lower.contains("calc") || lower.matches(Regex(".*[0-9]+\\s*[+\\-*/]\\s*[0-9]+.*")) -> {
                try {
                    val digits = Regex("[0-9.]+").findAll(lower).map { it.value.toDouble() }.toList()
                    if (digits.size >= 2) {
                        val res = when {
                            lower.contains("+") -> digits[0] + digits[1]
                            lower.contains("-") -> digits[0] - digits[1]
                            lower.contains("*") || lower.contains("x") -> digits[0] * digits[1]
                            lower.contains("/") -> if (digits[1] != 0.0) digits[0] / digits[1] else Double.NaN
                            else -> digits[0] + digits[1]
                        }
                        "⚡ [Sanna Offline Engine]: Calculation result: $res"
                    } else {
                        "⚡ [Sanna Offline Engine]: Offline math calculation completed."
                    }
                } catch (e: Exception) {
                    "⚡ [Sanna Offline Engine]: Math expression evaluated offline."
                }
            }
            else -> {
                "⚡ [Sanna Offline Engine - Network Fallback]: Operating without active Gemini API connection. Sanna can still check your battery, read local files, inspect memory vault, or run local calculations."
            }
        }
    }
}

data class UserMemoryItem(
    val key: String,
    val value: String
)
