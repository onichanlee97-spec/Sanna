package com.example.ui.screens

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
import com.example.data.CodeSandbox
import com.example.ui.components.*
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.AgentUiState
import kotlinx.coroutines.launch

@Composable
fun SandboxScreen(state: AgentUiState, onOpenDrawer: () -> Unit = {}) {
    var jsCode by remember { mutableStateOf("function fib(n) {\n  if (n <= 1) return n;\n  return fib(n-1) + fib(n-2);\n}\n'Fibonacci(10) = ' + fib(10);") }
    var outputLog by remember { mutableStateOf<List<String>>(listOf("Code Sandbox ready. Type JavaScript expression and run.")) }
    val scope = rememberCoroutineScope()

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
                    Text("CODE SANDBOX", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Sandboxed JavaScript Rhino execution engine", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = false,
                    onClick = { jsCode = "function fib(n) {\n  if (n <= 1) return n;\n  return fib(n-1) + fib(n-2);\n}\n'Fibonacci(10) = ' + fib(10);" },
                    label = { Text("Fibonacci", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = false,
                    onClick = { jsCode = "let data = JSON.parse('{\"user\":\"Sanna\",\"status\":\"Active\",\"version\":2.0}');\n'Agent: ' + data.user + ' | Version: ' + data.version;" },
                    label = { Text("JSON Parse", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = false,
                    onClick = { jsCode = "Math.sin(Math.PI / 4) * Math.sqrt(25) + Math.pow(2, 8);" },
                    label = { Text("Math Eval", style = MaterialTheme.typography.labelSmall) }
                )
            }

            HudFrame(Modifier.fillMaxWidth().weight(1f), "SCRIPT_EDITOR") {
                Column(Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = jsCode,
                        onValueChange = { jsCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    outputLog = outputLog + "> Executing sandboxed script..."
                                    val result = CodeSandbox.executeJavaScript(jsCode)
                                    outputLog = outputLog + result
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RUN SCRIPT IN SANDBOX", color = Color.Black)
                        }
                        Button(
                            onClick = { outputLog = listOf("Console cleared.") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1A24))
                        ) {
                            Text("CLEAR", color = Color.Gray)
                        }
                    }
                }
            }

            HudFrame(Modifier.fillMaxWidth().height(160.dp), "CONSOLE_OUTPUT") {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(outputLog) { log ->
                        Text(log, style = MaterialTheme.typography.bodySmall, color = if (log.contains("Error")) Color.Red else NeonCyan)
                    }
                }
            }
        }
    }
}
