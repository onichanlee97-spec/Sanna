package com.aistudio.futureagent.agxjyz.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val tools: List<Tool>? = null
)

data class Tool(
    @Json(name = "functionDeclarations")
    val functionDeclarations: List<FunctionDeclaration>
)

data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>? = null
)

data class Content(
    val role: String? = null,
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    @Json(name = "inline_data")
    val inlineData: InlineData? = null,
    @Json(name = "functionCall")
    val functionCall: FunctionCall? = null,
    @Json(name = "functionResponse")
    val functionResponse: FunctionResponse? = null
)

data class FunctionCall(
    val name: String,
    val args: Map<String, Any>? = null
)

data class FunctionResponse(
    val name: String,
    val response: Map<String, Any>
)

data class InlineData(
    @Json(name = "mime_type")
    val mimeType: String,
    val data: String
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

data class EmbeddingRequest(
    val content: Content
)

data class EmbeddingResponse(
    val embedding: EmbeddingValue
)

data class EmbeddingValue(
    val values: List<Float>
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") modelName: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/{model}:embedContent")
    suspend fun embedContent(
        @Path("model") modelName: String,
        @Query("key") apiKey: String,
        @Body request: EmbeddingRequest
    ): EmbeddingResponse
}

object RetrofitClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: GeminiApiService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

object GeminiFallbackExecutor {
    suspend fun generateWithFallback(
        context: android.content.Context,
        apiKey: String,
        request: GeminiRequest,
        onFallbackTriggered: ((fromModel: String, toModel: String) -> Unit)? = null
    ): GeminiResponse {
        val apiKeyManager = com.aistudio.futureagent.agxjyz.utils.ApiKeyManager(context)
        val hasMultiKeys = apiKeyManager.getTotalKeysCount() > 0
        var effectiveApiKey = if (hasMultiKeys) apiKeyManager.getCurrentApiKey() else apiKey
        if (effectiveApiKey.isBlank()) {
            effectiveApiKey = apiKey
        }

        val models = SecureStorage.AVAILABLE_MODELS
        val currentModel = SecureStorage.getSelectedModel(context)
        val sortedModels = listOf(currentModel) + models.filter { it != currentModel }

        val totalKeyAttempts = if (hasMultiKeys) apiKeyManager.getTotalKeysCount() else 1
        var keyAttempt = 0
        var lastException: Exception? = null

        while (keyAttempt < totalKeyAttempts) {
            for (i in sortedModels.indices) {
                val model = sortedModels[i]
                try {
                    val response = RetrofitClient.api.generateContent(model, effectiveApiKey, request)
                    if (model != currentModel) {
                        SecureStorage.saveSelectedModel(context, model)
                    }
                    return response
                } catch (e: Exception) {
                    lastException = e
                    val isQuotaOrRateLimit = isQuotaException(e)
                    if (isQuotaOrRateLimit) {
                        val nextModel = sortedModels.getOrNull(i + 1)
                        if (nextModel != null) {
                            onFallbackTriggered?.invoke(model, nextModel)
                            SecureStorage.saveSelectedModel(context, nextModel)
                        }
                    } else {
                        throw e
                    }
                }
            }

            // If all models failed on the current key, attempt key rotation if multi-keys configured
            if (hasMultiKeys && apiKeyManager.rotateToNextKey()) {
                effectiveApiKey = apiKeyManager.getCurrentApiKey()
                keyAttempt++
            } else {
                break
            }
        }

        throw lastException ?: RuntimeException("All configured Gemini models and API keys encountered quota or rate limits.")
    }

    fun isQuotaException(e: Exception): Boolean {
        if (e is retrofit2.HttpException) {
            val code = e.code()
            if (code == 429 || code == 403 || code == 503) return true
        }
        val msg = e.message ?: ""
        val lower = msg.lowercase()
        return lower.contains("429") ||
               lower.contains("403") ||
               lower.contains("503") ||
               lower.contains("quota") ||
               lower.contains("exceeded") ||
               lower.contains("resource_exhausted") ||
               lower.contains("rate") ||
               lower.contains("limit") ||
               lower.contains("overloaded") ||
               lower.contains("unavailable")
    }
}
