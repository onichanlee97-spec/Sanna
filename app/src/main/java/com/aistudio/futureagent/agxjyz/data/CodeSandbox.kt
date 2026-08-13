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
            } catch (e: Exception) {
                "Sandbox Runtime Error: ${e.localizedMessage}"
            } finally {
                try {
                    Context.exit()
                } catch (ignored: Exception) {}
            }
        }
    }
}
