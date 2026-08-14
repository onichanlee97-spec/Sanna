package com.aistudio.futureagent.agxjyz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.futureagent.agxjyz.viewmodel.AgentViewModel

@Composable
fun AuditLogScreen(viewModel: AgentViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Cryptographic Audit Logs") },
                actions = {
                    IconButton(onClick = { viewModel.loadAuditLogs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Logs")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.auditLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No audit logs recorded", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.auditLogs) { log ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = log,
                            color = Color(0xFF00FF00),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 12.dp),
                            color = Color.DarkGray,
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}
