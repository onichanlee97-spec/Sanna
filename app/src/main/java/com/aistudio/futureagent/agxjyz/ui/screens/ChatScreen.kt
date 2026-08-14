package com.aistudio.futureagent.agxjyz.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import com.aistudio.futureagent.agxjyz.ui.components.*
import com.aistudio.futureagent.agxjyz.ui.theme.NeonCyan
import com.aistudio.futureagent.agxjyz.viewmodel.AgentUiState
import com.aistudio.futureagent.agxjyz.viewmodel.ChatMessage

@Composable
fun ChatScreen(
    state: AgentUiState,
    onOpenDrawer: () -> Unit = {},
    onSendMessage: (String, String?) -> Unit,
    onScheduleTrigger: (String, Long) -> Unit,
    onVoiceInput: () -> Unit,
    onClearMemory: () -> Unit,
    onSelectModel: (String) -> Unit = {},
    onConfirmAction: () -> Unit = {},
    onCancelAction: () -> Unit = {}
) {
    var input by remember { mutableStateOf("") }
    var attachedImageBase64 by remember { mutableStateOf<String?>(null) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            attachedImageBase64 = uriToBase64(context, uri)
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Box(Modifier.fillMaxSize()) {
        BlueprintGrid()
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // ChatGPT Header Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Drawer",
                        tint = NeonCyan
                    )
                }

                // Model Selector Pill (ChatGPT Style)
                Box {
                    Surface(
                        onClick = { showModelMenu = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0B1A26),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF1E3A4C))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(NeonCyan, CircleShape)
                            )
                            Text(
                                com.aistudio.futureagent.agxjyz.data.SecureStorage.getModelDisplayName(state.selectedModel),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Model",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false },
                        modifier = Modifier.background(Color(0xFF0B1A26))
                    ) {
                        com.aistudio.futureagent.agxjyz.data.SecureStorage.AVAILABLE_MODELS.forEach { modelId ->
                            val displayName = com.aistudio.futureagent.agxjyz.data.SecureStorage.getModelDisplayName(modelId)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = displayName,
                                        color = if (modelId == state.selectedModel) NeonCyan else Color.White,
                                        fontWeight = if (modelId == state.selectedModel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectModel(modelId)
                                    showModelMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = onClearMemory) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "New Chat",
                        tint = NeonCyan
                    )
                }
            }

            // Live Duplex Voice Stream Banner
            if (state.isLiveDuplexActive) {
                Spacer(Modifier.height(4.dp))
                HudFrame(label = "🎙️ LIVE DUPLEX VOICE STREAM ACTIVE") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Continuous audio duplex listener active",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                        Text("16kHz LIVE", style = MaterialTheme.typography.labelSmall, color = Color.Green)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Message Stream or ChatGPT Welcome View
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.messages.isEmpty()) {
                    // Empty Chat Welcome View (ChatGPT Style)
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.aistudio.futureagent.agxjyz.R.drawable.img_hero_sanna),
                            contentDescription = "Sanna Hero Banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(Modifier.height(24.dp))
                        AgentOrb()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Where shall we begin?",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Sanna Autonomous AI Agent • Multi-step reasoning & tool execution",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(24.dp))

                        val suggestions = listOf(
                            "⚡ Audit system battery & memory telemetry",
                            "🐝 Deploy multi-agent swarm pipeline",
                            "💻 Execute Fibonacci script in JS Sandbox",
                            "📱 Check stored files & automation rules"
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            suggestions.forEach { prompt ->
                                Card(
                                    onClick = {
                                        onSendMessage(prompt.substringAfter(" "), null)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF091622)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1B3B4F)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            prompt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.LightGray
                                        )
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Send suggestion",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(state.messages) { msg ->
                            ChatGPTMessageRow(msg = msg)
                        }

                        // Persistent Human-In-The-Loop Approval Queue
                        items(state.approvals) { approval ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0E0E)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F1919)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(8.dp).background(Color.Red, CircleShape))
                                        Spacer(Modifier.width(8.dp))
                                        Text("🛡️ PENDING SECURITY AUTHORIZATION", style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text("Action: ${approval.actionName}", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    Text("Risk Level: ${approval.riskLevel}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("Details: ${approval.payload.take(120)}...", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                    Spacer(Modifier.height(12.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onConfirmAction() }, // Uses active dialog trigger
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("AUTHORIZE", color = Color.Black, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
                                        }
                                        OutlinedButton(
                                            onClick = { onCancelAction() }, // Uses active dialog trigger
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, Color.Red),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("DENY", color = Color.Red)
                                        }
                                    }
                                }
                            }
                        }

                        if (state.isProcessing) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(28.dp)
                                            .background(NeonCyan, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Sanna thinking",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "SANNA AGENT LOOP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Thinking & executing sub-agent tools...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Attached image preview if selected
            if (attachedImageUri != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "Attached image preview",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Image attached for analysis", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    }
                    IconButton(onClick = {
                        attachedImageUri = null
                        attachedImageBase64 = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove attachment", tint = Color.Red)
                    }
                }
            }

            // ChatGPT Bottom Input Bar
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF091724),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A3B4E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment options
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach image",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onVoiceInput,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { if (input.isNotBlank()) showScheduleDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Schedule trigger",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Ask Sanna anything...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            if (input.isNotBlank() || attachedImageBase64 != null) {
                                onSendMessage(input, attachedImageBase64)
                                input = ""
                                attachedImageUri = null
                                attachedImageBase64 = null
                            }
                        },
                        enabled = input.isNotBlank() || attachedImageBase64 != null,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (input.isNotBlank() || attachedImageBase64 != null) NeonCyan else Color(0xFF132A38),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Send Prompt",
                            tint = if (input.isNotBlank() || attachedImageBase64 != null) Color.Black else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Disclaimer Footer
            Text(
                "Sanna Autonomous Agent • Gemini 2.0 Flash • Multi-Tool Execution Engine",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
            )
        }
    }

    // High Risk Governance Guardrail Dialog
    if (state.pendingConfirmation != null) {
        AlertDialog(
            onDismissRequest = onCancelAction,
            title = { Text("🛡️ AGENT GOVERNANCE POLICY GUARDRAIL", color = NeonCyan) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("High-risk sub-agent tool execution pending authorization:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("Tool: ${state.pendingConfirmation.toolName}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Description: ${state.pendingConfirmation.description}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmAction,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("AUTHORIZE ACTION", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelAction) {
                    Text("DENY & CANCEL", color = Color.Red)
                }
            }
        )
    }

    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text("Schedule Sanna Routine") },
            text = { Text("Schedule WorkManager task to run '${input.take(30)}' in the background in 1 minute?") },
            confirmButton = {
                TextButton(onClick = {
                    onScheduleTrigger(input, 1L)
                    input = ""
                    showScheduleDialog = false
                }) {
                    Text("SCHEDULE", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun ChatGPTMessageRow(msg: ChatMessage) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val decodedBitmap = remember(msg.imageBase64) {
        if (!msg.imageBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(msg.imageBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Sanna Icon",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Box {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (msg.isUser) 18.dp else 4.dp,
                    bottomEnd = if (msg.isUser) 4.dp else 18.dp
                ),
                color = if (msg.isUser) Color(0xFF0F2C3E) else Color(0xFF091622),
                border = androidx.compose.foundation.BorderStroke(
                    0.6.dp,
                    if (msg.isUser) NeonCyan.copy(alpha = 0.6f) else Color(0xFF1B3B4F)
                ),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showMenu = true }
                        )
                    }
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        if (msg.isUser) "YOU" else "SANNA",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (msg.isUser) NeonCyan else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )

                    if (decodedBitmap != null) {
                        Spacer(Modifier.height(8.dp))
                        Image(
                            bitmap = decodedBitmap.asImageBitmap(),
                            contentDescription = "Uploaded image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else if (!msg.imageBase64.isNullOrEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("[Attached Image]", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF091622)).border(0.5.dp, Color(0xFF1B3B4F))
            ) {
                DropdownMenuItem(
                    text = { Text("Copy Message", color = Color.White) },
                    onClick = {
                        try {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Copy failed", Toast.LENGTH_SHORT).show()
                        }
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonCyan)
                    }
                )
            }
        }
    }
}

private fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) Base64.encodeToString(bytes, Base64.NO_WRAP) else null
    } catch (e: Exception) {
        null
    }
}

