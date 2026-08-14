package com.sanna.agent.agent;

import android.content.Context;
import com.aistudio.futureagent.agxjyz.security.AuditLogger;
import com.sanna.agent.utils.ApiKeyManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClientWrapper {
    private final ApiKeyManager apiKeyManager;
    private final Context context;
    private final OkHttpClient httpClient;

    public ApiClientWrapper(Context context) {
        this.context = context;
        this.apiKeyManager = new ApiKeyManager(context);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public String executeAiRequestWithFailover(String urlEndpoint, String jsonPayload) {
        int maxAttempts = Math.max(1, apiKeyManager.getTotalKeysCount());
        int attempt = 0;

        if (apiKeyManager.getTotalKeysCount() == 0) {
            AuditLogger.logEvent(context, "API_ERROR", "No API keys configured in ApiKeyManager.");
            return "Error: No API keys configured. Please add keys in settings.";
        }

        while (attempt < maxAttempts) {
            String currentKey = apiKeyManager.getCurrentApiKey();
            if (currentKey == null || currentKey.trim().isEmpty()) {
                boolean rotated = apiKeyManager.rotateToNextKey();
                if (!rotated) break;
                attempt++;
                continue;
            }

            try {
                String effectiveUrl = urlEndpoint;
                if (urlEndpoint.contains("generativelanguage.googleapis.com") && !urlEndpoint.contains("key=")) {
                    String separator = urlEndpoint.contains("?") ? "&" : "?";
                    effectiveUrl = urlEndpoint + separator + "key=" + currentKey;
                }

                RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(effectiveUrl)
                        .addHeader("Authorization", "Bearer " + currentKey)
                        .addHeader("x-goog-api-key", currentKey)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    int statusCode = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    // Check for Rate Limit (429) or Quota Exceeded (403 / specific error bodies)
                    boolean isQuotaExceeded = (statusCode == 429) ||
                            (statusCode == 403 && responseBody.toLowerCase().contains("quota")) ||
                            (responseBody.toLowerCase().contains("resource_exhausted")) ||
                            (responseBody.toLowerCase().contains("rate_limit_exceeded"));

                    if (isQuotaExceeded) {
                        AuditLogger.logEvent(context, "API_FAILOVER", "API Key index " + apiKeyManager.getCurrentKeyIndex() + " exhausted quota (Code: " + statusCode + "). Rotating...");

                        boolean rotated = apiKeyManager.rotateToNextKey();
                        if (!rotated) {
                            AuditLogger.logEvent(context, "API_FATAL", "All configured API keys have exhausted their quotas.");
                            return "Error: All API keys have exhausted their quotas (HTTP " + statusCode + ": " + responseBody + ")";
                        }
                        attempt++;
                        continue;
                    }

                    if (!response.isSuccessful()) {
                        AuditLogger.logEvent(context, "API_ERROR", "API request failed with code " + statusCode + ": " + responseBody);
                        boolean rotated = apiKeyManager.rotateToNextKey();
                        if (!rotated) {
                            return "Error: API request failed with status " + statusCode + ": " + responseBody;
                        }
                        attempt++;
                        continue;
                    }

                    // Success! Return the response body
                    AuditLogger.logEvent(context, "API_SUCCESS", "API request succeeded with key index " + apiKeyManager.getCurrentKeyIndex());
                    return responseBody;
                }

            } catch (IOException e) {
                AuditLogger.logEvent(context, "API_EXCEPTION", "Network exception with current key index " + apiKeyManager.getCurrentKeyIndex() + ": " + e.getMessage());
                boolean rotated = apiKeyManager.rotateToNextKey();
                if (!rotated) {
                    return "Error: Network failure and no more API keys available: " + e.getMessage();
                }
                attempt++;
            }
        }

        return "Error: Maximum failover retry attempts reached.";
    }

    public ApiKeyManager getApiKeyManager() {
        return apiKeyManager;
    }
}
