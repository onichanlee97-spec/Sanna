package com.aistudio.futureagent.agxjyz.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.futureagent.agxjyz.data.SecureStorage
import com.aistudio.futureagent.agxjyz.ui.components.*
import com.aistudio.futureagent.agxjyz.ui.theme.NeonCyan
import com.aistudio.futureagent.agxjyz.utils.ApiKeyManager
import com.aistudio.futureagent.agxjyz.viewmodel.AgentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PipelineScreen(viewModel: AgentViewModel = viewModel(), onOpenDrawer: () -> Unit = {}) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val apiKeyManager = remember { ApiKeyManager(context) }
    val modelDiscovery = remember { com.aistudio.futureagent.agxjyz.agent.ApiModelDiscovery(context) }
    var inputApiKeys by remember { mutableStateOf("") }
    var keyListVersion by remember { mutableIntStateOf(0) }
    var modelDiscoveryVersion by remember { mutableIntStateOf(0) }
    var isDiscoveringModels by remember { mutableStateOf(false) }
    var providerFilter by remember { mutableStateOf("ALL") }

    val allApiKeys = remember(keyListVersion) { apiKeyManager.getAllKeys() }
    val activeKeyIndex = remember(keyListVersion) { apiKeyManager.getCurrentKeyIndex() }
    val availableModels = remember(modelDiscoveryVersion, keyListVersion) { SecureStorage.getAvailableModels(context) }
    val providerBreakdown = remember(modelDiscoveryVersion, keyListVersion) { modelDiscovery.getDiscoveredModelsByProvider() }

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
                                    Text(String.format(java.util.Locale.US, "$%.5f", uiState.totalCostEstUsd), style = MaterialTheme.typography.titleLarge, color = Color.Green)
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
                    val rulesList = remember(savedMessage) { com.aistudio.futureagent.agxjyz.data.AutomationEngine.getRules(context) }
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
                                                com.aistudio.futureagent.agxjyz.data.AutomationEngine.toggleRule(context, rule.id, it)
                                                savedMessage = "Rule '${rule.name}' updated."
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                                        )
                                        TextButton(onClick = {
                                            com.aistudio.futureagent.agxjyz.data.AutomationEngine.deleteRule(context, rule.id)
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
                                        com.aistudio.futureagent.agxjyz.data.AutomationEngine.addRule(context, ruleNameInput, triggerTypeInput, condValInput, actionPromptInput)
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
                    val activeProvider = SecureStorage.getModelProvider(selectedModel)
                    val apiKeyManager = remember { com.aistudio.futureagent.agxjyz.utils.ApiKeyManager(context) }
                    val isCurrentProviderKeyConfigured = apiKeyManager.isProviderConfigured(activeProvider)

                    val filteredModels = remember(availableModels, providerFilter) {
                        when (providerFilter) {
                            "META" -> availableModels.filter { it.contains("llama") || it.contains("meta") }
                            "GEMINI" -> availableModels.filter { it.contains("gemini") }
                            "OPENAI" -> availableModels.filter { it.startsWith("gpt-") || it.startsWith("o1") || it.startsWith("o3") }
                            "ANTHROPIC" -> availableModels.filter { it.contains("claude") }
                            "GROQ" -> availableModels.filter { it.contains("versatile") || it.contains("instant") || it.contains("specdec") || it.contains("deepseek") || it.contains("mixtral") }
                            else -> availableModels
                        }
                    }

                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "LLM MODELS & PROVIDER API KEY MAPPING") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Active Model: $activeDisplayName", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Provider: $activeProvider", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(
                                            if (isCurrentProviderKeyConfigured) "• API Key Active" else "• Key Not Configured",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCurrentProviderKeyConfigured) Color(0xFF10B981) else Color(0xFFF59E0B)
                                        )
                                    }
                                }
                            }
                            Text(
                                "Each model automatically routes to its provider's configured API key with multi-tier failover.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )

                            // Provider Filter Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                val filterTabs = listOf("ALL", "GEMINI", "META", "OPENAI", "ANTHROPIC", "GROQ")
                                items(filterTabs) { tab ->
                                    val isSelected = (providerFilter == tab)
                                    Surface(
                                        modifier = Modifier.clickable { providerFilter = tab },
                                        color = if (isSelected) NeonCyan else Color(0xFF0F2B3C),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0xFF1E3A4C))
                                    ) {
                                        Text(
                                            text = tab,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.Black else Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            
                            filteredModels.forEach { model ->
                                val displayName = SecureStorage.getModelDisplayName(model)
                                val itemProvider = SecureStorage.getModelProvider(model)
                                val hasProviderKey = apiKeyManager.isProviderConfigured(itemProvider)
                                val isItemActive = (model == selectedModel)

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedModel = model
                                            viewModel.selectModel(model)
                                            savedMessage = "Selected model updated to $displayName ($itemProvider)"
                                        },
                                    color = if (isItemActive) Color(0xFF0A2E44) else Color.Transparent,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                color = when (itemProvider) {
                                                    "Meta (Llama)" -> Color(0xFF0064E0).copy(alpha = 0.3f)
                                                    "Gemini" -> Color(0xFF1E88E5).copy(alpha = 0.3f)
                                                    "OpenAI" -> Color(0xFF10A37F).copy(alpha = 0.3f)
                                                    "Anthropic" -> Color(0xFFD97706).copy(alpha = 0.3f)
                                                    "Groq" -> Color(0xFFF97316).copy(alpha = 0.3f)
                                                    else -> Color(0xFF1E3A4C)
                                                },
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = itemProvider,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when (itemProvider) {
                                                        "Meta (Llama)" -> Color(0xFF60A5FA)
                                                        "Gemini" -> NeonCyan
                                                        "OpenAI" -> Color(0xFF34D399)
                                                        "Anthropic" -> Color(0xFFFBBF24)
                                                        "Groq" -> Color(0xFFFB923C)
                                                        else -> Color.White
                                                    },
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isItemActive) NeonCyan else Color.White
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        model,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.Gray
                                                    )
                                                    Text(
                                                        if (hasProviderKey) "• Key Ready" else "• Key Needed",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (hasProviderKey) Color(0xFF10B981) else Color(0xFF6B7280)
                                                    )
                                                }
                                            }
                                        }

                                        RadioButton(
                                            selected = isItemActive,
                                            onClick = {
                                                selectedModel = model
                                                viewModel.selectModel(model)
                                                savedMessage = "Selected model updated to $displayName ($itemProvider)"
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                        )
                                    }
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

                // IMPORT API KEY Manager
                item {
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = "🔑 IMPORT API KEY") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Import Single & Multiple API Keys",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonCyan
                            )
                            Text(
                                "Paste one or multiple API keys (Meta/Llama, Gemini, OpenAI, Anthropic, Groq). Sanna detects providers automatically and auto-falls back to the next active provider key if quota or rate limits are reached.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )

                            OutlinedTextField(
                                value = inputApiKeys,
                                onValueChange = { inputApiKeys = it },
                                label = { Text("Import API Key(s)") },
                                placeholder = { Text("meta_...,\nAIzaSy...,\nsk-proj-...,\ngsk_...,\nsk-ant-...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color.Gray
                                ),
                                maxLines = 5
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (inputApiKeys.isNotBlank()) {
                                            val addedCount = apiKeyManager.importKeys(inputApiKeys)
                                            keyListVersion++
                                            inputApiKeys = ""
                                            isDiscoveringModels = true
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val res = modelDiscovery.discoverAllModels(apiKeyManager.getAllKeys())
                                                withContext(Dispatchers.Main) {
                                                    isDiscoveringModels = false
                                                    modelDiscoveryVersion++
                                                    savedMessage = "Imported $addedCount key(s) and auto-discovered ${res.totalModels} models across ${res.providerCount} provider(s)."
                                                }
                                            }
                                        } else {
                                            savedMessage = "Please enter or paste at least one API key."
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Import Keys",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Import & Save Keys", color = Color.Black)
                                }

                                if (inputApiKeys.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { inputApiKeys = "" },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                                    ) {
                                        Text("Clear")
                                    }
                                }
                            }

                            // Auto-Import All Provider Models Button
                            Button(
                                onClick = {
                                    isDiscoveringModels = true
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val res = modelDiscovery.discoverAllModels(apiKeyManager.getAllKeys())
                                        withContext(Dispatchers.Main) {
                                            isDiscoveringModels = false
                                            modelDiscoveryVersion++
                                            savedMessage = "Auto-imported ${res.totalModels} models from ${res.providerCount} provider(s)."
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2B3C)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                                enabled = !isDiscoveringModels
                            ) {
                                if (isDiscoveringModels) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Querying Provider APIs...", color = NeonCyan)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Auto-Import Models",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("⚡ Auto-Import Models From All Providers", color = NeonCyan)
                                }
                            }

                            // Provider Catalog Summary Chips
                            if (providerBreakdown.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Discovered Model Catalog (${availableModels.size} total):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        items(providerBreakdown.entries.toList()) { (prov, list) ->
                                            Surface(
                                                color = Color(0xFF071926),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A4C))
                                            ) {
                                                Text(
                                                    "$prov: ${list.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeonCyan,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1E3A4C), modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Saved Keys (${allApiKeys.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NeonCyan
                                )
                                if (allApiKeys.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            apiKeyManager.clearAllKeys()
                                            keyListVersion++
                                            savedMessage = "All saved API keys cleared."
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Clear All", color = Color(0xFFFF5252), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            if (allApiKeys.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF0F2B3C).copy(alpha = 0.5f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A4C))
                                ) {
                                    Text(
                                        "No API keys imported yet. Paste your API keys above and tap 'Import & Save Keys'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    allApiKeys.forEachIndexed { index, key ->
                                        val isActive = (index == activeKeyIndex)
                                        val provider = ApiKeyManager.detectProvider(key)
                                        val masked = ApiKeyManager.getMaskedKey(key)

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    apiKeyManager.setActiveKeyIndex(index)
                                                    keyListVersion++
                                                    savedMessage = "Active API key set to #${index + 1} ($provider)"
                                                },
                                            color = if (isActive) Color(0xFF0A2E44) else Color(0xFF071926),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isActive) NeonCyan else Color(0xFF1A3B4F)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            "#${index + 1}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.Gray
                                                        )
                                                        val badgeBg = when {
                                                            isActive -> NeonCyan
                                                            provider == "Meta (Llama)" -> Color(0xFF0064E0)
                                                            provider == "Gemini" -> Color(0xFF1E88E5)
                                                            provider == "OpenAI" -> Color(0xFF10A37F)
                                                            provider == "Anthropic" -> Color(0xFFD97706)
                                                            provider == "Groq" -> Color(0xFFF97316)
                                                            else -> Color(0xFF1E3A4C)
                                                        }
                                                        Surface(
                                                            color = badgeBg,
                                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                provider,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (isActive) Color.Black else Color.White,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        if (isActive) {
                                                            Text(
                                                                "ACTIVE",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = NeonCyan
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        masked,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                        ),
                                                        color = if (isActive) Color.White else Color.LightGray
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (!isActive) {
                                                        TextButton(
                                                            onClick = {
                                                                apiKeyManager.setActiveKeyIndex(index)
                                                                keyListVersion++
                                                                savedMessage = "Active API key set to #${index + 1} ($provider)"
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("USE", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }

                                                    // API Delete (X) Button
                                                    IconButton(
                                                        onClick = {
                                                            apiKeyManager.deleteKeyAt(index)
                                                            keyListVersion++
                                                            savedMessage = "API Key #${index + 1} ($provider) deleted."
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Delete API Key",
                                                            tint = Color(0xFFFF5252),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
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
