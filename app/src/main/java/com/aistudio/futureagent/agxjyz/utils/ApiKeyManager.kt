package com.aistudio.futureagent.agxjyz.utils

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.futureagent.agxjyz.data.SecureStorage

class ApiKeyManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "SannaApiKeyPrefs"
        private const val KEY_CURRENT_INDEX = "current_key_index"
        private const val KEY_CUSTOM_KEYS = "custom_api_keys"

        fun getMaskedKey(key: String): String {
            if (key.length <= 8) return "••••••••"
            return "${key.take(6)}...${key.takeLast(4)}"
        }

        fun detectProvider(key: String): String {
            val trimmed = key.trim()
            return when {
                trimmed.startsWith("AIzaSy") || trimmed.startsWith("AIza") -> "Gemini"
                trimmed.startsWith("meta_") || trimmed.startsWith("llama_") || trimmed.startsWith("MLA_") ||
                    trimmed.startsWith("EAAB") || trimmed.startsWith("LA-") ||
                    trimmed.contains("meta", ignoreCase = true) || trimmed.contains("llama", ignoreCase = true) -> "Meta (Llama)"
                trimmed.startsWith("sk-ant-") -> "Anthropic"
                trimmed.startsWith("sk-proj-") || trimmed.startsWith("sk-") -> "OpenAI"
                trimmed.startsWith("gsk_") -> "Groq"
                trimmed.startsWith("pplx-") -> "Perplexity"
                trimmed.startsWith("mistral-") -> "Mistral"
                else -> "Custom / LLM"
            }
        }

        fun getEffectiveApiKey(context: Context): String {
            val manager = ApiKeyManager(context)
            val current = manager.getCurrentApiKey()
            if (current.isNotBlank() && current != "MY_GEMINI_API_KEY") {
                return current
            }
            val secure = SecureStorage.getApiKey(context)
            if (secure.isNotBlank() && secure != "MY_GEMINI_API_KEY") {
                return secure
            }
            val buildConfigKey = com.aistudio.futureagent.agxjyz.BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                return buildConfigKey
            }
            return ""
        }

        fun getEffectiveKeyForModel(context: Context, modelId: String): String {
            val manager = ApiKeyManager(context)
            val targetProvider = SecureStorage.getModelProvider(modelId)
            val providerKey = manager.getKeyForProvider(targetProvider)
            if (!providerKey.isNullOrBlank() && providerKey != "MY_GEMINI_API_KEY") {
                return providerKey
            }
            return getEffectiveApiKey(context)
        }

        fun getEffectiveKeys(context: Context): List<String> {
            val manager = ApiKeyManager(context)
            val list = manager.getAllKeys().filter { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }.toMutableList()
            val secure = SecureStorage.getApiKey(context)
            if (secure.isNotBlank() && secure != "MY_GEMINI_API_KEY" && !list.contains(secure)) {
                list.add(secure)
            }
            val buildConfigKey = com.aistudio.futureagent.agxjyz.BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY" && !list.contains(buildConfigKey)) {
                list.add(buildConfigKey)
            }
            return list
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private var apiKeys: MutableList<String> = mutableListOf()

    init {
        loadKeys()
    }

    @Synchronized
    private fun loadKeys() {
        val savedKeys = prefs.getString(KEY_CUSTOM_KEYS, "") ?: ""
        if (savedKeys.isBlank()) {
            val fallbackKey = SecureStorage.getApiKey(context)
            val buildConfigKey = com.aistudio.futureagent.agxjyz.BuildConfig.GEMINI_API_KEY
            if (fallbackKey.isNotBlank() && fallbackKey != "MY_GEMINI_API_KEY") {
                apiKeys = mutableListOf(fallbackKey)
                persistKeys()
            } else if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                apiKeys = mutableListOf(buildConfigKey)
                persistKeys()
            } else {
                apiKeys = mutableListOf()
            }
        } else {
            apiKeys = savedKeys.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableList()
        }
    }

    @Synchronized
    private fun persistKeys() {
        val joined = apiKeys.joinToString("\n")
        prefs.edit()
            .putString(KEY_CUSTOM_KEYS, joined)
            .apply()
        
        // Sync active key to SecureStorage
        val active = getCurrentApiKey()
        if (active.isNotBlank() && active != "MY_GEMINI_API_KEY") {
            SecureStorage.saveApiKey(context, active)
        }
    }

    @Synchronized
    fun getAllKeys(): List<String> {
        loadKeys()
        if (apiKeys.isEmpty()) {
            val fallback = getEffectiveApiKey(context)
            if (fallback.isNotBlank()) {
                apiKeys.add(fallback)
                persistKeys()
            }
        }
        return apiKeys.toList()
    }

    @Synchronized
    fun getKeysForProvider(provider: String): List<String> {
        val all = getAllKeys()
        return all.filter { detectProvider(it).equals(provider, ignoreCase = true) }
    }

    @Synchronized
    fun getKeyForProvider(provider: String): String? {
        val matching = getKeysForProvider(provider)
        if (matching.isNotEmpty()) return matching.first()
        // If requesting Gemini, check fallback
        if (provider.equals("Gemini", ignoreCase = true)) {
            val sec = SecureStorage.getApiKey(context)
            if (sec.isNotBlank() && sec != "MY_GEMINI_API_KEY") return sec
            val bc = com.aistudio.futureagent.agxjyz.BuildConfig.GEMINI_API_KEY
            if (bc.isNotBlank() && bc != "MY_GEMINI_API_KEY") return bc
        }
        return null
    }

    @Synchronized
    fun isProviderConfigured(provider: String): Boolean {
        return getKeyForProvider(provider) != null
    }

    @Synchronized
    fun getConfiguredProviders(): List<String> {
        val keys = getAllKeys()
        val providers = keys.map { detectProvider(it) }.toMutableList()
        val sec = SecureStorage.getApiKey(context)
        if (sec.isNotBlank() && sec != "MY_GEMINI_API_KEY") {
            providers.add(detectProvider(sec))
        }
        val bc = com.aistudio.futureagent.agxjyz.BuildConfig.GEMINI_API_KEY
        if (bc.isNotBlank() && bc != "MY_GEMINI_API_KEY") {
            providers.add("Gemini")
        }
        return providers.distinct()
    }

    @Synchronized
    fun getCurrentApiKey(): String {
        loadKeys()
        if (apiKeys.isEmpty()) {
            val sec = SecureStorage.getApiKey(context)
            if (sec.isNotBlank() && sec != "MY_GEMINI_API_KEY") return sec
            val bc = com.aistudio.futureagent.agxjyz.BuildConfig.GEMINI_API_KEY
            if (bc.isNotBlank() && bc != "MY_GEMINI_API_KEY") return bc
            return ""
        }
        var index = prefs.getInt(KEY_CURRENT_INDEX, 0)
        if (index >= apiKeys.size || index < 0) {
            index = 0
            prefs.edit().putInt(KEY_CURRENT_INDEX, index).apply()
        }
        return apiKeys[index]
    }

    @Synchronized
    fun setActiveKeyIndex(index: Int) {
        loadKeys()
        if (index in 0 until apiKeys.size) {
            prefs.edit().putInt(KEY_CURRENT_INDEX, index).apply()
            SecureStorage.saveApiKey(context, apiKeys[index])
        }
    }

    @Synchronized
    fun rotateToNextKey(): Boolean {
        loadKeys()
        if (apiKeys.isEmpty()) {
            return false
        }
        val currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)
        val nextIndex = currentIndex + 1

        if (nextIndex >= apiKeys.size) {
            return false
        }

        prefs.edit().putInt(KEY_CURRENT_INDEX, nextIndex).apply()
        SecureStorage.saveApiKey(context, apiKeys[nextIndex])
        return true
    }

    /**
     * Import raw input text that may contain one or multiple keys (separated by commas, newlines, semicolons, or spaces).
     * Returns the count of newly added keys.
     */
    @Synchronized
    fun importKeys(rawInput: String): Int {
        loadKeys()
        val parsed = rawInput.split(Regex("[,;\n\r\t ]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length >= 8 }

        var addedCount = 0
        for (k in parsed) {
            if (!apiKeys.contains(k)) {
                apiKeys.add(k)
                addedCount++
            }
        }

        if (addedCount > 0 || apiKeys.isNotEmpty()) {
            persistKeys()
        }
        return addedCount
    }

    @Synchronized
    fun addKey(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return false
        loadKeys()
        if (!apiKeys.contains(trimmed)) {
            apiKeys.add(trimmed)
            persistKeys()
            return true
        }
        return false
    }

    @Synchronized
    fun deleteKeyAt(index: Int): Boolean {
        loadKeys()
        if (index in 0 until apiKeys.size) {
            apiKeys.removeAt(index)
            var currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)
            if (currentIndex >= apiKeys.size) {
                currentIndex = (apiKeys.size - 1).coerceAtLeast(0)
                prefs.edit().putInt(KEY_CURRENT_INDEX, currentIndex).apply()
            }
            persistKeys()
            return true
        }
        return false
    }

    @Synchronized
    fun deleteKey(key: String): Boolean {
        loadKeys()
        val removed = apiKeys.remove(key.trim())
        if (removed) {
            var currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)
            if (currentIndex >= apiKeys.size) {
                currentIndex = (apiKeys.size - 1).coerceAtLeast(0)
                prefs.edit().putInt(KEY_CURRENT_INDEX, currentIndex).apply()
            }
            persistKeys()
        }
        return removed
    }

    @Synchronized
    fun clearAllKeys() {
        apiKeys.clear()
        prefs.edit()
            .putString(KEY_CUSTOM_KEYS, "")
            .putInt(KEY_CURRENT_INDEX, 0)
            .apply()
    }

    @Synchronized
    fun getTotalKeysCount(): Int {
        loadKeys()
        return apiKeys.size
    }

    @Synchronized
    fun getCurrentKeyIndex(): Int {
        return prefs.getInt(KEY_CURRENT_INDEX, 0)
    }

    @Synchronized
    fun getSavedKeysString(): String {
        loadKeys()
        return apiKeys.joinToString("\n")
    }

    @Synchronized
    fun setApiKeys(commaSeparatedKeys: String) {
        importKeys(commaSeparatedKeys)
    }
}
