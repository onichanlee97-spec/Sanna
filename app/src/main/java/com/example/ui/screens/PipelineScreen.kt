package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SecureStorage
import com.example.ui.components.*
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.AgentViewModel

@Composable
fun PipelineScreen(viewModel: AgentViewModel = viewModel(), onOpenDrawer: () -> Unit = {}) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var apiKey by remember { mutableStateOf(SecureStorage.getApiKey(context)) }
    var oauthToken by remember { mutableStateOf(SecureStorage.getOAuthToken(context)) }
    var wakeWordEnabled by remember { mutableStateOf(SecureStorage.isWakeWordEnabled(context)) }
    var selectedModel by remember { mutableStateOf(SecureStorage.getSelectedModel(context)) }
    var selectedPersona by remember { mutableStateOf(SecureStorage.getPersona(context)) }
    var customPrompt by remember { mutableStateOf(SecureStorage.getCustomPrompt(context)) }
    var governanceEnabled by remember { mutableStateOf(SecureStorage.isGovernanceEnabled(context)) }

    var webhookName by remember { mutableStateOf("") }
    var webhookUrl by remember { mutableStateOf("https://api.example.com/webhook") }
    var webhookMethod by remember { mutableStateOf("POST") }

    var newMemKey by remember { mutableStateOf("") }
    var newMemValue by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        BlueprintGrid()
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = NeonCyan)
                }
                Column {
                    Text("PIPELINE & GOVERNANCE", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Settings, personas, webhooks, and telemetry analytics", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Telemetry & Cost Analytics
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "📊 TELEMETRY & COST METRICS ANALYTICS") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Tokens Processed", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("${uiState.totalTokensUsed}", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                                }
                                Column {
                                    Text("Est. Cost (USD)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(String.format("$%.5f", uiState.totalCostEstUsd), style = MaterialTheme.typography.titleLarge, color = Color.Green)
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Agent Calls", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("${uiState.requestCount}", style = MaterialTheme.typography.titleMedium)
                                }
                                Column {
                                    Text("Avg. Latency", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("${uiState.averageLatencyMs} ms", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }

                // Agent Governance & Confirmation Policy
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "🛡️ AGENT GOVERNANCE & HIGH-RISK GUARDRAILS") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("High-Risk Action Authorization", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                                Text("Requires explicit user approval before executing file edits, SMS dispatches, or settings changes.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Switch(
                                checked = governanceEnabled,
                                onCheckedChange = {
                                    governanceEnabled = it
                                    SecureStorage.setGovernanceEnabled(context, it)
                                    savedMessage = "Governance guardrails updated."
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                            )
                        }
                    }
                }

                // Custom Webhook & API Connector Hub
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "🔌 CUSTOM WEBHOOK & API CONNECTOR HUB") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Register external API endpoints for Sanna tool dispatch:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            OutlinedTextField(
                                value = webhookName,
                                onValueChange = { webhookName = it },
                                label = { Text("Connector Name (e.g. HomeAssistant)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = webhookUrl,
                                    onValueChange = { webhookUrl = it },
                                    label = { Text("Webhook URL Endpoint") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                                )
                                OutlinedTextField(
                                    value = webhookMethod,
                                    onValueChange = { webhookMethod = it },
                                    label = { Text("Method") },
                                    modifier = Modifier.width(90.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                                )
                            }
                            Button(
                                onClick = {
                                    if (webhookName.isNotBlank() && webhookUrl.isNotBlank()) {
                                        savedMessage = "Webhook connector '$webhookName' registered."
                                        webhookName = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Register Webhook Connector", color = Color.Black)
                            }
                        }
                    }
                }

                // Real-Time Gemini Multimodal Duplex Voice Mode
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "🎙️ REAL-TIME DUPLEX VOICE STREAMING") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Continuous Voice Duplex Stream", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                                Text("Low-latency continuous hands-free voice exchange mode.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Switch(
                                checked = uiState.isLiveDuplexActive,
                                onCheckedChange = { viewModel.toggleLiveDuplexMode() },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                            )
                        }
                    }
                }

                // Offline Engine Status
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "⚡ OFFLINE SMART CACHE & FALLBACK ENGINE") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Engine Mode: Active Offline Fallback Ready", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                            Text("When network calls drop, Sanna evaluates local queries, device telemetry, file operations, and memory vault facts offline.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }

                // Rule-Based Automation & Trigger Engine (IFTTT Macros)
                item {
                    val rulesList = remember(savedMessage) { com.example.data.AutomationEngine.getRules(context) }
                    var ruleNameInput by remember { mutableStateOf("") }
                    var triggerTypeInput by remember { mutableStateOf("BATTERY_LOW") }
                    var condValInput by remember { mutableStateOf("20") }
                    var actionPromptInput by remember { mutableStateOf("") }

                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "🔄 RULE-BASED AUTOMATION & IFTTT MACROS") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Active Automation Macros (${rulesList.size}):", style = MaterialTheme.typography.titleSmall, color = NeonCyan)
                            
                            rulesList.forEach { rule ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(rule.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                        Text("IF ${rule.triggerType} (${rule.conditionValue}) -> ${rule.actionPrompt}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = rule.isEnabled,
                                            onCheckedChange = {
                                                com.example.data.AutomationEngine.toggleRule(context, rule.id, it)
                                                savedMessage = "Rule '${rule.name}' updated."
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                                        )
                                        TextButton(onClick = {
                                            com.example.data.AutomationEngine.deleteRule(context, rule.id)
                                            savedMessage = "Rule '${rule.name}' deleted."
                                        }) {
                                            Text("DEL", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Text("Create New Automation Macro:", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                            OutlinedTextField(
                                value = ruleNameInput,
                                onValueChange = { ruleNameInput = it },
                                label = { Text("Macro Name (e.g. Night Saver)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = triggerTypeInput,
                                    onValueChange = { triggerTypeInput = it },
                                    label = { Text("Trigger (BATTERY_LOW, TIME_CRON)") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                                )
                                OutlinedTextField(
                                    value = condValInput,
                                    onValueChange = { condValInput = it },
                                    label = { Text("Condition Val") },
                                    modifier = Modifier.width(110.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                                )
                            }
                            OutlinedTextField(
                                value = actionPromptInput,
                                onValueChange = { actionPromptInput = it },
                                label = { Text("Action Prompt Directive") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                            )
                            Button(
                                onClick = {
                                    if (ruleNameInput.isNotBlank() && actionPromptInput.isNotBlank()) {
                                        com.example.data.AutomationEngine.addRule(context, ruleNameInput, triggerTypeInput, condValInput, actionPromptInput)
                                        savedMessage = "Automation macro '$ruleNameInput' created."
                                        ruleNameInput = ""
                                        actionPromptInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Create Automation Macro", color = Color.Black)
                            }
                        }
                    }
                }

                // Agent Persona Customizer
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "AGENT PERSONA & SYSTEM PROMPT CUSTOMIZER") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Active Persona Profile:", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                            
                            SecureStorage.AVAILABLE_PERSONAS.forEach { persona ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPersona = persona
                                            SecureStorage.savePersona(context, persona)
                                            savedMessage = "Persona updated to $persona"
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(persona, style = MaterialTheme.typography.bodyMedium, color = if (persona == selectedPersona) NeonCyan else Color.White)
                                    RadioButton(
                                        selected = (persona == selectedPersona),
                                        onClick = {
                                            selectedPersona = persona
                                            SecureStorage.savePersona(context, persona)
                                            savedMessage = "Persona updated to $persona"
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customPrompt,
                                onValueChange = { customPrompt = it },
                                label = { Text("Custom System Instructions") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray),
                                minLines = 2
                            )

                            Button(
                                onClick = {
                                    SecureStorage.savePersona(context, selectedPersona)
                                    SecureStorage.saveCustomPrompt(context, customPrompt)
                                    savedMessage = "Persona directives saved."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Persona Configuration", color = Color.Black)
                            }
                        }
                    }
                }

                // Long-Term Memory Vault Manager
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "🧠 PERSISTENT USER MEMORY VAULT") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Sanna remembers user facts across chat sessions:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                            if (uiState.memories.isEmpty()) {
                                Text("No facts stored in long-term memory yet.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                uiState.memories.forEach { mem ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text("• ${mem.key}", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                                            Text(mem.value, style = MaterialTheme.typography.bodySmall)
                                        }
                                        TextButton(onClick = { viewModel.deleteFact(mem.key) }) {
                                            Text("DELETE", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newMemKey,
                                    onValueChange = { newMemKey = it },
                                    label = { Text("Fact Key") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                                )
                                OutlinedTextField(
                                    value = newMemValue,
                                    onValueChange = { newMemValue = it },
                                    label = { Text("Fact Detail") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                                )
                            }
                            Button(
                                onClick = {
                                    if (newMemKey.isNotBlank() && newMemValue.isNotBlank()) {
                                        viewModel.rememberFact(newMemKey, newMemValue)
                                        newMemKey = ""
                                        newMemValue = ""
                                        savedMessage = "New fact added to Memory Vault."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Store Fact to Vault", color = Color.Black)
                            }
                        }
                    }
                }

                // Model Selector & Auto-Fallback
                item {
                    val activeDisplayName = SecureStorage.getModelDisplayName(selectedModel)
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "GEMINI MODELS & QUOTA FALLBACK") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Active Model: $activeDisplayName", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                            Text("Sanna automatically cycles through available models on HTTP 429 quota limits.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            
                            SecureStorage.AVAILABLE_MODELS.forEach { model ->
                                val displayName = SecureStorage.getModelDisplayName(model)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedModel = model
                                            viewModel.selectModel(model)
                                            savedMessage = "Selected model updated to $displayName"
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(displayName, style = MaterialTheme.typography.bodyMedium, color = if (model == selectedModel) NeonCyan else Color.White)
                                    RadioButton(
                                        selected = (model == selectedModel),
                                        onClick = {
                                            selectedModel = model
                                            viewModel.selectModel(model)
                                            savedMessage = "Selected model updated to $displayName"
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                    )
                                }
                            }
                        }
                    }
                }

                // Export Transcript
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "📤 TRANSCRIPT EXPORTER") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Export all chat history, execution tasks, and memory vault items as Markdown:", style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = {
                                    viewModel.exportTranscriptToFile()
                                    savedMessage = "Transcript exported to local device files."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Export Transcript to Markdown File", color = Color.Black)
                            }
                        }
                    }
                }

                // Layer 1: User Interaction & Wake Word
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "1. USER INTERACTION & WAKE WORD") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Wake Word Activation ('Hey Sanna')", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                                Spacer(Modifier.height(4.dp))
                                Text("Continuously listens for wake phrase in background service loop.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = wakeWordEnabled,
                                onCheckedChange = {
                                    wakeWordEnabled = it
                                    SecureStorage.setWakeWordEnabled(context, it)
                                    savedMessage = "Wake word setting updated."
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                            )
                        }
                    }
                }

                // Layer 6 & 7: Secure Storage & OAuth PKCE
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "6 & 7. SECURE STORAGE & CONNECTOR CREDENTIALS") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("Gemini API Key (Secure Vault)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray)
                            )
                            OutlinedTextField(
                                value = oauthToken,
                                onValueChange = { oauthToken = it },
                                label = { Text("OAuth / PKCE Token") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray)
                            )
                            Button(
                                onClick = {
                                    SecureStorage.saveApiKey(context, apiKey)
                                    SecureStorage.saveOAuthToken(context, oauthToken)
                                    savedMessage = "Credentials securely saved to device vault."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Secure Credentials", color = Color.Black)
                            }
                            if (savedMessage.isNotBlank()) {
                                Text(savedMessage, style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}
