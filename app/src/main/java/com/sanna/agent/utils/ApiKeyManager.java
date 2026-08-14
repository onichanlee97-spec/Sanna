package com.sanna.agent.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApiKeyManager {
    private static final String PREF_NAME = "SannaApiKeyPrefs";
    private static final String KEY_CURRENT_INDEX = "current_key_index";
    private static final String KEY_CUSTOM_KEYS = "custom_api_keys";

    private final SharedPreferences prefs;
    private List<String> apiKeys;

    public ApiKeyManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadKeys();
    }

    private void loadKeys() {
        String savedKeys = prefs.getString(KEY_CUSTOM_KEYS, "");
        if (savedKeys == null || savedKeys.trim().isEmpty()) {
            apiKeys = new ArrayList<>();
        } else {
            String[] split = savedKeys.split(",");
            apiKeys = new ArrayList<>();
            for (String key : split) {
                String trimmed = key.trim();
                if (!trimmed.isEmpty()) {
                    apiKeys.add(trimmed);
                }
            }
        }
    }

    public synchronized String getCurrentApiKey() {
        loadKeys();
        if (apiKeys.isEmpty()) {
            return "";
        }
        int index = prefs.getInt(KEY_CURRENT_INDEX, 0);
        if (index >= apiKeys.size() || index < 0) {
            index = 0;
            prefs.edit().putInt(KEY_CURRENT_INDEX, index).apply();
        }
        return apiKeys.get(index).trim();
    }

    public synchronized boolean rotateToNextKey() {
        loadKeys();
        if (apiKeys.isEmpty()) {
            return false;
        }
        int currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0);
        int nextIndex = currentIndex + 1;

        if (nextIndex >= apiKeys.size()) {
            // Reached the end of the key list
            return false;
        }

        prefs.edit().putInt(KEY_CURRENT_INDEX, nextIndex).apply();
        return true;
    }

    public synchronized void setApiKeys(String commaSeparatedKeys) {
        prefs.edit()
                .putString(KEY_CUSTOM_KEYS, commaSeparatedKeys)
                .putInt(KEY_CURRENT_INDEX, 0)
                .apply();
        loadKeys();
    }

    public synchronized String getSavedKeysString() {
        return prefs.getString(KEY_CUSTOM_KEYS, "");
    }

    public synchronized int getTotalKeysCount() {
        loadKeys();
        return apiKeys.size();
    }

    public synchronized int getCurrentKeyIndex() {
        return prefs.getInt(KEY_CURRENT_INDEX, 0);
    }
}
