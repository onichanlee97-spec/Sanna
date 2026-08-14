package com.aistudio.futureagent.agxjyz.utils

import android.content.Context
import android.content.SharedPreferences

class ApiKeyManager(context: Context) {

    companion object {
        private const val PREF_NAME = "SannaApiKeyPrefs"
        private const val KEY_CURRENT_INDEX = "current_key_index"
        private const val KEY_CUSTOM_KEYS = "custom_api_keys"
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
            apiKeys = mutableListOf()
        } else {
            apiKeys = savedKeys.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableList()
        }
    }

    @Synchronized
    fun getCurrentApiKey(): String {
        loadKeys()
        if (apiKeys.isEmpty()) {
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
    fun rotateToNextKey(): Boolean {
        loadKeys()
        if (apiKeys.isEmpty()) {
            return false
        }
        val currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)
        val nextIndex = currentIndex + 1

        if (nextIndex >= apiKeys.size) {
            // Reached the end of the key list
            return false
        }

        prefs.edit().putInt(KEY_CURRENT_INDEX, nextIndex).apply()
        return true
    }

    @Synchronized
    fun setApiKeys(commaSeparatedKeys: String) {
        prefs.edit()
            .putString(KEY_CUSTOM_KEYS, commaSeparatedKeys)
            .putInt(KEY_CURRENT_INDEX, 0)
            .apply()
        loadKeys()
    }

    @Synchronized
    fun getSavedKeysString(): String {
        return prefs.getString(KEY_CUSTOM_KEYS, "") ?: ""
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
    fun resetIndex() {
        prefs.edit().putInt(KEY_CURRENT_INDEX, 0).apply()
    }
}
