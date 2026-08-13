package com.aistudio.futureagent.agxjyz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.aistudio.futureagent.agxjyz.ui.theme.GridLine

@Composable
fun BlueprintGrid() {
 Canvas(modifier = Modifier.fillMaxSize()) {
 val step = 24.dp.toPx()
 if (step > 0.5f) {
 var x = 0f
 while (x <= size.width) {
 drawLine(GridLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
 x += step
 }
 var y = 0f
 while (y <= size.height) {
 drawLine(GridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
 y += step
 }
 }
 }
}

private val Int.dp: androidx.compose.ui.unit.Dp get() = androidx.compose.ui.unit.Dp(this.toFloat())
