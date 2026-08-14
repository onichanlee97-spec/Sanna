package com.aistudio.futureagent.agxjyz.security

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AuditLogger {
    private const val LOG_FILE_NAME = "tamper_evident_audit.log"

    @JvmStatic
    @Synchronized
    fun logEvent(context: Context, eventType: String, details: String) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val logEntry = String.format(Locale.US, "[%s] EVENT: %s | DETAILS: %s\n", timestamp, eventType, details)

            val previousHash = if (file.exists() && file.length() > 0) {
                val existingBytes = file.readBytes()
                bytesToHex(MessageDigest.getInstance("SHA-256").digest(existingBytes))
            } else {
                "GENESIS_BLOCK_ROOT"
            }

            val signedEntry = logEntry + "PREV_HASH: " + previousHash + "\n----------------------------------------\n"

            FileOutputStream(file, true).use { fos ->
                fos.write(signedEntry.toByteArray(StandardCharsets.UTF_8))
            }
        } catch (ignored: Exception) {}
    }

    fun log(context: Context, action: String, details: String) = logEvent(context, action, details)

    fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuilder(2 * hash.size)
        for (b in hash) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        return hexString.toString()
    }
}

