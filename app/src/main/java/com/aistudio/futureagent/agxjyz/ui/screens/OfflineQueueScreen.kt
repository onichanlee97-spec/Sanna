package com.aistudio.futureagent.agxjyz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.futureagent.agxjyz.viewmodel.AgentViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun OfflineQueueScreen(viewModel: AgentViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Offline Dispatch Queue") },
                actions = {
                    IconButton(onClick = {
                        viewModel.queueOfflineRequest("https://api.sanna.agent/v1/telemetry", "{\"event\":\"heartbeat\",\"timestamp\":${System.currentTimeMillis()}}")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Enqueue Test Request")
                    }
                    IconButton(onClick = { viewModel.flushOfflineQueue() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Flush Queue")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.offlineRequests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No pending offline requests", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.offlineRequests) { request ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text("URL: ${request.url}", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Queued: ${formatTimestamp(request.timestamp)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    request.payload.take(100) + if (request.payload.length > 100) "..." else "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { /* In a real app, delete from repository */ }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Request")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(timestamp))
}
