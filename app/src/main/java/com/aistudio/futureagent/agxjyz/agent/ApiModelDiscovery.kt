package com.aistudio.futureagent.agxjyz.agent

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.futureagent.agxjyz.security.AuditLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiModelDiscovery(private val context: Context) {

    companion object {
        private const val PREF_NAME = "SannaDiscoveredModelsPrefs"
        private const val KEY_DISCOVERED_MODELS = "discovered_models"
        private const val KEY_DETECTED_PROVIDER = "detected_provider"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    enum class Provider {
        GOOGLE_GEMINI,
        OPENAI,
        ANTHROPIC,
        UNKNOWN
    }

    fun identifyKeyProvider(apiKey: String?): Provider {
        if (apiKey.isNullOrBlank()) {
            return Provider.UNKNOWN
        }
        val trimmed = apiKey.trim()
        return when {
            trimmed.startsWith("AIza") -> Provider.GOOGLE_GEMINI
            trimmed.startsWith("sk-ant-") -> Provider.ANTHROPIC
            trimmed.startsWith("sk-") -> Provider.OPENAI
            else -> Provider.UNKNOWN
        }
    }

    fun discoverAndImportModels(apiKey: String): String {
        val provider = identifyKeyProvider(apiKey)
        prefs.edit().putString(KEY_DETECTED_PROVIDER, provider.name).apply()

        return when (provider) {
            Provider.GOOGLE_GEMINI -> fetchGeminiModels(apiKey)
            Provider.OPENAI -> fetchOpenAIModels(apiKey)
            Provider.ANTHROPIC -> fetchAnthropicModels(apiKey)
            Provider.UNKNOWN -> "Error: Unrecognized API key format."
        }
    }

    private fun fetchGeminiModels(apiKey: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
        val request = Request.Builder().url(url).get().build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "Failed to query Gemini models: HTTP ${response.code}"
                }
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val modelsArray = json.optJSONArray("models")

                val modelNames = mutableListOf<String>()
                if (modelsArray != null) {
                    for (i in 0 until modelsArray.length()) {
                        val modelObj = modelsArray.getJSONObject(i)
                        var name = modelObj.optString("name")
                        if (name.startsWith("models/")) {
                            name = name.substring(7)
                        }
                        if (name.isNotBlank()) {
                            modelNames.add(name)
                        }
                    }
                }

                saveDiscoveredModels(modelNames)
                AuditLogger.logEvent(context, "MODEL_IMPORT", "Successfully imported ${modelNames.size} Gemini models.")
                "Success: Imported ${modelNames.size} Gemini models."
            }
        } catch (e: Exception) {
            AuditLogger.logEvent(context, "MODEL_IMPORT_ERROR", e.message ?: "Unknown error")
            "Error parsing Gemini models: ${e.message}"
        }
    }

    private fun fetchOpenAIModels(apiKey: String): String {
        val url = "https://api.openai.com/v1/models"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "Failed to query OpenAI models: HTTP ${response.code}"
                }
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val dataArray = json.optJSONArray("data")

                val modelNames = mutableListOf<String>()
                if (dataArray != null) {
                    for (i in 0 until dataArray.length()) {
                        val modelObj = dataArray.getJSONObject(i)
                        val id = modelObj.optString("id")
                        if (id.contains("gpt")) {
                            modelNames.add(id)
                        }
                    }
                }

                saveDiscoveredModels(modelNames)
                AuditLogger.logEvent(context, "MODEL_IMPORT", "Successfully imported ${modelNames.size} OpenAI models.")
                "Success: Imported ${modelNames.size} OpenAI models."
            }
        } catch (e: Exception) {
            AuditLogger.logEvent(context, "MODEL_IMPORT_ERROR", e.message ?: "Unknown error")
            "Error parsing OpenAI models: ${e.message}"
        }
    }

    private fun fetchAnthropicModels(apiKey: String): String {
        val modelNames = listOf(
            "claude-3-5-sonnet-20241022",
            "claude-3-opus-20240229",
            "claude-3-haiku-20240307"
        )
        saveDiscoveredModels(modelNames)
        AuditLogger.logEvent(context, "MODEL_IMPORT", "Loaded Anthropic model defaults.")
        return "Success: Loaded Anthropic model defaults."
    }

    private fun saveDiscoveredModels(models: List<String>) {
        val joined = models.joinToString(",")
        prefs.edit().putString(KEY_DISCOVERED_MODELS, joined).apply()
    }

    fun getDiscoveredModels(): List<String> {
        val saved = prefs.getString(KEY_DISCOVERED_MODELS, "") ?: ""
        if (saved.isBlank()) {
            return emptyList()
        }
        return saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getDetectedProvider(): String {
        return prefs.getString(KEY_DETECTED_PROVIDER, "UNKNOWN") ?: "UNKNOWN"
    }
}
