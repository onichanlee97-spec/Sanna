package com.aistudio.futureagent.agxjyz.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val BlueprintBlack = Color(0xFF060D14)
val GridLine = Color(0xFF0D2130)
val NeonCyan = Color(0xFF00E5FF)
val NeonPurple = Color(0xFF7C4DFF)
val NeonMint = Color(0xFF00FFC2)
val TextDim = Color(0x99E0F7FF)

val BlueprintDarkScheme = darkColorScheme(
 background = BlueprintBlack,
 surface = Color(0xFF0A1A24),
 primary = NeonCyan,
 secondary = NeonPurple,
 tertiary = NeonMint,
 onBackground = Color(0xFFE0F7FF),
 onSurface = Color(0xFFE0F7FF)
)

@Composable
fun BlueprintTheme(content: @Composable () -> Unit) {
 MaterialTheme(
 colorScheme = BlueprintDarkScheme,
 typography = Typography(
 labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextDim, letterSpacing = 1.5.sp),
 bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.White),
 headlineMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp, color = NeonCyan)
 ),
 content = content
 )
}
