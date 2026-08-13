package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.AgentUiState

@Composable
fun CoreHubScreen(state: AgentUiState, onOpenDrawer: () -> Unit = {}) {
    Box(Modifier.fillMaxSize()) {
        BlueprintGrid()
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = NeonCyan)
                }
                Column {
                    Text("CORE_OS // TELEMETRY HUB", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("STATUS: AUTONOMOUS | LATENCY ${state.latencyMs}ms | MEMORY ${state.memoryUsage}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
 Spacer(Modifier.height(24.dp))
 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
 AgentOrb()
 }
 Spacer(Modifier.height(24.dp))
 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
 HudFrame(Modifier.weight(1f).padding(end=8.dp), "CPU_NODE") { Text("${state.cpuLoad}% • 3.4GHz", style=MaterialTheme.typography.bodyMedium) }
 HudFrame(Modifier.weight(1f).padding(start=8.dp), "NEURAL_LINK") { Text("ACTIVE • 128B TOKENS", style=MaterialTheme.typography.bodyMedium) }
 }
 Spacer(Modifier.height(16.dp))
 HudFrame(Modifier.fillMaxWidth(), "ACTIVE_TASKS") {
 LazyRow {
 items(state.tasks.take(5)) { task ->
 BlueprintCard(Modifier.padding(end=8.dp), task.id, task.title)
 }
 }
 }
 }
 }
}
