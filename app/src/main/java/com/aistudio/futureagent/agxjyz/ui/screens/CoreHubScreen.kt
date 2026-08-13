package com.aistudio.futureagent.agxjyz.ui.screens

import android.content.Context
import android.os.BatteryManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aistudio.futureagent.agxjyz.data.AutomationEngine
import com.aistudio.futureagent.agxjyz.ui.components.*
import com.aistudio.futureagent.agxjyz.ui.theme.NeonCyan
import com.aistudio.futureagent.agxjyz.viewmodel.AgentUiState
import java.util.Locale

@Composable
fun CoreHubScreen(state: AgentUiState, onOpenDrawer: () -> Unit = {}) {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(84) }
    var isCharging by remember { mutableStateOf(false) }
    var activeRulesCount by remember { mutableIntStateOf(2) }

    LaunchedEffect(Unit) {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 84
            isCharging = bm?.isCharging ?: false
        } catch (e: Exception) {
            // fallback
        }

        try {
            activeRulesCount = AutomationEngine.getRules(context).filter { it.isEnabled }.size
        } catch (e: Exception) {
            // fallback
        }
    }

    Box(Modifier.fillMaxSize()) {
        BlueprintGrid()
        
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = NeonCyan)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SANNA_CORE // TELEMETRY",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        "STATUS: AUTONOMOUS_ACTIVE | RUNTIME ONLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                
                // Active badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x3300E5FF)),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "NODE_01",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Central Agent Orb
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                AgentOrb()
            }

            Spacer(Modifier.height(20.dp))

            // Telemetry Grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // First Row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Battery Frame
                    HudFrame(
                        modifier = Modifier.weight(1f),
                        label = "DEVICE_POWER"
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else if (batteryLevel < 20) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
                                    contentDescription = "Battery Icon",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "$batteryLevel%",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isCharging) "CHARGING_AC" else "DISCHARGING",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCharging) NeonCyan else Color.LightGray
                            )
                        }
                    }

                    // Automation Rules Count Frame
                    HudFrame(
                        modifier = Modifier.weight(1f),
                        label = "AUTOMATION_ENGINE"
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsSuggest,
                                    contentDescription = "Automation Icon",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "$activeRulesCount Rules",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "ACTIVE_MACROS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Second Row (Tokens & Costs)
                HudFrame(
                    modifier = Modifier.fillMaxWidth(),
                    label = "NEURAL_NET_METRICS"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Token,
                                    contentDescription = "Token Icon",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%,d", state.totalTokensUsed),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "ESTIMATED_TOKENS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "$%.5f", state.totalCostEstUsd),
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonCyan
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "EST_COST_USD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Third Row (System Performance Nodes)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HudFrame(
                        modifier = Modifier.weight(1f),
                        label = "CPU_LOAD"
                    ) {
                        Text(
                            text = "${state.cpuLoad}% • 3.4GHz",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    HudFrame(
                        modifier = Modifier.weight(1f),
                        label = "LATENCY"
                    ) {
                        Text(
                            text = "${state.latencyMs} ms",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Active Tasks Track
            HudFrame(
                modifier = Modifier.fillMaxWidth(),
                label = "ACTIVE_TASKS"
            ) {
                if (state.tasks.isEmpty()) {
                    Text(
                        "No tasks currently executed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.tasks.take(5)) { task ->
                            BlueprintCard(
                                modifier = Modifier.width(180.dp),
                                id = task.id,
                                type = task.title
                            )
                        }
                    }
                }
            }
        }
    }
}
