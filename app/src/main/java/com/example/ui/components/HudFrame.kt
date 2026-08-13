package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan

@Composable
fun HudFrame(modifier: Modifier = Modifier, label: String = "SYS_01", content: @Composable () -> Unit) {
 Card(
 modifier = modifier.drawBehind {
 val stroke = 2f
 val len = 20f
 drawLine(NeonCyan, Offset(0f,0f), Offset(len,0f), stroke)
 drawLine(NeonCyan, Offset(0f,0f), Offset(0f,len), stroke)
 drawLine(NeonCyan, Offset(size.width,0f), Offset(size.width-len,0f), stroke)
 drawLine(NeonCyan, Offset(size.width,0f), Offset(size.width,len), stroke)
 drawLine(NeonCyan, Offset(0f,size.height), Offset(len,size.height), stroke)
 drawLine(NeonCyan, Offset(0f,size.height), Offset(0f,size.height-len), stroke)
 drawLine(NeonCyan, Offset(size.width,size.height), Offset(size.width-len,size.height), stroke)
 drawLine(NeonCyan, Offset(size.width,size.height), Offset(size.width,size.height-len), stroke)
 },
 shape = RoundedCornerShape(12.dp),
 colors = CardDefaults.cardColors(containerColor = Color(0x1100E5FF)),
 border = BorderStroke(0.5.dp, Color(0x4400E5FF))
 ) {
 Column(Modifier.padding(14.dp)) {
 Text("■ $label", style = MaterialTheme.typography.labelSmall)
 Spacer(Modifier.height(8.dp))
 content()
 }
 }
}
