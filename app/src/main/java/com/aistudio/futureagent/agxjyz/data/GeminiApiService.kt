package com.aistudio.futureagent.agxjyz.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
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
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateWithFallback(
        context: android.content.Context,
        apiKey: String,
        request: GeminiRequest,
        onFallbackTriggered: ((fromModelOrProvider: String, toModelOrProvider: String) -> Unit)? = null
    ): GeminiResponse {
        val apiKeyManager = com.aistudio.futureagent.agxjyz.utils.ApiKeyManager(context)
        val effectiveKeys = com.aistudio.futureagent.agxjyz.utils.ApiKeyManager.getEffectiveKeys(context)
        val allKeys = if (effectiveKeys.isNotEmpty()) {
            effectiveKeys
        } else if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            listOf(apiKey)
        } else {
            emptyList()
        }

        if (allKeys.isEmpty()) {
            throw RuntimeException("No valid API key configured. Please enter your API key in settings or AI Studio.")
        }

        val selectedModel = SecureStorage.getSelectedModel(context)
        val targetProvider = SecureStorage.getModelProvider(selectedModel)

        // Prioritize keys matching the selected model's provider first
        val matchingKeys = mutableListOf<Pair<Int, String>>()
        val otherKeys = mutableListOf<Pair<Int, String>>()

        for (i in allKeys.indices) {
            val key = allKeys[i]
            val prov = com.aistudio.futureagent.agxjyz.utils.ApiKeyManager.detectProvider(key)
            if (prov.equals(targetProvider, ignoreCase = true) || (targetProvider == "Gemini" && prov == "Gemini")) {
                matchingKeys.add(Pair(i, key))
            } else {
                otherKeys.add(Pair(i, key))
            }
        }

        val keysToTry = matchingKeys + otherKeys

        var lastException: Exception? = null

        for (keyPairIndex in keysToTry.indices) {
            val (keyIndex, currentKey) = keysToTry[keyPairIndex]
            val providerName = com.aistudio.futureagent.agxjyz.utils.ApiKeyManager.detectProvider(currentKey)

            // Select models for this provider
            val modelsForProvider = getModelsForProvider(providerName, selectedModel)

            for (modelIndex in modelsForProvider.indices) {
                val model = modelsForProvider[modelIndex]
                try {
                    val response = executeProviderCall(providerName, currentKey, model, request)
                    // If successful and we had to rotate keys/models, update active index & model
                    if (allKeys.size > 1 && apiKeyManager.getCurrentKeyIndex() != keyIndex) {
                        apiKeyManager.setActiveKeyIndex(keyIndex)
                    }
                    SecureStorage.saveSelectedModel(context, model)
                    return response
                } catch (e: Exception) {
                    lastException = e
                    val isQuota = isQuotaException(e)
                    
                    // Check if there is a next model for this provider to failover to
                    val nextModel = modelsForProvider.getOrNull(modelIndex + 1)
                    if (nextModel != null) {
                        val reason = if (isQuota) "Quota auto-fallback" else "Model failover (${e.message?.take(25)})"
                        onFallbackTriggered?.invoke(
                            "$providerName ($model)",
                            "$providerName ($nextModel) [$reason]"
                        )
                        // Continue to try next model with same key
                        continue
                    } else {
                        // Provider exhausted, falling back to next provider key
                        val nextKeyPair = keysToTry.getOrNull(keyPairIndex + 1)
                        if (nextKeyPair != null) {
                            val nextProvider = com.aistudio.futureagent.agxjyz.utils.ApiKeyManager.detectProvider(nextKeyPair.second)
                            val reason = if (isQuota) "Quota limit reached" else "Error (${e.message?.take(25)})"
                            onFallbackTriggered?.invoke(
                                "$providerName Key #${keyIndex + 1} ($reason)",
                                "Next Active LLM Provider: $nextProvider (Key #${nextKeyPair.first + 1})"
                            )
                        }
                    }
                }
            }
        }

        throw lastException ?: RuntimeException("All configured LLM providers and API keys failed to return a response.")
    }

    private fun getModelsForProvider(provider: String, currentSelected: String): List<String> {
        return when (provider) {
            "Meta (Llama)" -> {
                val list = listOf(
                    "llama-3.3-70b-instruct",
                    "llama-3.1-405b-instruct",
                    "llama-3.1-70b-instruct",
                    "llama-3.1-8b-instruct",
                    "llama-3.2-11b-vision-instruct",
                    "llama-3.2-3b-instruct",
                    "llama-3.2-1b-instruct",
                    "meta-llama/Llama-3.3-70B-Instruct"
                )
                if (list.contains(currentSelected)) listOf(currentSelected) + list.filter { it != currentSelected } else list
            }
            "Anthropic" -> {
                val list = listOf(
                    "claude-3-7-sonnet-20250219",
                    "claude-3-5-sonnet-20241022",
                    "claude-3-5-haiku-20241022",
                    "claude-3-opus-20240229"
                )
                if (list.contains(currentSelected)) listOf(currentSelected) + list.filter { it != currentSelected } else list
            }
            "OpenAI" -> {
                val list = listOf("gpt-4o", "gpt-4o-mini", "o3-mini", "o1-preview", "o1-mini")
                if (list.contains(currentSelected)) listOf(currentSelected) + list.filter { it != currentSelected } else list
            }
            "Groq" -> {
                val list = listOf(
                    "llama-3.3-70b-versatile",
                    "deepseek-r1-distill-llama-70b",
                    "llama-3.1-8b-instant",
                    "mixtral-8x7b-32768"
                )
                if (list.contains(currentSelected)) listOf(currentSelected) + list.filter { it != currentSelected } else list
            }
            "Mistral" -> {
                val list = listOf("mistral-large-latest", "mistral-small-latest", "codestral-latest")
                if (list.contains(currentSelected)) listOf(currentSelected) + list.filter { it != currentSelected } else list
            }
            "Perplexity" -> {
                val list = listOf("sonar-pro", "sonar")
                if (list.contains(currentSelected)) listOf(currentSelected) + list.filter { it != currentSelected } else list
            }
            else -> {
                // Gemini & default
                val standardGemini = listOf(
                    "gemini-2.5-flash",
                    "gemini-3.7-flash",
                    "gemini-3.5-flash",
                    "gemini-3.1-pro-preview",
                    "gemini-3.1-flash-lite-preview",
                    "gemini-flash-latest",
                    "gemini-2.5-pro",
                    "gemini-2.5-flash-image"
                )
                if (standardGemini.contains(currentSelected)) {
                    listOf(currentSelected) + standardGemini.filter { it != currentSelected }
                } else {
                    standardGemini
                }
            }
        }
    }

    private suspend fun executeProviderCall(
        provider: String,
        apiKey: String,
        model: String,
        request: GeminiRequest
    ): GeminiResponse {
        return when (provider) {
            "Gemini" -> {
                RetrofitClient.api.generateContent(model, apiKey, request)
            }
            "Meta (Llama)", "OpenAI", "Groq", "Perplexity", "Mistral", "Custom / LLM" -> {
                executeOpenAiCompatibleCall(provider, apiKey, model, request)
            }
            "Anthropic" -> {
                executeAnthropicCall(apiKey, model, request)
            }
            else -> {
                RetrofitClient.api.generateContent(model, apiKey, request)
            }
        }
    }

    private fun executeOpenAiCompatibleCall(
        provider: String,
        apiKey: String,
        model: String,
        request: GeminiRequest
    ): GeminiResponse {
        val endpointUrl = when (provider) {
            "Meta (Llama)" -> "https://api.llama.com/v1/chat/completions"
            "Groq" -> "https://api.groq.com/openai/v1/chat/completions"
            "Perplexity" -> "https://api.perplexity.ai/chat/completions"
            "Mistral" -> "https://api.mistral.ai/v1/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }

        val jsonPayload = org.json.JSONObject()
        jsonPayload.put("model", model)

        val messagesArray = org.json.JSONArray()
        if (request.systemInstruction != null) {
            val sysText = request.systemInstruction.parts.mapNotNull { it.text }.joinToString("\n")
            if (sysText.isNotBlank()) {
                val sysObj = org.json.JSONObject()
                sysObj.put("role", "system")
                sysObj.put("content", sysText)
                messagesArray.put(sysObj)
            }
        }

        for (content in request.contents) {
            val msgObj = org.json.JSONObject()
            msgObj.put("role", if (content.role == "model" || content.role == "assistant") "assistant" else "user")
            val textContent = content.parts.mapNotNull { it.text }.joinToString("\n")
            msgObj.put("content", textContent)
            messagesArray.put(msgObj)
        }
        jsonPayload.put("messages", messagesArray)

        // Tools / functions if present
        if (!request.tools.isNullOrEmpty()) {
            val toolsArray = org.json.JSONArray()
            for (tool in request.tools) {
                for (fn in tool.functionDeclarations) {
                    val toolObj = org.json.JSONObject()
                    toolObj.put("type", "function")
                    val fnObj = org.json.JSONObject()
                    fnObj.put("name", fn.name)
                    fnObj.put("description", fn.description)
                    if (fn.parameters != null) {
                        fnObj.put("parameters", org.json.JSONObject(fn.parameters))
                    }
                    toolObj.put("function", fnObj)
                    toolsArray.put(toolObj)
                }
            }
            if (toolsArray.length() > 0) {
                jsonPayload.put("tools", toolsArray)
            }
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = jsonPayload.toString().toRequestBody(mediaType)
        val httpRequest = okhttp3.Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val httpResponse = httpClient.newCall(httpRequest).execute()
        val responseBodyStr = httpResponse.body?.string() ?: ""

        if (!httpResponse.isSuccessful) {
            val code = httpResponse.code
            throw RuntimeException("HTTP $code from $provider ($endpointUrl): $responseBodyStr")
        }

        val resJson = org.json.JSONObject(responseBodyStr)
        val choices = resJson.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val firstChoice = choices.getJSONObject(0)
            val msg = firstChoice.optJSONObject("message")
            if (msg != null) {
                val toolCalls = msg.optJSONArray("tool_calls")
                if (toolCalls != null && toolCalls.length() > 0) {
                    val tc = toolCalls.getJSONObject(0)
                    val fn = tc.optJSONObject("function")
                    val name = fn?.optString("name") ?: "tool"
                    val argsStr = fn?.optString("arguments") ?: "{}"
                    val argsMap = mutableMapOf<String, Any>()
                    try {
                        val argsJson = org.json.JSONObject(argsStr)
                        val keys = argsJson.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            argsMap[k] = argsJson.get(k)
                        }
                    } catch (e: Exception) {
                        // ignore args parse error
                    }
                    return GeminiResponse(
                        candidates = listOf(
                            Candidate(
                                content = Content(
                                    role = "model",
                                    parts = listOf(
                                        Part(functionCall = FunctionCall(name = name, args = argsMap))
                                    )
                                )
                            )
                        )
                    )
                }

                val contentText = msg.optString("content", "")
                return GeminiResponse(
                    candidates = listOf(
                        Candidate(
                            content = Content(
                                role = "model",
                                parts = listOf(Part(text = contentText))
                            )
                        )
                    )
                )
            }
        }

        return GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        role = "model",
                        parts = listOf(Part(text = "Completed response from $provider"))
                    )
                )
            )
        )
    }

    private fun executeAnthropicCall(
        apiKey: String,
        model: String,
        request: GeminiRequest
    ): GeminiResponse {
        val endpointUrl = "https://api.anthropic.com/v1/messages"
        val jsonPayload = org.json.JSONObject()
        jsonPayload.put("model", model)
        jsonPayload.put("max_tokens", 4096)

        if (request.systemInstruction != null) {
            val sysText = request.systemInstruction.parts.mapNotNull { it.text }.joinToString("\n")
            if (sysText.isNotBlank()) {
                jsonPayload.put("system", sysText)
            }
        }

        val messagesArray = org.json.JSONArray()
        for (content in request.contents) {
            val msgObj = org.json.JSONObject()
            msgObj.put("role", if (content.role == "model" || content.role == "assistant") "assistant" else "user")
            val textContent = content.parts.mapNotNull { it.text }.joinToString("\n")
            msgObj.put("content", textContent)
            messagesArray.put(msgObj)
        }
        jsonPayload.put("messages", messagesArray)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = jsonPayload.toString().toRequestBody(mediaType)
        val httpRequest = okhttp3.Request.Builder()
            .url(endpointUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val httpResponse = httpClient.newCall(httpRequest).execute()
        val responseBodyStr = httpResponse.body?.string() ?: ""

        if (!httpResponse.isSuccessful) {
            val code = httpResponse.code
            throw RuntimeException("HTTP $code from Anthropic: $responseBodyStr")
        }

        val resJson = org.json.JSONObject(responseBodyStr)
        val contentArray = resJson.optJSONArray("content")
        val textBuilder = StringBuilder()
        if (contentArray != null) {
            for (i in 0 until contentArray.length()) {
                val block = contentArray.getJSONObject(i)
                if (block.optString("type") == "text") {
                    textBuilder.append(block.optString("text"))
                }
            }
        }

        return GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        role = "model",
                        parts = listOf(Part(text = textBuilder.toString()))
                    )
                )
            )
        )
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
               lower.contains("unavailable") ||
               lower.contains("capacity") ||
               lower.contains("credits") ||
               lower.contains("billing")
    }
}
