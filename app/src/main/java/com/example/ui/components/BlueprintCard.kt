package com.example.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BlueprintCard(modifier: Modifier = Modifier, id: String, type: String) {
 HudFrame(modifier.width(120.dp), id) { Text(type, style = MaterialTheme.typography.labelSmall) }
}
