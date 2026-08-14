package com.aistudio.futureagent.agxjyz.utils

import android.content.Context
import com.aistudio.futureagent.agxjyz.security.AuditLogger
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

object ScriptSynthesizer {

    fun saveAndCompileScript(context: Context, fileName: String, scriptContent: String): Boolean {
        return try {
            val scriptsDir = File(context.filesDir, "custom_tools")
            if (!scriptsDir.exists()) {
                scriptsDir.mkdirs()
            }

            val scriptFile = File(scriptsDir, fileName)
            FileOutputStream(scriptFile).use { fos ->
                fos.write(scriptContent.toByteArray(StandardCharsets.UTF_8))
            }

            AuditLogger.logEvent(context, "TOOL_SYNTHESIS", "Successfully synthesized custom tool: $fileName")
            true
        } catch (e: Exception) {
            AuditLogger.logEvent(context, "TOOL_SYNTHESIS_ERROR", e.message ?: "Unknown synthesis error")
            false
        }
    }

    fun listSynthesizedScripts(context: Context): List<File> {
        val scriptsDir = File(context.filesDir, "custom_tools")
        if (!scriptsDir.exists()) return emptyList()
        return scriptsDir.listFiles()?.filter { it.isFile } ?: emptyList()
    }

    fun readScript(scriptFile: File): String {
        return try {
            scriptFile.readText(StandardCharsets.UTF_8)
        } catch (e: Exception) {
            "// Failed to read script: ${e.message}"
        }
    }

    fun deleteScript(context: Context, fileName: String): Boolean {
        return try {
            val scriptFile = File(File(context.filesDir, "custom_tools"), fileName)
            val deleted = scriptFile.delete()
            if (deleted) {
                AuditLogger.logEvent(context, "TOOL_DELETION", "Deleted custom tool: $fileName")
            }
            deleted
        } catch (e: Exception) {
            false
        }
    }
}
