package com.aistudio.futureagent.agxjyz.data

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CodeSandbox {
    suspend fun executeJavaScript(script: String): String {
        return withContext(Dispatchers.IO) {
            var rhino: Context? = null
            try {
                rhino = Context.enter()
                rhino.optimizationLevel = -1 // Without bytecode generation for Android compatibility
                val scope: Scriptable = rhino.initStandardObjects()
                val result = rhino.evaluateString(scope, script, "AgentSandbox", 1, null)
                "Execution Result: ${Context.toString(result)}"
            } catch (e: org.mozilla.javascript.RhinoException) {
                val line = e.lineNumber()
                val col = e.columnNumber()
                val details = e.details()
                val source = e.lineSource() ?: "Unknown source line"
                "Sandbox Runtime Error: $details\n" +
                "Location: Line $line, Column $col\n" +
                "Source: '$source'\n" +
                "🛠️ Self-Healing Suggestion: Double check spelling, declare variables with 'let' or 'const', or add matching brackets/braces."
            } catch (e: Exception) {
                "Sandbox Runtime Error: ${e.localizedMessage}"
            } finally {
                try {
                    Context.exit()
                } catch (ignored: Exception) {}
            }
        }
    }

    suspend fun executeJavaScriptWithSelfHealing(script: String): String {
        val initialResult = executeJavaScript(script)
        if (initialResult.contains("Sandbox Runtime Error:")) {
            var patchedScript = script
            var patchApplied = ""
            
            if (initialResult.contains("is not defined", ignoreCase = true)) {
                val regex = "\"([^\"]+)\" is not defined".toRegex()
                val match = regex.find(initialResult)
                if (match != null) {
                    val varName = match.groupValues[1]
                    patchedScript = "var $varName = 0;\n$script"
                    patchApplied = "Automatically declared undefined variable '$varName'"
                }
            } else if (initialResult.contains("missing ) after formal parameters", ignoreCase = true)) {
                patchedScript = script.replace("function(", "function()").replace("fn(", "fn()")
                patchApplied = "Fixed missing parentheses in function declaration"
            } else if (initialResult.contains("missing }", ignoreCase = true) || initialResult.contains("compound statement", ignoreCase = true)) {
                patchedScript = "$script\n}"
                patchApplied = "Appended missing closing brace '}' to balance compound block"
            } else if (initialResult.contains("missing ;", ignoreCase = true)) {
                patchedScript = script.replace("\n", ";\n")
                patchApplied = "Inserted missing semicolons at line terminations"
            }
            
            if (patchApplied.isNotEmpty()) {
                val retryResult = executeJavaScript(patchedScript)
                if (!retryResult.contains("Sandbox Runtime Error:")) {
                    return "**[Sanna Self-Healing Engine Success]**\n" +
                           "• **Initial Error**: Caught sandbox crash during script execution.\n" +
                           "• **Diagnostics Applied**: $patchApplied\n" +
                           "• **Patched Code**:\n```javascript\n$patchedScript\n```\n" +
                           "• **Healing Verification**: SUCCESS\n" +
                           "• **Execution Output**: $retryResult"
                } else {
                    return "**[Sanna Self-Healing Engine Attempted]**\n" +
                           "• **Initial Error**: Caught sandbox crash.\n" +
                           "• **Diagnostics Applied**: $patchApplied\n" +
                           "• **Secondary Error**: $retryResult\n" +
                           "⚠️ Self-healing recursion failed due to deeper logical errors."
                }
            }
        }
        return initialResult
    }
}
