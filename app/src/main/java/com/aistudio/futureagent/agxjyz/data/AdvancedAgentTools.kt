package com.aistudio.futureagent.agxjyz.data

import android.content.Context
import com.aistudio.futureagent.agxjyz.BuildConfig
import com.aistudio.futureagent.agxjyz.data.room.AgentRepository
import com.aistudio.futureagent.agxjyz.data.room.ApprovalEntity
import com.aistudio.futureagent.agxjyz.data.room.VectorEntity
import com.aistudio.futureagent.agxjyz.utils.NetworkRateLimiter
import com.aistudio.futureagent.agxjyz.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.sqrt

object AdvancedAgentTools {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun executeAdvancedTool(
        context: Context,
        repository: AgentRepository,
        apiKey: String,
        toolName: String,
        args: Map<String, Any>?
    ): String {
        // Enforce rate limiting for all network-bound tools
        if (toolName in listOf("web_search", "vision_analysis", "vector_store_add", "vector_store_query")) {
            if (!NetworkRateLimiter.acquire()) {
                return "Rate limit exceeded. Please wait a moment before trying again."
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                when (toolName) {
                    "calculator" -> {
                        val expr = args?.get("expression")?.toString() ?: "0"
                        evalMathExpression(expr)
                    }
                    "web_search" -> {
                        val query = args?.get("query")?.toString() ?: ""
                        val maxResults = args?.get("maxResults")?.toString()?.toIntOrNull() ?: 5
                        executeWebSearch(query, maxResults)
                    }
                    "vision_analysis" -> {
                        val imageBase64 = args?.get("imageBase64")?.toString() ?: ""
                        val prompt = args?.get("prompt")?.toString() ?: "Describe this image."
                        executeVisionAnalysis(context, apiKey, imageBase64, prompt)
                    }
                    "vector_store_add" -> {
                        val text = args?.get("text")?.toString() ?: ""
                        val metadata = args?.get("metadata")?.toString() ?: "{}"
                        val ttlHours = args?.get("ttlHours")?.toString()?.toLongOrNull() ?: 24L
                        vectorStoreAdd(repository, apiKey, text, metadata, ttlHours)
                    }
                    "vector_store_query" -> {
                        val query = args?.get("query")?.toString() ?: ""
                        val topK = args?.get("topK")?.toString()?.toIntOrNull() ?: 3
                        vectorStoreQuery(repository, apiKey, query, topK)
                    }
                    "shell_execution" -> {
                        val command = args?.get("command")?.toString() ?: ""
                        executeShellCommand(command)
                    }
                    "currency_converter" -> {
                        val amount = args?.get("amount")?.toString()?.toDoubleOrNull() ?: 1.0
                        val from = args?.get("from")?.toString()?.uppercase() ?: "USD"
                        val to = args?.get("to")?.toString()?.uppercase() ?: "EUR"
                        convertCurrency(amount, from, to)
                    }
                    else -> "Unknown tool function."
                }
            } catch (e: Exception) {
                "Tool execution error: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun executeWebSearch(query: String, maxResults: Int): String {
        val tavilyKey = try { BuildConfig.TAVILY_API_KEY } catch (e: Exception) { "" }
        if (tavilyKey.isBlank() || tavilyKey == "TAVILY_API_KEY") {
            return searchDuckDuckGo(query)
        }

        var attempt = 0
        val maxRetries = 3
        var lastError = ""

        while (attempt < maxRetries) {
            try {
                val json = JSONObject().apply {
                    put("query", query)
                    put("max_results", maxResults)
                    put("include_answer", true)
                }

                val request = Request.Builder()
                    .url("https://api.tavily.com/search")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $tavilyKey")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val data = JSONObject(body)
                        val answer = data.optString("answer")
                        val results = data.optJSONArray("results")
                        
                        val sb = StringBuilder()
                        if (answer.isNotBlank() && answer != "null") sb.append("Answer: $answer\n\n")
                        if (results != null) {
                            for (i in 0 until results.length()) {
                                val item = results.getJSONObject(i)
                                sb.append("${i + 1}. ${item.getString("title")}\n")
                                sb.append("   URL: ${item.getString("url")}\n")
                                sb.append("   Snippet: ${item.getString("content")}\n\n")
                            }
                        }
                        return sb.toString()
                    } else if (response.code == 429 || response.code >= 500) {
                        lastError = "HTTP ${response.code}: ${response.message}"
                    } else {
                        return "Web search failed: HTTP ${response.code} ${response.message}"
                    }
                }

                if (lastError.isNotBlank()) {
                    attempt++
                    val waitTime = (2.0.pow(attempt.toDouble()) * 1000).toLong()
                    delay(waitTime)
                }
            } catch (e: Exception) {
                attempt++
                if (attempt >= maxRetries) return "Web search failed after $maxRetries retries: ${e.localizedMessage}"
                val waitTime = (2.0.pow(attempt.toDouble()) * 1000).toLong()
                delay(waitTime)
                lastError = e.localizedMessage ?: "Unknown error"
            }
        }
        return "Web search failed after $maxRetries retries: $lastError"
    }

