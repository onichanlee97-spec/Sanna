package com.aistudio.futureagent.agxjyz.ui.tools

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aistudio.futureagent.agxjyz.ui.components.BlueprintGrid
import com.aistudio.futureagent.agxjyz.ui.components.HudFrame
import com.aistudio.futureagent.agxjyz.ui.theme.BlueprintTheme
import com.aistudio.futureagent.agxjyz.ui.theme.NeonCyan
import com.aistudio.futureagent.agxjyz.utils.ApiKeyManager
import com.aistudio.futureagent.agxjyz.utils.ScriptSynthesizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ToolSynthesizerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlueprintTheme {
                ToolSynthesizerContent(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolSynthesizerContent(onBack: () -> Unit = {}) {
    var toolName by remember { mutableStateOf("calculator_tool.js") }
    var scriptBody by remember {
        mutableStateOf(
            "// Dynamic Synthesized Agent Tool\nfunction calculateTax(amount, rate) {\n    return amount * (1 + rate / 100);\n}\ncalculateTax(250, 8.5);"
        )
    }
    var statusMessage by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    var synthesizedList by remember { mutableStateOf(ScriptSynthesizer.listSynthesizedScripts(context)) }

    val apiKeyManager = remember { ApiKeyManager(context) }
    val modelDiscovery = remember { com.aistudio.futureagent.agxjyz.agent.ApiModelDiscovery(context) }
    var apiKeysInput by remember { mutableStateOf(apiKeyManager.getSavedKeysString()) }
    var keyStatusMessage by remember {
        val currentProvider = modelDiscovery.getDetectedProvider()
        val existingModels = modelDiscovery.getDiscoveredModels()
        if (existingModels.isNotEmpty()) {
            mutableStateOf("Provider: $currentProvider | Models: ${existingModels.size} imported (${existingModels.take(3).joinToString(", ")}...)")
        } else {
            mutableStateOf("Status: No API key analyzed yet.")
        }
    }
    var totalLoadedKeys by remember { mutableIntStateOf(apiKeyManager.getTotalKeysCount()) }
    var activeKeyIndex by remember { mutableIntStateOf(apiKeyManager.getCurrentKeyIndex()) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dynamic Tool Synthesizer", color = NeonCyan) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF07131D))
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            BlueprintGrid()
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HudFrame(Modifier.fillMaxWidth(), "SYNTHESIZER_INPUT") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = toolName,
                            onValueChange = { toolName = it },
                            label = { Text("Tool Script File Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        OutlinedTextField(
                            value = scriptBody,
                            onValueChange = { scriptBody = it },
                            label = { Text("JavaScript Execution Payload") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        Button(
                            onClick = {
                                val success = ScriptSynthesizer.saveAndCompileScript(context, toolName, scriptBody)
                                statusMessage = if (success) {
                                    synthesizedList = ScriptSynthesizer.listSynthesizedScripts(context)
                                    "Tool '$toolName' compiled and synthesized successfully!"
                                } else {
                                    "Failed to synthesize tool."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("SAVE & COMPILE DYNAMIC TOOL", color = Color.Black)
                        }

                        if (statusMessage.isNotEmpty()) {
                            Text(
                                statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }
                }

                HudFrame(Modifier.fillMaxWidth(), "API_FAILOVER_AND_MODEL_DISCOVERY") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "API Key Failover & Model Auto-Discovery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Enter comma-separated API keys. Sanna auto-identifies provider prefixes (AIza for Gemini, sk- for OpenAI, sk-ant- for Anthropic) and discovers models dynamically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAAAAA)
                        )

                        OutlinedTextField(
                            value = apiKeysInput,
                            onValueChange = { apiKeysInput = it },
                            label = { Text("API Keys (Comma-Separated)") },
                            placeholder = { Text("AIzaSyKey1..., AIzaSyKey2..., sk-..., sk-ant-...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    apiKeyManager.setApiKeys(apiKeysInput)
                                    totalLoadedKeys = apiKeyManager.getTotalKeysCount()
                                    activeKeyIndex = apiKeyManager.getCurrentKeyIndex()
                                    val currentKey = apiKeyManager.getCurrentApiKey()
                                    val provider = modelDiscovery.identifyKeyProvider(currentKey)
                                    keyStatusMessage = "API Keys saved ($totalLoadedKeys keys). Detected provider: $provider"
                                    Toast.makeText(context, "API Keys updated. Total keys: $totalLoadedKeys", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                            ) {
                                Text("SAVE KEYS", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val currentKey = apiKeyManager.getCurrentApiKey()
                                    if (currentKey.isBlank()) {
                                        Toast.makeText(context, "Please save an API key first.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    keyStatusMessage = "Identifying key and querying provider inventory..."
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        val resultMessage = modelDiscovery.discoverAndImportModels(currentKey)
                                        val importedList = modelDiscovery.getDiscoveredModels()
                                        val providerName = modelDiscovery.getDetectedProvider()
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            keyStatusMessage = "Provider: $providerName | $resultMessage"
                                            Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Text("AUTO-IMPORT MODELS", color = Color.Black)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Key: #${activeKeyIndex + 1} of $totalLoadedKeys",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (totalLoadedKeys > 0) NeonCyan else Color.Gray
                            )
                        }

                        if (keyStatusMessage.isNotEmpty()) {
                            Text(
                                keyStatusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }
                }

                Text("Synthesized Custom Tools", style = MaterialTheme.typography.titleMedium, color = Color.White)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(synthesizedList) { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2B3C))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, style = MaterialTheme.typography.bodyMedium, color = NeonCyan)
                                    Text("${file.length()} bytes", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    ScriptSynthesizer.deleteScript(context, file.name)
                                    synthesizedList = ScriptSynthesizer.listSynthesizedScripts(context)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
