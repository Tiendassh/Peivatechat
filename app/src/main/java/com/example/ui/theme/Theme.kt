package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    secondary = CoralSecondary,
    tertiary = RoseTertiary,
    background = MidnightBackground,
    surface = CardSurface,
    surfaceVariant = CardSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    secondary = CoralSecondary,
    tertiary = RoseTertiary,
    background = Color(0xFFFFF6F8), // Soft pink-ish white for light theme
    surface = Color.White,
    surfaceVariant = Color(0xFFFCEEF1),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF2C1D21),
    onSurface = Color(0xFF2C1D21),
    onSurfaceVariant = Color(0xFF705E63)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to prioritize our signature theme branding!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
