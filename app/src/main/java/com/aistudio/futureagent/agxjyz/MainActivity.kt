package com.aistudio.futureagent.agxjyz

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.futureagent.agxjyz.data.VoiceHelper
import com.aistudio.futureagent.agxjyz.ui.screens.*
import com.aistudio.futureagent.agxjyz.ui.theme.BlueprintTheme
import com.aistudio.futureagent.agxjyz.ui.theme.NeonCyan
import com.aistudio.futureagent.agxjyz.viewmodel.AgentViewModel
import kotlinx.coroutines.launch
import java.util.Locale

data class DrawerItemData(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun DrawerItemLabel(item: DrawerItemData, isSelected: Boolean) {
    Column {
        Text(
            item.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isSelected) NeonCyan else Color.White
        )
        Text(
            item.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) NeonCyan.copy(alpha = 0.7f) else Color.Gray
        )
    }
}

class MainActivity : FragmentActivity() {
    private val viewModel: AgentViewModel by viewModels()
    private lateinit var voiceHelper: VoiceHelper

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                voiceHelper.speak("Understood. Executing objective.")
                viewModel.sendMessage(spokenText, null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            // Safe fallback
        }
        voiceHelper = VoiceHelper(this)

        setContent {
            BlueprintTheme {
                var tab by remember { mutableIntStateOf(1) } // Default to CHAT interface
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val drawerItems = remember {
                    listOf(
                        DrawerItemData(1, "Chat Console", "Main Agent Interface", Icons.Default.Chat),
                        DrawerItemData(0, "Core System Hub", "Telemetry & Node Status", Icons.Default.Memory),
                        DrawerItemData(2, "Skills Matrix", "Dynamic Tool Manager", Icons.Default.Extension),
                        DrawerItemData(3, "Multi-Agent Swarm", "Sub-Agent Pipeline", Icons.Default.AccountTree),
                        DrawerItemData(4, "Code Sandbox", "JS Rhino Interpreter", Icons.Default.Code),
                        DrawerItemData(5, "File Workspace", "Code Syntax & Artifacts", Icons.Default.Folder),
                        DrawerItemData(6, "Pipeline & Governance", "Webhooks, Settings & Rules", Icons.Default.Settings)
                    )
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color(0xFF07131D),
                            drawerContentColor = Color.White,
                            modifier = Modifier.width(310.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    Text("SANNA AGENT", style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
                                    Text("Autonomous Voice & AI Workspace", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                                    Spacer(Modifier.height(16.dp))

                                    OutlinedButton(
                                        onClick = {
                                            tab = 1
                                            viewModel.clearMemory()
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, NeonCyan),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "New Session", modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("New Chat Session")
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    Divider(color = Color(0xFF1E3A4C))
                                    Spacer(Modifier.height(12.dp))

                                    drawerItems.forEach { item ->
                                        val isSelected = (tab == item.id)
                                        NavigationDrawerItem(
                                            label = {
                                                DrawerItemLabel(item, isSelected)
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.title,
                                                    tint = if (isSelected) NeonCyan else Color.Gray
                                                )
                                            },
                                            selected = isSelected,
                                            onClick = {
                                                tab = item.id
                                                scope.launch { drawerState.close() }
                                            },
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedContainerColor = Color(0xFF0F2B3C),
                                                unselectedContainerColor = Color.Transparent
                                            ),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Divider(color = Color(0xFF1E3A4C))
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(com.aistudio.futureagent.agxjyz.data.SecureStorage.getModelDisplayName(state.selectedModel), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Badge(containerColor = NeonCyan, contentColor = Color.Black) {
                                            Text("ONLINE", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Surface(color = Color(0xFF060D14), modifier = Modifier.fillMaxSize()) {
                        when (tab) {
                            0 -> CoreHubScreen(state, onOpenDrawer = { scope.launch { drawerState.open() } })
                            1 -> ChatScreen(
                                state = state,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onSendMessage = { prompt, img ->
                                    viewModel.sendMessage(prompt, img)
                                    voiceHelper.speak("Processing directive.")
                                },
                                onScheduleTrigger = { prompt, delay -> viewModel.scheduleBackgroundTrigger(prompt, delay) },
                                onVoiceInput = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Sanna...")
                                    }
                                    try {
                                        speechRecognizerLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        voiceHelper.speak("Speech recognition unavailable.")
                                    }
                                },
                                onClearMemory = { viewModel.clearMemory() },
                                onSelectModel = { model -> viewModel.selectModel(model) },
                                onConfirmAction = { viewModel.confirmPendingAction() },
                                onCancelAction = { viewModel.cancelPendingAction() }
                            )
                            2 -> SkillsScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                            3 -> SwarmScreen(
                                state = state,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onRunSwarm = { topic -> viewModel.sendMessage("Deploy multi-agent Swarm collaboration on topic: $topic") }
                            )
                            4 -> SandboxScreen(state, onOpenDrawer = { scope.launch { drawerState.open() } })
                            5 -> FilesScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                            6 -> PipelineScreen(viewModel, onOpenDrawer = { scope.launch { drawerState.open() } })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceHelper.shutdown()
    }
}
