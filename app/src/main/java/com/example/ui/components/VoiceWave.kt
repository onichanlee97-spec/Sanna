package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import kotlin.math.sin

@Composable
fun VoiceWave(modifier: Modifier = Modifier) {
 val infinite = rememberInfiniteTransition(label = "wave")
 val phase by infinite.animateFloat(0f, 6.28f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "phase")
 Canvas(modifier = modifier.fillMaxWidth().height(40.dp)) {
 val width = size.width
 val height = size.height
 val centerY = height / 2f
 val points = 50
 val stepX = width / points
 for (i in 0 until points) {
 val x1 = i * stepX
 val x2 = (i + 1) * stepX
 val y1 = centerY + sin(i * 0.3f + phase) * (height * 0.4f)
 val y2 = centerY + sin((i + 1) * 0.3f + phase) * (height * 0.4f)
 drawLine(NeonCyan, Offset(x1, y1), Offset(x2, y2), strokeWidth = 2f)
 }
 }
}
