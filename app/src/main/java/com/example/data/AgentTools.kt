package com.example.data

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object AgentTools {
    private val client = OkHttpClient()

    suspend fun executeTool(toolName: String, query: String): String {
        return withContext(Dispatchers.IO) {
            try {
                when (toolName) {
                    "weather" -> fetchWeather(query)
                    "search" -> searchWikipedia(query)
                    "system" -> getSystemInfo()
                    else -> "Tool not found."
                }
            } catch (e: Exception) {
                "Tool execution error: ${e.localizedMessage}"
            }
        }
    }

    private fun fetchWeather(location: String): String {
        // Use Open-Meteo geocoding or direct weather simulation/API
        val url = "https://wttr.in/${java.net.URLEncoder.encode(location, "UTF-8")}?format=3"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.body?.string() ?: "Weather data unavailable."
            }
        }
        return "Could not retrieve weather for $location."
    }

    private fun searchWikipedia(query: String): String {
        val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${java.net.URLEncoder.encode(query, "UTF-8")}&format=json"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val queryObj = json.optJSONObject("query")
                val searchArr = queryObj?.optJSONArray("search")
                if (searchArr != null && searchArr.length() > 0) {
                    val firstResult = searchArr.getJSONObject(0)
                    val title = firstResult.optString("title")
                    val snippet = firstResult.optString("snippet").replace(Regex("<.*?>"), "")
                    return "Wiki Summary ($title): $snippet"
                }
            }
        }
        return "No Wikipedia records found for query: $query"
    }

    private fun getSystemInfo(): String {
        val osVersion = Build.VERSION.RELEASE
        val deviceModel = Build.MODEL
        val brand = Build.BRAND
        return "Device: $brand $deviceModel (Android $osVersion) | CPU Cores: ${Runtime.getRuntime().availableProcessors()} | Free Memory: ${Runtime.getRuntime().freeMemory() / (1024 * 1024)}MB"
    }
}
