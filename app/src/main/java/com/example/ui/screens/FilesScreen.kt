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
import com.example.data.SannaTools
import com.example.ui.components.*
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.launch

data class StoredFileInfo(
    val name: String,
    val sizeBytes: Long
)

@Composable
fun FilesScreen(onOpenDrawer: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var filesList by remember { mutableStateOf<List<StoredFileInfo>>(emptyList()) }
    var filenameInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var selectedFileContent by remember { mutableStateOf<Pair<String, String>?>(null) }
    var editModeContent by remember { mutableStateOf("") }

    fun refreshFiles() {
        scope.launch {
            val list = context.filesDir.listFiles()
            if (list != null) {
                filesList = list.map { StoredFileInfo(it.name, it.length()) }.sortedByDescending { it.name }
            } else {
                filesList = emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshFiles()
    }

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
                    Text("WORKSPACE FILES", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Code syntax viewer, artifacts, and transcripts", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // File Creation Card
            HudFrame(modifier = Modifier.fillMaxWidth(), label = "NEW FILE / ARTIFACT CREATOR") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = filenameInput,
                            onValueChange = { filenameInput = it },
                            label = { Text("Filename (e.g. script.js, transcript.md)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray)
                        )
                    }
                    OutlinedTextField(
                        value = contentInput,
                        onValueChange = { contentInput = it },
                        label = { Text("Content / Code Snippet") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = Color.Gray)
                    )
                    Button(
                        onClick = {
                            if (filenameInput.isNotBlank()) {
                                scope.launch {
                                    val res = SannaTools.createFile(context, filenameInput, contentInput)
                                    statusMessage = res
                                    refreshFiles()
                                    filenameInput = ""
                                    contentInput = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save File to Workspace", color = Color.Black)
                    }
                    if (statusMessage.isNotBlank()) {
                        Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                    }
                }
            }

            // File Explorer List
            HudFrame(modifier = Modifier.weight(1f), label = "STORED WORKSPACE FILES (${filesList.size})") {
                if (filesList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No local files found. Export a transcript or create a file above.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filesList) { file ->
                            val ext = file.name.substringAfterLast('.', "TXT").uppercase()
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1A24)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            val text = SannaTools.readFile(context, file.name)
                                            selectedFileContent = Pair(file.name, text)
                                            editModeContent = text
                                        }
                                    }
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Badge(containerColor = NeonCyan, contentColor = Color.Black) {
                                            Text(ext, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Column {
                                            Text(file.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                            Text("${file.sizeBytes} bytes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }

                                    TextButton(onClick = {
                                        scope.launch {
                                            SannaTools.deleteFile(context, file.name)
                                            refreshFiles()
                                        }
                                    }) {
                                        Text("DELETE", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Code Viewer & Editor
    if (selectedFileContent != null) {
        val (fileName, _) = selectedFileContent!!
        AlertDialog(
            onDismissRequest = { selectedFileContent = null },
            title = { Text("CODE VIEWER: $fileName", color = NeonCyan) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total characters: ${editModeContent.length}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    OutlinedTextField(
                        value = editModeContent,
                        onValueChange = { editModeContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            SannaTools.editFile(context, fileName, editModeContent)
                            refreshFiles()
                            selectedFileContent = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("SAVE CHANGES", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFileContent = null }) {
                    Text("CLOSE", color = Color.Gray)
                }
            }
        )
    }
}
