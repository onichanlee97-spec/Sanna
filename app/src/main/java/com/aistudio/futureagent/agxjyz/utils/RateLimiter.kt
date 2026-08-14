package com.aistudio.futureagent.agxjyz.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RateLimiter(private val maxTokens: Double, private val refillRatePerMs: Double) {
    private var availableTokens: Double = maxTokens
    private var lastRefillTimestamp: Long = System.currentTimeMillis()
    private val mutex = Mutex()

    suspend fun acquire(tokens: Double = 1.0): Boolean {
        mutex.withLock {
            refill()
            return if (availableTokens >= tokens) {
                availableTokens -= tokens
                true
            } else {
                false
            }
        }
    }

    private fun refill() {
        val now = System.currentTimeMillis()
        val elapsedTime = now - lastRefillTimestamp
        val tokensToAdd = elapsedTime * refillRatePerMs
        availableTokens = (availableTokens + tokensToAdd).coerceAtMost(maxTokens)
        lastRefillTimestamp = now
    }
}

object NetworkRateLimiter {
    // Standard limit: 5 requests per 10 seconds (0.0005 tokens per ms)
    private val limiter = RateLimiter(5.0, 0.0005)

    suspend fun acquire() = limiter.acquire()
}
