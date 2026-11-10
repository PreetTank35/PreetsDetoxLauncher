package com.preet.detoxlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun PreetsDetoxLauncherTheme(
    isPureAmoled: Boolean = true,
    accentIndex: Int = 0,
    isLightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val accent = AvailableAccents.getOrElse(accentIndex) { AccentEmerald }

    val colorScheme = if (isLightMode) {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = Color(0xFFF9F9F9),
            onBackground = Color(0xFF111111),
            surface = Color.White,
            onSurface = Color(0xFF111111),
            surfaceVariant = Color(0xFFEEEEEE),
            onSurfaceVariant = Color(0xFF555555),
            outline = Color(0xFFCCCCCC)
        )
    } else {
        val bg = if (isPureAmoled) AmoledBlack else NeutralDarkBackground
        darkColorScheme(
            primary = accent,
            onPrimary = Color.Black,
            background = bg,
            onBackground = TextPrimary,
            surface = SurfaceDark,
            onSurface = TextPrimary,
            surfaceVariant = CardElevated,
            onSurfaceVariant = TextSecondary,
            outline = BorderDivider
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
