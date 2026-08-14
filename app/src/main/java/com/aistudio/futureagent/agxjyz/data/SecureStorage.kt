package com.aistudio.futureagent.agxjyz.data

import android.content.Context
import android.content.SharedPreferences

object SecureStorage {
    private const val PREF_NAME = "sanna_secure_prefs"
    private const val KEY_API_KEY = "gemini_api_key"
    private const val KEY_OAUTH_TOKEN = "oauth_token"
    private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
    private const val KEY_GOVERNANCE_ENABLED = "governance_enabled"
    private const val KEY_WEBHOOKS_JSON = "custom_webhooks_json"
    private const val KEY_SELECTED_MODEL = "selected_gemini_model"
    private const val KEY_PERSONA = "selected_agent_persona"
    private const val KEY_CUSTOM_PROMPT = "custom_system_prompt"

    val AVAILABLE_MODELS = listOf(
        "gemini-3.7-flash",
        "gemini-3.5-flash",
        "gemini-3.1-pro-preview",
        "gemini-3.1-flash-lite-preview",
        "gemini-flash-latest",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.5-flash-image",
        "gemini-3.1-flash-image-preview",
        "llama-3.3-70b-instruct",
        "llama-3.1-405b-instruct",
        "llama-3.1-70b-instruct",
        "llama-3.1-8b-instruct",
        "llama-3.2-11b-vision-instruct",
        "llama-3.2-3b-instruct",
        "llama-3.2-1b-instruct",
        "claude-3-7-sonnet-20250219",
        "claude-3-5-sonnet-20241022",
        "claude-3-5-haiku-20241022",
        "gpt-4o",
        "gpt-4o-mini",
        "o3-mini",
        "o1-preview",
        "llama-3.3-70b-versatile",
        "deepseek-r1-distill-llama-70b"
    )

    fun getModelProvider(modelId: String): String {
        val lower = modelId.lowercase()
        return when {
            lower.startsWith("gemini") || lower.startsWith("veo") -> "Gemini"
            lower.startsWith("claude") -> "Anthropic"
            lower.startsWith("gpt-") || lower.startsWith("o1") || lower.startsWith("o3") || lower.startsWith("chatgpt") -> "OpenAI"
            lower.contains("versatile") || lower.contains("instant") || lower.contains("specdec") || lower.contains("deepseek") || lower.contains("mixtral") || lower.contains("gemma2") -> "Groq"
            lower.startsWith("mistral") || lower.startsWith("codestral") -> "Mistral"
            lower.startsWith("sonar") -> "Perplexity"
            lower.contains("llama") || lower.contains("meta") -> "Meta (Llama)"
            else -> "Custom / LLM"
        }
    }

    fun getAvailableModels(context: Context): List<String> {
        val discovered = try {
            com.aistudio.futureagent.agxjyz.agent.ApiModelDiscovery(context).getDiscoveredModels()
        } catch (e: Exception) {
            emptyList()
        }
        return (AVAILABLE_MODELS + discovered).distinct()
    }

    fun getModelDisplayName(modelId: String): String {
        return when (modelId) {
            "gemini-3.7-flash" -> "Gemini 3.7 Flash"
            "gemini-3.5-flash" -> "Gemini 3.5 Flash"
            "gemini-3.1-pro-preview" -> "Gemini 3.1 Pro Preview"
            "gemini-3.1-flash-lite-preview" -> "Gemini 3.1 Flash Lite"
            "gemini-flash-latest" -> "Gemini Flash Latest"
            "gemini-2.5-flash" -> "Gemini 2.5 Flash"
            "gemini-2.5-pro" -> "Gemini 2.5 Pro"
            "gemini-2.5-flash-image" -> "Gemini 2.5 Flash Image"
            "gemini-3.1-flash-image-preview" -> "Gemini 3.1 Flash Image HD"
            "llama-3.3-70b-instruct" -> "Meta Llama 3.3 70B"
            "llama-3.1-405b-instruct" -> "Meta Llama 3.1 405B"
            "llama-3.1-70b-instruct" -> "Meta Llama 3.1 70B"
            "llama-3.1-8b-instruct" -> "Meta Llama 3.1 8B"
            "llama-3.2-11b-vision-instruct" -> "Meta Llama 3.2 11B Vision"
            "llama-3.2-3b-instruct" -> "Meta Llama 3.2 3B"
            "llama-3.2-1b-instruct" -> "Meta Llama 3.2 1B"
            "llama-3.3-70b-versatile" -> "Groq Llama 3.3 70B"
            "llama-3.1-70b-specdec" -> "Groq Llama 3.1 70B"
            "llama-3.1-8b-instant" -> "Groq Llama 3.1 8B"
            "deepseek-r1-distill-llama-70b" -> "Groq DeepSeek R1 70B"
            "mixtral-8x7b-32768" -> "Groq Mixtral 8x7B"
            "gpt-4o" -> "OpenAI GPT-4o"
            "gpt-4o-mini" -> "OpenAI GPT-4o Mini"
            "o1-preview" -> "OpenAI o1 Preview"
            "o1-mini" -> "OpenAI o1 Mini"
            "o3-mini" -> "OpenAI o3 Mini"
            "claude-3-7-sonnet-20250219" -> "Claude 3.7 Sonnet"
            "claude-3-5-sonnet-20241022" -> "Claude 3.5 Sonnet"
            "claude-3-5-haiku-20241022" -> "Claude 3.5 Haiku"
            "claude-3-opus-20240229" -> "Claude 3 Opus"
            "mistral-large-latest" -> "Mistral Large"
            "mistral-small-latest" -> "Mistral Small"
            "codestral-latest" -> "Mistral Codestral"
            "sonar-pro" -> "Perplexity Sonar Pro"
            "sonar" -> "Perplexity Sonar"
            else -> modelId.replace("-", " ").replace("_", " ").split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        }
    }

