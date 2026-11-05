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

private val LightColorScheme = lightColorScheme(
    primary = ChampagneGold,
    onPrimary = Color.White,
    primaryContainer = SoftGoldAccent,
    onPrimaryContainer = DeepBronzeGold,
    secondary = ElegantBlack,
    onSecondary = CreamSurface,
    secondaryContainer = SoftSandCard,
    onSecondaryContainer = ElegantBlack,
    tertiary = OliveSageAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3E8E0),
    onTertiaryContainer = Color(0xFF283325),
    background = WarmBeigeBackground,
    onBackground = ElegantBlack,
    surface = CreamSurface,
    onSurface = ElegantBlack,
    surfaceVariant = SoftSandCard,
    onSurfaceVariant = CharcoalSecondary,
    outline = WarmTaupeBorder,
    outlineVariant = Color(0xFFE5DDD0),
    error = ErrorCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkGoldPrimary,
    onPrimary = DarkObsidianBackground,
    primaryContainer = Color(0xFF4B381B),
    onPrimaryContainer = SoftGoldAccent,
    secondary = DarkCreamText,
    onSecondary = DarkObsidianBackground,
    secondaryContainer = DarkCardSurface,
    onSecondaryContainer = DarkCreamText,
    tertiary = Color(0xFF93A68D),
    onTertiary = DarkObsidianBackground,
    background = DarkObsidianBackground,
    onBackground = DarkCreamText,
    surface = DarkEspressoSurface,
    onSurface = DarkCreamText,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkMutedText,
    outline = Color(0xFF463F38),
    outlineVariant = Color(0xFF332E29),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun RoomAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    RoomAITheme(darkTheme = darkTheme, content = content)
}
