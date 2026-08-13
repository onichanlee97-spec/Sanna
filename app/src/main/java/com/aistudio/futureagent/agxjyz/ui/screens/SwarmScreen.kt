package com.aistudio.futureagent.agxjyz.ui.screens

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
import androidx.compose.ui.unit.dp
import com.aistudio.futureagent.agxjyz.ui.components.*
import com.aistudio.futureagent.agxjyz.ui.theme.NeonCyan
import com.aistudio.futureagent.agxjyz.viewmodel.AgentUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SwarmMessage(
    val agentName: String,
    val roleColor: Color,
    val text: String
)

@Composable
fun SwarmScreen(state: AgentUiState, onOpenDrawer: () -> Unit = {}, onRunSwarm: (String) -> Unit) {
    var topic by remember { mutableStateOf("") }
    var swarmMessages by remember { mutableStateOf(listOf(
        SwarmMessage("System", NeonCyan, "Multi-Agent Swarm SwarmNet initialized. Enter a topic for parallel collaboration.")
    )) }
    var isSwarmRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        BlueprintGrid()
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = NeonCyan)
                }
                Column {
                    Text("MULTI-AGENT SWARM", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Sub-agent collaboration & debate pipeline", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(12.dp))

            HudFrame(Modifier.fillMaxWidth().weight(1f), "SWARM_DEBATE_STREAM") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(swarmMessages) { msg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1A24)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(msg.agentName, style = MaterialTheme.typography.labelMedium, color = msg.roleColor)
                                Spacer(Modifier.height(4.dp))
                                Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HudFrame(Modifier.fillMaxWidth(), "SWARM_INPUT") {
                Column {
                    TextField(
                        value = topic,
                        onValueChange = { topic = it },
                        placeholder = { Text("// enter research topic or architectural challenge...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (topic.isNotBlank() && !isSwarmRunning) {
                                val currentTopic = topic
                                topic = ""
                                isSwarmRunning = true
                                scope.launch {
                                    swarmMessages = swarmMessages + SwarmMessage("Orchestrator", NeonCyan, "Deploying swarm for topic: '$currentTopic'")
                                    delay(800)
                                    swarmMessages = swarmMessages + SwarmMessage("Research Agent", Color(0xFF00FF66), "Gathering global facts, literature, and architectural patterns regarding $currentTopic...")
                                    delay(1200)
                                    swarmMessages = swarmMessages + SwarmMessage("Code Analyst", Color(0xFFFFB000), "Synthesizing implementation specs, data structures, and algorithmic logic for $currentTopic...")
                                    delay(1200)
                                    swarmMessages = swarmMessages + SwarmMessage("Critic Agent", Color(0xFFFF4444), "Reviewing potential edge cases, bottlenecks, and security implications. Consensus reached: Optimal design verified.")
                                    onRunSwarm(currentTopic)
                                    isSwarmRunning = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSwarmRunning
                    ) {
                        Text(if (isSwarmRunning) "SWARM COLLABORATING..." else "INITIATE SWARM DEBATE", color = Color.Black)
                    }
                }
            }
        }
    }
}
