package com.aistudio.futureagent.agxjyz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.futureagent.agxjyz.viewmodel.AgentViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.platform.LocalContext
import com.aistudio.futureagent.agxjyz.MainActivity
import com.aistudio.futureagent.agxjyz.service.AgentListeningService

@Composable
fun DiagnosticsScreen(viewModel: AgentViewModel) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text("Agent Diagnostics") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("WorkManager Background Scheduler", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Maintenance worker runs every 15 minutes to prune expired vector embeddings and sync memory vaults.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.triggerMaintenance() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trigger Manual Maintenance")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("IPC Bound Service Bridge", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Bi-directional Messenger bridge for secure inter-process communication between UI and Agent Core.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.sendToolExecutionRequest("ping_test") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test IPC Messenger Link")
                        }
                    }
                }
            }

            item {
                val context = LocalContext.current
                var isListeningActive by remember { mutableStateOf(AgentListeningService.isRunning) }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Floating Overlay & Permanent Open Mic", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Launches a draggable WindowManager floating mic icon over all apps with permanently open, continuous background audio listening.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (isListeningActive) {
                                    AgentListeningService.stop(context)
                                    isListeningActive = false
                                } else {
                                    val activity = context as? MainActivity
                                    if (activity != null) {
                                        activity.requestOverlayAndStartListening()
                                        isListeningActive = true
                                    } else {
                                        AgentListeningService.start(context)
                                        isListeningActive = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (isListeningActive) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isListeningActive) "Stop Floating Overlay & Mic" else "Launch Floating Icon & Permanent Mic")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Foreground Service Watchdog", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Executes long-running agent tasks in the foreground with an ongoing status notification leading back to chat.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.startForegroundAgentTask("Scraping web research data") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Foreground Agent Task")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Automatic Network Connectivity Flusher", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Background daemon listening to NetworkCallback to automatically flush the Room offline queue when online.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.flushOfflineQueue() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trigger Offline Queue Flush")
                        }
                    }
                }
            }

            item {
                Text("System Metadata", style = MaterialTheme.typography.labelLarge)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                MetadataItem("Sandbox Mode", "Active (Restricted Toybox)")
                MetadataItem("Audit Chain", "Tamper-Evident SHA-256")
                MetadataItem("Network Daemon", "NetworkCallback Connected")
                MetadataItem("Foreground Watchdog", "AgentForegroundService")
                MetadataItem("Memory Trimmer", "Active (TRIM_MEMORY_RUNNING_CRITICAL)")
            }
        }
    }
}

@Composable
fun MetadataItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
