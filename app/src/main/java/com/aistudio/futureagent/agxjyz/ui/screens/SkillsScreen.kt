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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.futureagent.agxjyz.data.AgentSkill
import com.aistudio.futureagent.agxjyz.data.SkillManager
import com.aistudio.futureagent.agxjyz.ui.components.*
import com.aistudio.futureagent.agxjyz.ui.theme.NeonCyan

@Composable
fun SkillsScreen(onOpenDrawer: () -> Unit = {}) {
    val skills by SkillManager.skills.collectAsStateWithLifecycle()
    var selectedSkill by remember { mutableStateOf<AgentSkill?>(null) }

    Box(Modifier.fillMaxSize()) {
        BlueprintGrid()
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = NeonCyan)
                }
                Column {
                    Text("AGENT SKILLS MATRIX", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Dynamic markdown skill toggles for AI agent loop", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(skills) { skill ->
                    HudFrame(modifier = Modifier.fillMaxWidth(), label = skill.id.uppercase()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(skill.name, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                                Spacer(Modifier.height(4.dp))
                                Text(skill.description, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = skill.isEnabled,
                                onCheckedChange = { checked ->
                                    SkillManager.toggleSkill(skill.id, checked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { selectedSkill = skill },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1A24)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("VIEW SKILL.md", color = NeonCyan)
                        }
                    }
                }
            }
        }
    }

    if (selectedSkill != null) {
        AlertDialog(
            onDismissRequest = { selectedSkill = null },
            title = { Text(selectedSkill!!.name) },
            text = {
                Column {
                    Text(selectedSkill!!.markdownContent, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSkill = null }) {
                    Text("CLOSE", color = NeonCyan)
                }
            }
        )
    }
}
