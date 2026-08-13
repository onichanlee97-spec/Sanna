package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.GridLine

@Composable
fun BlueprintGrid() {
 Canvas(modifier = Modifier.fillMaxSize()) {
 val step = 24.dp.toPx()
 for (x in 0..size.width.toInt() step step.toInt()) {
 drawLine(GridLine, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 0.5f)
 }
 for (y in 0..size.height.toInt() step step.toInt()) {
 drawLine(GridLine, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 0.5f)
 }
 }
}

private val Int.dp: androidx.compose.ui.unit.Dp get() = androidx.compose.ui.unit.Dp(this.toFloat())
