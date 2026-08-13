package com.aistudio.futureagent.agxjyz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.futureagent.agxjyz.ui.components.*
import com.aistudio.futureagent.agxjyz.viewmodel.AgentUiState
import com.aistudio.futureagent.agxjyz.viewmodel.TaskStatus

@Composable
fun TaskMatrixScreen(state: AgentUiState) {
 Box(Modifier.fillMaxSize()) {
 BlueprintGrid()
 Column(Modifier.fillMaxSize().padding(16.dp)) {
 Text("TASK_MATRIX // AUTONOMOUS QUEUE", style = MaterialTheme.typography.headlineMedium)
 Spacer(Modifier.height(16.dp))
 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
 val queuedTasks = state.tasks.filter { it.status == TaskStatus.QUEUED }
 val executingTasks = state.tasks.filter { it.status == TaskStatus.EXECUTING }
 val doneTasks = state.tasks.filter { it.status == TaskStatus.DONE }

 HudFrame(Modifier.weight(1f).fillMaxHeight(0.8f), "QUEUED") {
 LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
 items(queuedTasks) { task ->
 Text("[ ] ${task.title}", style = MaterialTheme.typography.bodyMedium)
 }
 }
 }
 HudFrame(Modifier.weight(1f).fillMaxHeight(0.8f), "EXECUTING") {
 LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
 items(executingTasks) { task ->
 Text("[~] ${task.title}", style = MaterialTheme.typography.bodyMedium)
 }
 }
 }
 HudFrame(Modifier.weight(1f).fillMaxHeight(0.8f), "DONE") {
 LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
 items(doneTasks) { task ->
 Text("[x] ${task.title}", style = MaterialTheme.typography.bodyMedium)
 }
 }
 }
 }
 }
 }
}