    private suspend fun executeVisionAnalysis(context: Context, apiKey: String, imageBase64: String, prompt: String): String {
        var tempFile: File? = null
        try {
            if (imageBase64.isNotBlank()) {
                // Cache image in private cache dir for processing safety (Scoped Storage)
                val data = android.util.Base64.decode(imageBase64.substringAfter(","), android.util.Base64.DEFAULT)
                tempFile = File(context.cacheDir, "vision_cache_${System.currentTimeMillis()}.jpg")
                tempFile.writeBytes(data)
            }

            val request = GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64.substringAfter(",")))
                    ))
                )
            )
            val response = RetrofitClient.api.generateContent("gemini-1.5-flash", apiKey, request)
            return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No vision analysis result."
        } finally {
            // Atomic cleanup of temporary cache files
            try {
                tempFile?.delete()
            } catch (e: Exception) {}
        }
    }

    private suspend fun vectorStoreAdd(repository: AgentRepository, apiKey: String, text: String, metadata: String, ttlHours: Long): String {
        // Atomic Pruning of expired entries before addition
        repository.pruneExpired(System.currentTimeMillis())

        val embedding = getEmbedding(apiKey, text)
        val expiresAt = System.currentTimeMillis() + (ttlHours * 60 * 60 * 1000)
        val vector = VectorEntity(content = text, metadata = metadata, embedding = embedding, expiresAt = expiresAt)
        repository.insertVector(vector)
        return "Document successfully indexed into semantic vector store (TTL: ${ttlHours}h)."
    }

    private suspend fun vectorStoreQuery(repository: AgentRepository, apiKey: String, query: String, topK: Int): String {
        // Ensure we only query non-expired vectors
        repository.pruneExpired(System.currentTimeMillis())

        val queryEmbedding = getEmbedding(apiKey, query)
        val allVectors = repository.getAllVectors()
        
        val scored = allVectors.map {
            it to cosineSimilarity(queryEmbedding, it.embedding)
        }.sortedByDescending { it.second }.take(topK)

        if (scored.isEmpty()) return "No matches found in vector store."

        val sb = StringBuilder("Top Semantic Matches:\n")
        scored.forEachIndexed { index, (vector, score) ->
            sb.append("${index + 1}. Content: ${vector.content}\n")
            sb.append("   Score: ${String.format("%.4f", score)}\n")
            sb.append("   Metadata: ${vector.metadata}\n\n")
        }
        return sb.toString()
    }

    private suspend fun getEmbedding(apiKey: String, text: String): List<Float> {
        val request = EmbeddingRequest(content = Content(parts = listOf(Part(text = text))))
        val response = RetrofitClient.api.embedContent("gemini-embedding-2-preview", apiKey, request)
        return response.embedding.values
    }

    private fun cosineSimilarity(vecA: List<Float>, vecB: List<Float>): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in vecA.indices) {
            dotProduct += vecA[i] * vecB[i]
            normA += vecA[i] * vecA[i]
            normB += vecB[i] * vecB[i]
        }
        if (normA == 0.0 || normB == 0.0) return 0.0
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }

    private fun executeShellCommand(command: String): String {
        // Security Guardrail: Block destructive patterns
        val dangerousPatterns = listOf("rm -rf", "mkfs", "> /dev/sd", "dd if=", "chmod 777", "chown", "shutdown", "reboot")
        for (pattern in dangerousPatterns) {
            if (command.contains(pattern)) {
                return "Security Guardrail: Command blocked due to destructive matching patterns ('$pattern')."
            }
        }

        // Production Android: Prefer restricted toybox paths for reliability in sandbox
        val shellCommand = if (File("/system/bin/toybox").exists()) {
            "/system/bin/toybox $command"
        } else {
            command
        }

        return try {
            val process = Runtime.getRuntime().exec(shellCommand)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                errorOutput.append(line).append("\n")
            }
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                "Shell Output:\n$output"
            } else {
                "Shell Command Failed (Exit Code $exitCode):\n$errorOutput"
            }
        } catch (e: Exception) {
            "Shell execution error: ${e.localizedMessage}"
        }
    }

    private fun evalMathExpression(expr: String): String {
        return try {
            val cleaned = expr.replace(Regex("[^0-9+\\-*/().]"), "")
            val result = object : Any() {
                fun eval(): Double {
                    return object : Any() {
                        var pos = -1
                        var ch = 0
                        fun nextChar() {
                            ch = if (++pos < cleaned.length) cleaned[pos].code else -1
                        }
                        fun eat(charToEat: Int): Boolean {
                            while (ch == ' '.code) nextChar()
                            if (ch == charToEat) {
                                nextChar()
                                return true
                            }
                            return false
                        }
                        fun parse(): Double {
                            nextChar()
                            val x = parseExpression()
                            if (pos < cleaned.length) throw RuntimeException("Unexpected: " + ch.toChar())
                            return x
                        }
                        fun parseExpression(): Double {
                            var x = parseTerm()
                            while (true) {
                                when {
                                    eat('+'.code) -> x += parseTerm()
                                    eat('-'.code) -> x -= parseTerm()
                                    else -> return x
                                }
                            }
                        }
                        fun parseTerm(): Double {
                            var x = parseFactor()
                            while (true) {
                                when {
                                    eat('*'.code) -> x *= parseFactor()
                                    eat('/'.code) -> x /= parseFactor()
                                    else -> return x
                                }
                            }
                        }
                        fun parseFactor(): Double {
                            if (eat('+'.code)) return parseFactor()
                            if (eat('-'.code)) return -parseFactor()
                            var x: Double
                            val startPos = pos
                            if (eat('('.code)) {
                                x = parseExpression()
                                eat(')'.code)
                            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                                x = cleaned.substring(startPos, pos).toDouble()
                            } else {
                                throw RuntimeException("Unexpected: " + ch.toChar())
                            }
                            return x
                        }
                    }.parse()
                }
            }.eval()
            "Result of $expr = $result"
        } catch (e: Exception) {
            "Could not evaluate math expression '$expr': ${e.localizedMessage}"
        }
    }

    private fun searchDuckDuckGo(query: String): String {
        try {
            val url = "https://api.duckduckgo.com/?q=${URLEncoder.encode(query, "UTF-8")}&format=json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val abstractText = json.optString("AbstractText")
                    if (abstractText.isNotEmpty()) {
                        return "DuckDuckGo Summary: $abstractText"
                    }
                    val related = json.optJSONArray("RelatedTopics")
                    if (related != null && related.length() > 0) {
                        val first = related.optJSONObject(0)
                        if (first != null && first.has("Text")) {
                            return "DuckDuckGo Result: ${first.getString("Text")}"
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        return "Search result for '$query': No summary available, but query processed."
    }

    private fun convertCurrency(amount: Double, from: String, to: String): String {
        val rates = mapOf("USD" to 1.0, "EUR" to 0.92, "GBP" to 0.79, "JPY" to 155.0, "INR" to 83.2, "CAD" to 1.36)
        val fromRate = rates[from] ?: 1.0
        val toRate = rates[to] ?: 1.0
        val amountInUsd = amount / fromRate
        val converted = amountInUsd * toRate
        return "$amount $from = ${String.format("%.2f", converted)} $to"
    }

    // HITL Persistence Helper
    suspend fun createApproval(context: Context, repository: AgentRepository, actionName: String, payload: String, riskLevel: String): String {
        val approvalId = UUID.randomUUID().toString().substring(0, 8)
        val approval = ApprovalEntity(
            id = approvalId,
            actionName = actionName,
            payload = payload,
            riskLevel = riskLevel,
            status = "PENDING"
        )
        repository.insertApproval(approval)
        
        // Push heads-up notification for Human-In-The-Loop authorization
        NotificationHelper.postApprovalNotification(context, approvalId, actionName, riskLevel)
        
        return approvalId
    }
}
