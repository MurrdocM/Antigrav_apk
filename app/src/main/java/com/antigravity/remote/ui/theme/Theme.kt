package com.antigravity.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AntigravityColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    background = BgDark,
    surface = SurfaceDark,
    onPrimary = TextPrimary,
    onSecondary = BgDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = DangerColor
)

@Composable
fun AntigravityTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AntigravityColorScheme,
        content = content
    )
}
