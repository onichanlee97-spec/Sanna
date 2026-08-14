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
        private const val KEY_PROVIDER_MODELS_PREFIX = "prov_models_"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    enum class Provider(val displayName: String) {
        GOOGLE_GEMINI("Gemini"),
        META_LLAMA("Meta (Llama)"),
        OPENAI("OpenAI"),
        ANTHROPIC("Anthropic"),
        GROQ("Groq"),
        MISTRAL("Mistral"),
        PERPLEXITY("Perplexity"),
        UNKNOWN("Custom LLM")
    }

    data class DiscoveryResult(
        val totalModels: Int,
        val providerCount: Int,
        val providerBreakdown: Map<String, List<String>>,
        val message: String
    )

    fun identifyKeyProvider(apiKey: String?): Provider {
        if (apiKey.isNullOrBlank()) {
            return Provider.UNKNOWN
        }
        val trimmed = apiKey.trim()
        return when {
            trimmed.startsWith("AIza") -> Provider.GOOGLE_GEMINI
            trimmed.startsWith("meta_") || trimmed.startsWith("llama_") || trimmed.startsWith("MLA_") ||
                trimmed.startsWith("EAAB") || trimmed.startsWith("LA-") ||
                trimmed.contains("meta", ignoreCase = true) || trimmed.contains("llama", ignoreCase = true) -> Provider.META_LLAMA
            trimmed.startsWith("sk-ant-") -> Provider.ANTHROPIC
            trimmed.startsWith("sk-proj-") || (trimmed.startsWith("sk-") && !trimmed.startsWith("sk-ant-")) -> Provider.OPENAI
            trimmed.startsWith("gsk_") -> Provider.GROQ
            trimmed.startsWith("mistral-") -> Provider.MISTRAL
            trimmed.startsWith("pplx-") -> Provider.PERPLEXITY
            else -> Provider.UNKNOWN
        }
    }

    /**
     * Automatically queries and imports all models available across all supplied API keys.
     */
    fun discoverAllModels(apiKeys: List<String>): DiscoveryResult {
        val breakdown = mutableMapOf<String, MutableList<String>>()
        val allDiscovered = mutableListOf<String>()

        val uniqueKeys = apiKeys.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        if (uniqueKeys.isEmpty()) {
            // Default built-in fallback
            val defaultMeta = getMetaLlamaCatalog()
            breakdown["Meta (Llama)"] = defaultMeta.toMutableList()
            allDiscovered.addAll(defaultMeta)

            val defaultGemini = listOf(
                "gemini-3.7-flash",
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview",
                "gemini-3.1-flash-lite-preview",
                "gemini-flash-latest",
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-2.5-flash-image",
                "gemini-3.1-flash-image-preview"
            )
            breakdown["Gemini"] = defaultGemini.toMutableList()
            allDiscovered.addAll(defaultGemini)

            saveDiscoveredModels(allDiscovered)
            return DiscoveryResult(
                totalModels = allDiscovered.size,
                providerCount = breakdown.size,
                providerBreakdown = breakdown,
                message = "Loaded catalog with ${allDiscovered.size} default models."
            )
        }

        for (key in uniqueKeys) {
            val provider = identifyKeyProvider(key)
            val models = when (provider) {
                Provider.GOOGLE_GEMINI -> fetchGeminiModelList(key)
                Provider.META_LLAMA -> fetchMetaModelList(key)
                Provider.OPENAI -> fetchOpenAIModelList(key)
                Provider.ANTHROPIC -> fetchAnthropicModelList(key)
                Provider.GROQ -> fetchGroqModelList(key)
                Provider.MISTRAL -> fetchMistralModelList(key)
                Provider.PERPLEXITY -> fetchPerplexityModelList(key)
                Provider.UNKNOWN -> listOf("custom-model-1", "custom-model-2")
            }

            val list = breakdown.getOrPut(provider.displayName) { mutableListOf() }
            for (m in models) {
                if (!list.contains(m)) list.add(m)
                if (!allDiscovered.contains(m)) allDiscovered.add(m)
            }
        }

        saveDiscoveredModels(allDiscovered)
        for ((prov, list) in breakdown) {
            prefs.edit().putString(KEY_PROVIDER_MODELS_PREFIX + prov, list.joinToString(",")).apply()
        }

        AuditLogger.logEvent(
            context,
            "ALL_MODELS_AUTO_IMPORTED",
            "Discovered ${allDiscovered.size} models across ${breakdown.size} providers."
        )

        return DiscoveryResult(
            totalModels = allDiscovered.size,
            providerCount = breakdown.size,
            providerBreakdown = breakdown,
            message = "Successfully imported ${allDiscovered.size} models from ${breakdown.size} provider(s)."
        )
    }

    fun discoverAndImportModels(apiKey: String): String {
        val result = discoverAllModels(listOf(apiKey))
        return result.message
    }

    private fun getMetaLlamaCatalog(): List<String> {
        return listOf(
            "llama-3.3-70b-instruct",
            "llama-3.1-405b-instruct",
            "llama-3.1-70b-instruct",
            "llama-3.1-8b-instruct",
            "llama-3.2-11b-vision-instruct",
            "llama-3.2-3b-instruct",
            "llama-3.2-1b-instruct",
            "llama-guard-3-8b",
            "meta-llama/Llama-3.3-70B-Instruct"
        )
    }

    private fun fetchMetaModelList(apiKey: String): List<String> {
        val models = mutableListOf<String>()
        // Attempt dynamic query if Meta / Llama API endpoint is active
        try {
            val url = "https://api.llama.com/v1/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val id = data.getJSONObject(i).optString("id")
                            if (id.isNotBlank()) models.add(id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback to comprehensive Meta catalog
        }

        if (models.isEmpty()) {
            models.addAll(getMetaLlamaCatalog())
        }
        return models
    }

    private fun fetchGroqModelList(apiKey: String): List<String> {
        val models = mutableListOf<String>()
        try {
            val url = "https://api.groq.com/openai/v1/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val id = data.getJSONObject(i).optString("id")
                            if (id.isNotBlank() && !id.contains("whisper") && !id.contains("tts")) {
                                models.add(id)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        if (models.isEmpty()) {
            models.addAll(listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-70b-specdec",
                "llama-3.1-8b-instant",
                "llama-3.2-11b-vision-preview",
                "llama-3.2-3b-preview",
                "llama-3.2-1b-preview",
                "deepseek-r1-distill-llama-70b",
                "mixtral-8x7b-32768",
                "gemma2-9b-it"
            ))
        }
        return models
    }

    private fun fetchGeminiModelList(apiKey: String): List<String> {
        val models = mutableListOf<String>()
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val modelsArray = json.optJSONArray("models")
                    if (modelsArray != null) {
                        for (i in 0 until modelsArray.length()) {
                            val modelObj = modelsArray.getJSONObject(i)
                            val methods = modelObj.optJSONArray("supportedGenerationMethods")
                            val isGenerateContent = methods?.let { arr ->
                                var has = false
                                for (j in 0 until arr.length()) {
                                    if (arr.getString(j) == "generateContent") has = true
                                }
                                has
                            } ?: true

                            if (isGenerateContent) {
                                var name = modelObj.optString("name")
                                if (name.startsWith("models/")) {
                                    name = name.substring(7)
                                }
                                if (name.isNotBlank()) {
                                    models.add(name)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        if (models.isEmpty()) {
            models.addAll(listOf(
                "gemini-3.7-flash",
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview",
                "gemini-3.1-flash-lite-preview",
                "gemini-flash-latest",
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-2.5-flash-image",
                "gemini-3.1-flash-image-preview"
            ))
        }
        return models
    }

    private fun fetchOpenAIModelList(apiKey: String): List<String> {
        val models = mutableListOf<String>()
        try {
            val url = "https://api.openai.com/v1/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val id = data.getJSONObject(i).optString("id")
                            if (id.startsWith("gpt-") || id.startsWith("o1") || id.startsWith("o3") || id.startsWith("chatgpt")) {
                                models.add(id)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        if (models.isEmpty()) {
            models.addAll(listOf(
                "gpt-4o",
                "gpt-4o-mini",
                "o1-preview",
                "o1-mini",
                "o3-mini",
                "gpt-4-turbo",
                "gpt-3.5-turbo"
            ))
        }
        return models
    }

    private fun fetchAnthropicModelList(apiKey: String): List<String> {
        return listOf(
            "claude-3-7-sonnet-20250219",
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229",
            "claude-3-haiku-20240307"
        )
    }

    private fun fetchMistralModelList(apiKey: String): List<String> {
        val models = mutableListOf<String>()
        try {
            val url = "https://api.mistral.ai/v1/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val id = data.getJSONObject(i).optString("id")
                            if (id.isNotBlank()) models.add(id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        if (models.isEmpty()) {
            models.addAll(listOf("mistral-large-latest", "mistral-small-latest", "codestral-latest", "pixtral-large-latest"))
        }
        return models
    }

    private fun fetchPerplexityModelList(apiKey: String): List<String> {
        return listOf(
            "sonar-pro",
            "sonar",
            "sonar-reasoning",
            "sonar-reasoning-pro"
        )
    }

    private fun saveDiscoveredModels(models: List<String>) {
        val joined = models.distinct().joinToString(",")
        prefs.edit().putString(KEY_DISCOVERED_MODELS, joined).apply()
    }

    fun getDiscoveredModels(): List<String> {
        val saved = prefs.getString(KEY_DISCOVERED_MODELS, "") ?: ""
        if (saved.isBlank()) {
            return emptyList()
        }
        return saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    fun getDiscoveredModelsByProvider(): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()
        val allDiscovered = getDiscoveredModels()
        if (allDiscovered.isEmpty()) return emptyMap()

        for (p in Provider.values()) {
            val key = KEY_PROVIDER_MODELS_PREFIX + p.displayName
            val saved = prefs.getString(key, "") ?: ""
            if (saved.isNotBlank()) {
                val list = saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (list.isNotEmpty()) {
                    map[p.displayName] = list
                }
            }
        }

        // If no provider specific breakdown saved yet, categorize dynamically
        if (map.isEmpty()) {
            val metaList = allDiscovered.filter { it.contains("llama") || it.contains("meta") }
            if (metaList.isNotEmpty()) map["Meta (Llama)"] = metaList

            val geminiList = allDiscovered.filter { it.contains("gemini") }
            if (geminiList.isNotEmpty()) map["Gemini"] = geminiList

            val openaiList = allDiscovered.filter { it.startsWith("gpt-") || it.startsWith("o1") || it.startsWith("o3") }
            if (openaiList.isNotEmpty()) map["OpenAI"] = openaiList

            val claudeList = allDiscovered.filter { it.contains("claude") }
            if (claudeList.isNotEmpty()) map["Anthropic"] = claudeList

            val groqList = allDiscovered.filter { it.contains("groq") || it.contains("mixtral") || it.contains("gemma") }
            if (groqList.isNotEmpty()) map["Groq"] = groqList
        }

        return map
    }

    fun getDetectedProvider(): String {
        return prefs.getString(KEY_DETECTED_PROVIDER, "Auto-Detect") ?: "Auto-Detect"
    }
}
