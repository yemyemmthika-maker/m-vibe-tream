package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1026),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = CyberViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2B1B54),
    onSecondaryContainer = Color(0xFFE5DEFF),
    tertiary = PulseCyan,
    onTertiary = Color(0xFF002A30),
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = Color(0xFFFF5252),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E1),
    onPrimaryContainer = Color(0xFF3E0014),
    secondary = CyberViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7DEFF),
    onSecondaryContainer = Color(0xFF20005E),
    tertiary = Color(0xFF008394),
    onTertiary = Color.White,
    background = DaylightBackground,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun VibeStreamTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
