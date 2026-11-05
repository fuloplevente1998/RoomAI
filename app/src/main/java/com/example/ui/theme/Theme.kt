package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * RoomAI Elegant Light Color Scheme:
 * - Meleg bézs háttér (#F6F1EA)
 * - Krémszínű és törtfehér felületek (#FCF9F4)
 * - Elegáns fekete tipográfia (#1C1917)
 * - Finom pezsgőarany kiemelések (#B88E52)
 */
val RoomAILightColorScheme = lightColorScheme(
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
    tertiaryContainer = SoftSageContainer,
    onTertiaryContainer = DeepSageOnContainer,
    background = WarmBeigeBackground,
    onBackground = ElegantBlack,
    surface = CreamSurface,
    onSurface = ElegantBlack,
    surfaceVariant = SoftSandCard,
    onSurfaceVariant = CharcoalSecondary,
    surfaceTint = ChampagneGold,
    surfaceBright = PureIvorySurfaceBright,
    surfaceDim = DeepSandContainerHighest,
    surfaceContainerLowest = SoftAlabasterContainerLowest,
    surfaceContainerLow = WarmCreamContainerLow,
    surfaceContainer = SoftSandContainer,
    surfaceContainerHigh = WarmSandContainerHigh,
    surfaceContainerHighest = DeepSandContainerHighest,
    inverseSurface = ElegantBlack,
    inverseOnSurface = CreamSurface,
    inversePrimary = GoldHighlight,
    outline = WarmTaupeBorder,
    outlineVariant = SubtleLinenDivider,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorLightContainer,
    onErrorContainer = OnErrorLightContainer,
    scrim = Color(0x661C1917)
)

/**
 * RoomAI Elegant Dark Color Scheme:
 * - Mély obszidián fekete háttér (#141210) és meleg espresso kártyafelületek (#1E1B18)
 * - Törtfehér / meleg bézs tipográfia (#F6F1EA)
 * - Pezsgőarany és meleg bézs akcentusok (#D6AE72)
 */
val RoomAIDarkColorScheme = darkColorScheme(
    primary = DarkGoldPrimary,
    onPrimary = DarkObsidianBackground,
    primaryContainer = DarkGoldContainer,
    onPrimaryContainer = DarkOnGoldContainer,
    secondary = DarkWarmBeigeSecondary,
    onSecondary = DarkObsidianBackground,
    secondaryContainer = DarkCardSurface,
    onSecondaryContainer = DarkCreamText,
    tertiary = Color(0xFF93A68D),
    onTertiary = DarkObsidianBackground,
    tertiaryContainer = Color(0xFF283325),
    onTertiaryContainer = SoftSageContainer,
    background = DarkObsidianBackground,
    onBackground = DarkCreamText,
    surface = DarkEspressoSurface,
    onSurface = DarkCreamText,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkMutedText,
    surfaceTint = DarkGoldPrimary,
    surfaceBright = DarkSurfaceBright,
    surfaceDim = DarkContainerLowest,
    surfaceContainerLowest = DarkContainerLowest,
    surfaceContainerLow = DarkContainerLow,
    surfaceContainer = DarkCardSurface,
    surfaceContainerHigh = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHighest,
    inverseSurface = CreamSurface,
    inverseOnSurface = ElegantBlack,
    inversePrimary = ChampagneGold,
    outline = DarkWarmTaupeBorder,
    outlineVariant = DarkSubtleDivider,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    scrim = Color(0x99000000)
)

/**
 * Builds theme-aware editorial typography so text colors adapt seamlessly
 * between Elegant Black (#1C1917) in Light Mode and Warm Off-White (#F6F1EA) in Dark Mode.
 */
private fun buildRoomAITypography(isDark: Boolean): Typography {
    val primaryTextColor = if (isDark) DarkCreamText else ElegantBlack
    val secondaryTextColor = if (isDark) DarkMutedText else CharcoalSecondary

    return Typography(
        displayLarge = TextStyle(
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.5).sp,
            color = primaryTextColor
        ),
        displayMedium = TextStyle(
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            color = primaryTextColor
        ),
        headlineLarge = TextStyle(
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 34.sp,
            color = primaryTextColor
        ),
        headlineMedium = TextStyle(
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = primaryTextColor
        ),
        headlineSmall = TextStyle(
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 19.sp,
            lineHeight = 25.sp,
            color = primaryTextColor
        ),
        titleLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = primaryTextColor
        ),
        titleMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp,
            color = primaryTextColor
        ),
        titleSmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            color = primaryTextColor
        ),
        bodyLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = primaryTextColor
        ),
        bodyMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = primaryTextColor
        ),
        bodySmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = secondaryTextColor
        ),
        labelLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp,
            color = primaryTextColor
        ),
        labelMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp,
            color = primaryTextColor
        ),
        labelSmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.4.sp,
            color = secondaryTextColor
        )
    )
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun RoomAITheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RoomAIDarkColorScheme else RoomAILightColorScheme
    val typography = buildRoomAITypography(isDark = darkTheme)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
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