    val AVAILABLE_PERSONAS = listOf(
        "Sanna Cyberpunk",
        "Jarvis Executive",
        "Concise Engineer",
        "Custom Persona"
    )

    fun setGovernanceEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_GOVERNANCE_ENABLED, enabled).apply()
    }

    fun isGovernanceEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_GOVERNANCE_ENABLED, true)
    }

    fun saveWebhooksRaw(context: Context, json: String) {
        getPrefs(context).edit().putString(KEY_WEBHOOKS_JSON, json).apply()
    }

    fun getWebhooksRaw(context: Context): String {
        return getPrefs(context).getString(KEY_WEBHOOKS_JSON, "") ?: ""
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = androidx.security.crypto.MasterKey.Builder(context)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()
            androidx.security.crypto.EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun getStandardPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("sanna_standard_prefs", Context.MODE_PRIVATE)
    }

    fun saveSelectedModel(context: Context, model: String) {
        val validModel = if (model.isBlank() || model == "gemini-1.5-flash" || model == "gemini-1.5-pro" || model == "gemini-2.0-flash") {
            "gemini-3.7-flash"
        } else {
            model
        }
        getPrefs(context).edit().putString(KEY_SELECTED_MODEL, validModel).apply()
        getStandardPrefs(context).edit().putString(KEY_SELECTED_MODEL, validModel).apply()
    }

    fun getSelectedModel(context: Context): String {
        val model = getPrefs(context).getString(KEY_SELECTED_MODEL, null)
            ?: getStandardPrefs(context).getString(KEY_SELECTED_MODEL, "gemini-3.7-flash")
            ?: "gemini-3.7-flash"
        return if (model.isBlank() || model == "gemini-1.5-flash" || model == "gemini-1.5-pro" || model == "gemini-2.0-flash") {
            "gemini-3.7-flash"
        } else {
            model
        }
    }

    fun savePersona(context: Context, persona: String) {
        getPrefs(context).edit().putString(KEY_PERSONA, persona).apply()
    }

    fun getPersona(context: Context): String {
        return getPrefs(context).getString(KEY_PERSONA, "Sanna Cyberpunk") ?: "Sanna Cyberpunk"
    }

    fun saveCustomPrompt(context: Context, prompt: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_PROMPT, prompt).apply()
    }

    fun getCustomPrompt(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_PROMPT, "You are Sanna, an advanced open-source Android AI agent. Execute user objectives with maximum speed and precision.") ?: ""
    }

    fun saveApiKey(context: Context, key: String) {
        try {
            getPrefs(context).edit().putString(KEY_API_KEY, key.trim()).apply()
        } catch (e: Exception) {}
        getStandardPrefs(context).edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun getApiKey(context: Context): String {
        val encryptedKey = try {
            getPrefs(context).getString(KEY_API_KEY, "") ?: ""
        } catch (e: Exception) {
            ""
        }
        if (encryptedKey.isNotBlank() && encryptedKey != "MY_GEMINI_API_KEY") {
            return encryptedKey.trim()
        }
        val standardKey = getStandardPrefs(context).getString(KEY_API_KEY, "") ?: ""
        if (standardKey.isNotBlank() && standardKey != "MY_GEMINI_API_KEY") {
            return standardKey.trim()
        }
        return ""
    }

    fun saveOAuthToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_OAUTH_TOKEN, token).apply()
    }

    fun getOAuthToken(context: Context): String {
        return getPrefs(context).getString(KEY_OAUTH_TOKEN, "") ?: ""
    }

    fun setWakeWordEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
    }

    fun isWakeWordEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WAKE_WORD_ENABLED, true)
    }
}
