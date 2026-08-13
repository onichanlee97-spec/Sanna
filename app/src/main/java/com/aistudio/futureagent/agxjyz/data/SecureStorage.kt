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
        "gemini-3.6-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.5-flash",
        "gemini-3.1-pro-preview",
        "gemini-3.1-flash-lite",
        "gemini-3-flash-preview",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-1.5-flash",
        "gemini-1.5-pro"
    )

    fun getModelDisplayName(modelId: String): String {
        return when (modelId) {
            "gemini-3.6-flash" -> "Gemini 3.6 Flash"
            "gemini-3.5-flash-lite" -> "Gemini 3.5 Flash Lite"
            "gemini-3.5-flash" -> "Gemini 3.5 Flash"
            "gemini-3.1-pro-preview" -> "Gemini 3.1 Pro Preview"
            "gemini-3.1-flash-lite" -> "Gemini 3.1 Flash Lite"
            "gemini-3-flash-preview" -> "Gemini 3 Flash Preview"
            "gemini-2.5-flash" -> "Gemini 2.5 Flash"
            "gemini-2.5-pro" -> "Gemini 2.5 Pro"
            "gemini-1.5-flash" -> "Gemini 1.5 Flash"
            "gemini-1.5-pro" -> "Gemini 1.5 Pro"
            else -> modelId.replace("-", " ").split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
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

    fun saveSelectedModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_MODEL, model).apply()
    }

    fun getSelectedModel(context: Context): String {
        return getPrefs(context).getString(KEY_SELECTED_MODEL, "gemini-3.6-flash") ?: "gemini-3.6-flash"
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
        getPrefs(context).edit().putString(KEY_API_KEY, key).apply()
    }

    fun getApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_API_KEY, "") ?: ""
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
