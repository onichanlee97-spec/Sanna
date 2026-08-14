package com.sanna.agent.agent;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.aistudio.futureagent.agxjyz.security.AuditLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class ApiModelDiscovery {
    private static final String PREF_NAME = "SannaDiscoveredModelsPrefs";
    private static final String KEY_DISCOVERED_MODELS = "discovered_models";
    private static final String KEY_DETECTED_PROVIDER = "detected_provider";

    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient httpClient;

    public ApiModelDiscovery(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.httpClient = new OkHttpClient();
    }

    public enum Provider {
        GOOGLE_GEMINI,
        OPENAI,
        ANTHROPIC,
        UNKNOWN
    }

    public Provider identifyKeyProvider(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Provider.UNKNOWN;
        }
        String trimmed = apiKey.trim();
        if (trimmed.startsWith("AIza")) {
            return Provider.GOOGLE_GEMINI;
        } else if (trimmed.startsWith("sk-ant-")) {
            return Provider.ANTHROPIC;
        } else if (trimmed.startsWith("sk-")) {
            return Provider.OPENAI;
        }
        return Provider.UNKNOWN;
    }

    public String discoverAndImportModels(String apiKey) {
        Provider provider = identifyKeyProvider(apiKey);
        prefs.edit().putString(KEY_DETECTED_PROVIDER, provider.name()).apply();

        switch (provider) {
            case GOOGLE_GEMINI:
                return fetchGeminiModels(apiKey);
            case OPENAI:
                return fetchOpenAIModels(apiKey);
            case ANTHROPIC:
                return fetchAnthropicModels(apiKey);
            default:
                return "Error: Unrecognized API key format.";
        }
    }

    private String fetchGeminiModels(String apiKey) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Failed to query Gemini models: HTTP " + response.code();
            }
            String body = response.body() != null ? response.body().string() : "";
            JSONObject json = new JSONObject(body);
            JSONArray modelsArray = json.optJSONArray("models");

            List<String> modelNames = new ArrayList<>();
            if (modelsArray != null) {
                for (int i = 0; i < modelsArray.length(); i++) {
                    JSONObject modelObj = modelsArray.getJSONObject(i);
                    String name = modelObj.optString("name");
                    if (name.startsWith("models/")) {
                        name = name.substring(7);
                    }
                    modelNames.add(name);
                }
            }

            saveDiscoveredModels(modelNames);
            AuditLogger.logEvent(context, "MODEL_IMPORT", "Successfully imported " + modelNames.size() + " Gemini models.");
            return "Success: Imported " + modelNames.size() + " Gemini models.";

        } catch (Exception e) {
            AuditLogger.logEvent(context, "MODEL_IMPORT_ERROR", e.getMessage() != null ? e.getMessage() : "Unknown error");
            return "Error parsing Gemini models: " + e.getMessage();
        }
    }

    private String fetchOpenAIModels(String apiKey) {
        String url = "https://api.openai.com/v1/models";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Failed to query OpenAI models: HTTP " + response.code();
            }
            String body = response.body() != null ? response.body().string() : "";
            JSONObject json = new JSONObject(body);
            JSONArray dataArray = json.optJSONArray("data");

            List<String> modelNames = new ArrayList<>();
            if (dataArray != null) {
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject modelObj = dataArray.getJSONObject(i);
                    String id = modelObj.optString("id");
                    if (id.contains("gpt")) {
                        modelNames.add(id);
                    }
                }
            }

            saveDiscoveredModels(modelNames);
            AuditLogger.logEvent(context, "MODEL_IMPORT", "Successfully imported " + modelNames.size() + " OpenAI models.");
            return "Success: Imported " + modelNames.size() + " OpenAI models.";

        } catch (Exception e) {
            AuditLogger.logEvent(context, "MODEL_IMPORT_ERROR", e.getMessage() != null ? e.getMessage() : "Unknown error");
            return "Error parsing OpenAI models: " + e.getMessage();
        }
    }

    private String fetchAnthropicModels(String apiKey) {
        List<String> modelNames = new ArrayList<>();
        modelNames.add("claude-3-5-sonnet-20241022");
        modelNames.add("claude-3-opus-20240229");
        modelNames.add("claude-3-haiku-20240307");
        saveDiscoveredModels(modelNames);
        AuditLogger.logEvent(context, "MODEL_IMPORT", "Loaded Anthropic model defaults.");
        return "Success: Loaded Anthropic model defaults.";
    }

    private void saveDiscoveredModels(List<String> models) {
        String joined = TextUtils.join(",", models);
        prefs.edit().putString(KEY_DISCOVERED_MODELS, joined).apply();
    }

    public List<String> getDiscoveredModels() {
        String saved = prefs.getString(KEY_DISCOVERED_MODELS, "");
        if (saved.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(saved.split(",")));
    }

    public String getDetectedProvider() {
        return prefs.getString(KEY_DETECTED_PROVIDER, "UNKNOWN");
    }
}
