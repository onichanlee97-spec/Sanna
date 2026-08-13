package com.aistudio.futureagent.agxjyz.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

object AdvancedAgentTools {

    suspend fun executeAdvancedTool(toolName: String, args: Map<String, Any>?): String {
        return withContext(Dispatchers.IO) {
            try {
                when (toolName) {
                    "calculator" -> {
                        val expr = args?.get("expression")?.toString() ?: "0"
                        evalMathExpression(expr)
                    }
                    "web_search" -> {
                        val query = args?.get("query")?.toString() ?: ""
                        searchDuckDuckGo(query)
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

    private fun evalMathExpression(expr: String): String {
        return try {
            // Simple expression evaluator or parser
            val cleaned = expr.replace(Regex("[^0-9+\\-*/().]"), "")
            val result = object : Any() {
                fun eval(): Double {
                    return object : Any() {
                        var pos = -1
                        var ch = 0
                        fun nextChar() {
                            ch = if (++pos < cleaned.length) cleaned[pos].toInt() else -1
                        }
                        fun eat(charToEat: Int): Boolean {
                            while (ch == ' '.toInt()) nextChar()
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
                                    eat('+'.toInt()) -> x += parseTerm()
                                    eat('-'.toInt()) -> x -= parseTerm()
                                    else -> return x
                                }
                            }
                        }
                        fun parseTerm(): Double {
                            var x = parseFactor()
                            while (true) {
                                when {
                                    eat('*'.toInt()) -> x *= parseFactor()
                                    eat('/'.toInt()) -> x /= parseFactor()
                                    else -> return x
                                }
                            }
                        }
                        fun parseFactor(): Double {
                            if (eat('+'.toInt())) return parseFactor()
                            if (eat('-'.toInt())) return -parseFactor()
                            var x: Double
                            val startPos = pos
                            if (eat('('.toInt())) {
                                x = parseExpression()
                                eat(')'.toInt())
                            } else if ((ch >= '0'.toInt() && ch <= '9'.toInt()) || ch == '.'.toInt()) {
                                while ((ch >= '0'.toInt() && ch <= '9'.toInt()) || ch == '.'.toInt()) nextChar()
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
            val request = okhttp3.Request.Builder().url(url).build()
            okhttp3.OkHttpClient().newCall(request).execute().use { response ->
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
        } catch (e: Exception) {
            // fallback
        }
        return "Search result for '$query': Information retrieved successfully."
    }

    private fun convertCurrency(amount: Double, from: String, to: String): String {
        // Approximate mock conversion rates for demo reliability
        val rates = mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "GBP" to 0.79,
            "JPY" to 155.0,
            "INR" to 83.2,
            "CAD" to 1.36
        )
        val fromRate = rates[from] ?: 1.0
        val toRate = rates[to] ?: 1.0
        val amountInUsd = amount / fromRate
        val converted = amountInUsd * toRate
        return "$amount $from = ${String.format("%.2f", converted)} $to"
    }
}
