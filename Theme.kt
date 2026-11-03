package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BotHostDarkColorScheme = darkColorScheme(
    primary = ElectricBlueLight,
    onPrimary = PureWhite,
    primaryContainer = DarkNavy700,
    onPrimaryContainer = PureWhite,
    secondary = CyberCyan,
    onSecondary = DarkNavy950,
    secondaryContainer = DarkNavy800,
    onSecondaryContainer = TextLightPrimary,
    tertiary = StatusRunningGreen,
    onTertiary = DarkNavy950,
    background = DarkNavy950,
    onBackground = TextLightPrimary,
    surface = DarkNavy900,
    onSurface = TextLightPrimary,
    surfaceVariant = DarkNavy800,
    onSurfaceVariant = TextLightSecondary,
    outline = NavyOutline,
    error = StatusErrorRed,
    onError = PureWhite,
)

private val BotHostLightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFDCEAFE),
    onPrimaryContainer = DarkNavy900,
    secondary = DarkNavy800,
    onSecondary = PureWhite,
    secondaryContainer = SlateSurfaceVariant,
    onSecondaryContainer = TextNavyPrimary,
    tertiary = StatusRunningGreen,
    onTertiary = PureWhite,
    background = PremiumWhite,
    onBackground = TextNavyPrimary,
    surface = PureWhite,
    onSurface = TextNavyPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSlateSecondary,
    outline = SlateOutline,
    error = StatusErrorRed,
    onError = PureWhite,
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BotHostXTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) BotHostDarkColorScheme else BotHostLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
