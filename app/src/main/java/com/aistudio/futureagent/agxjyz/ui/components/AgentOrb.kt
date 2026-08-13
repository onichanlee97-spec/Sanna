package com.aistudio.futureagent.agxjyz.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.aistudio.futureagent.agxjyz.ui.theme.NeonCyan

@Composable
fun AgentOrb(modifier: Modifier = Modifier) {
 val infinite = rememberInfiniteTransition(label = "orb")
 val pulse by infinite.animateFloat(0.6f, 1.2f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label="p")
 Canvas(modifier = modifier.size(180.dp)) {
 val r = size.minDimension/2 * pulse
 drawCircle(Brush.radialGradient(listOf(NeonCyan.copy(0.8f), Color.Transparent)), radius = r)
 drawCircle(NeonCyan, radius = size.minDimension/2*0.35f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
 for(i in 0..2){
 drawCircle(NeonCyan.copy(alpha = 0.3f - i*0.1f), radius = size.minDimension/2*0.5f + i*12f, style = androidx.compose.ui.graphics.drawscope.Stroke(0.8f))
 }
 }
}
