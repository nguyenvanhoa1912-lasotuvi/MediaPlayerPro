package com.mediaplayerpro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Bảng màu nền đậm (navy) + điểm nhấn cam/cyan. */
object AppColors {
    val Bg = Color(0xFF0B1220)
    val Surface = Color(0xFF131C2E)
    val SurfaceHigh = Color(0xFF1C2740)
    val Divider = Color(0xFF26324D)
    val Accent = Color(0xFFFF8A00)
    val OnAccent = Color(0xFF1A0E00)
    val Accent2 = Color(0xFF22D3EE)
    val Text = Color(0xFFF1F5F9)
    val Muted = Color(0xFF94A3B8)
}

private val scheme = darkColorScheme(
    primary = AppColors.Accent,
    onPrimary = AppColors.OnAccent,
    secondary = AppColors.Accent2,
    onSecondary = Color.Black,
    background = AppColors.Bg,
    onBackground = AppColors.Text,
    surface = AppColors.Surface,
    onSurface = AppColors.Text,
    surfaceVariant = AppColors.SurfaceHigh,
    onSurfaceVariant = AppColors.Muted,
    surfaceContainer = AppColors.Surface,
    surfaceContainerHigh = AppColors.SurfaceHigh,
    surfaceContainerHighest = AppColors.SurfaceHigh,
    outline = AppColors.Divider,
)

@Composable
fun MediaPlayerProTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
