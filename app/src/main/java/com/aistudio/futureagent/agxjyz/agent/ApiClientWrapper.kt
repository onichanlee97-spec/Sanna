package com.aistudio.futureagent.agxjyz.agent

import android.content.Context
import com.aistudio.futureagent.agxjyz.security.AuditLogger
import com.aistudio.futureagent.agxjyz.utils.ApiKeyManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ApiClientWrapper(private val context: Context) {

    private val apiKeyManager = ApiKeyManager(context)
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun executeAiRequestWithFailover(urlEndpoint: String, jsonPayload: String): String {
        val totalKeys = apiKeyManager.getTotalKeysCount()
        val maxAttempts = max(1, totalKeys)
        var attempt = 0

        if (totalKeys == 0) {
            AuditLogger.logEvent(context, "API_ERROR", "No API keys configured in ApiKeyManager.")
            return "Error: No API keys configured. Please add keys in settings or developer tools."
        }

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        while (attempt < maxAttempts) {
            val currentKey = apiKeyManager.getCurrentApiKey()
            if (currentKey.isBlank()) {
                val rotated = apiKeyManager.rotateToNextKey()
                if (!rotated) break
                attempt++
                continue
            }

            try {
                // Determine URL with API key param if not present or in headers
                val effectiveUrl = if (urlEndpoint.contains("generativelanguage.googleapis.com") && !urlEndpoint.contains("key=")) {
                    val separator = if (urlEndpoint.contains("?")) "&" else "?"
                    "$urlEndpoint${separator}key=$currentKey"
                } else {
                    urlEndpoint
                }

                val body = jsonPayload.toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(effectiveUrl)
                    .addHeader("Authorization", "Bearer $currentKey")
                    .addHeader("x-goog-api-key", currentKey)
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val statusCode = response.code
                    val responseBody = response.body?.string() ?: ""

                    // Check for Rate Limit (429) or Quota Exceeded (403 / specific error bodies)
                    val isQuotaExceeded = statusCode == 429 ||
                            (statusCode == 403 && responseBody.contains("quota", ignoreCase = true)) ||
                            responseBody.contains("resource_exhausted", ignoreCase = true) ||
                            responseBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                            responseBody.contains("RATE_LIMIT_EXCEEDED", ignoreCase = true)

                    if (isQuotaExceeded) {
                        AuditLogger.logEvent(
                            context,
                            "API_FAILOVER",
                            "API Key index ${apiKeyManager.getCurrentKeyIndex()} exhausted quota (Code: $statusCode). Rotating..."
                        )

                        val rotated = apiKeyManager.rotateToNextKey()
                        if (!rotated) {
                            AuditLogger.logEvent(context, "API_FATAL", "All configured API keys have exhausted their quotas.")
                            return "Error: All configured API keys have exhausted their quotas (HTTP $statusCode: $responseBody)"
                        }
                        attempt++
                        return@use // continue while loop
                    }

                    if (!response.isSuccessful) {
                        AuditLogger.logEvent(
                            context,
                            "API_ERROR",
                            "API request failed with code $statusCode: $responseBody"
                        )
                        val rotated = apiKeyManager.rotateToNextKey()
                        if (!rotated) {
                            return "Error: API request failed with status $statusCode: $responseBody"
                        }
                        attempt++
                        return@use // continue while loop
                    }

                    // Success! Return the response body
                    AuditLogger.logEvent(context, "API_SUCCESS", "API request succeeded with key index ${apiKeyManager.getCurrentKeyIndex()}")
                    return responseBody
                }

            } catch (e: IOException) {
                AuditLogger.logEvent(
                    context,
                    "API_EXCEPTION",
                    "Network exception with current key index ${apiKeyManager.getCurrentKeyIndex()}: ${e.message}"
                )
                val rotated = apiKeyManager.rotateToNextKey()
                if (!rotated) {
                    return "Error: Network failure and no more API keys available: ${e.message}"
                }
                attempt++
            }
        }

        return "Error: Maximum failover retry attempts reached."
    }

    fun getApiKeyManager(): ApiKeyManager = apiKeyManager
}
