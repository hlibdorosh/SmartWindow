package com.example.smartwindowsill.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MossOnDark,
    onPrimary = Color(0xFF12211A),
    primaryContainer = MossContainerDark,
    onPrimaryContainer = Chalk,
    secondary = ChalkSecondary,
    onSecondary = Soil,
    secondaryContainer = SoilSunken,
    onSecondaryContainer = Chalk,
    tertiary = ChalkSecondary,
    background = Soil,
    onBackground = Chalk,
    surface = SoilRaised,
    onSurface = Chalk,
    surfaceVariant = SoilSunken,
    onSurfaceVariant = ChalkSecondary,
    surfaceContainerLowest = Soil,
    surfaceContainerLow = SoilRaised,
    surfaceContainer = SoilRaised,
    surfaceContainerHigh = SoilSunken,
    surfaceContainerHighest = SoilSunken,
    outline = ChalkMuted,
    outlineVariant = HairlineDark,
    error = ClayErrorDark,
    onError = Soil,
    errorContainer = ClayErrorContainerDark,
    onErrorContainer = Chalk
)

private val LightColorScheme = lightColorScheme(
    primary = Moss,
    onPrimary = Color.White,
    primaryContainer = MossContainerLight,
    onPrimaryContainer = Ink,
    secondary = InkSecondary,
    onSecondary = Color.White,
    secondaryContainer = PaperSunken,
    onSecondaryContainer = Ink,
    tertiary = InkSecondary,
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = PaperSunken,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = PaperRaised,
    surfaceContainerLow = PaperRaised,
    surfaceContainer = PaperRaised,
    surfaceContainerHigh = PaperSunken,
    surfaceContainerHighest = PaperSunken,
    outline = InkMuted,
    outlineVariant = HairlineLight,
    error = ClayError,
    onError = Color.White,
    errorContainer = ClayErrorContainerLight,
    onErrorContainer = Ink
)

/** Colors that are not part of Material's scheme. */
@Immutable
data class AppColors(
    val water: Color,
    val comfortGood: Color,
    val comfortWarn: Color,
    val comfortBad: Color
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(WaterBlue, ComfortGood, ComfortWarn, ComfortBad)
}

val MaterialTheme.appColors: AppColors
    @Composable get() = LocalAppColors.current

@Composable
fun SmartWindowsillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) {
        AppColors(WaterBlueOnDark, MossOnDark, Color(0xFFD9A441), ClayErrorDark)
    } else {
        AppColors(WaterBlue, ComfortGood, ComfortWarn, ComfortBad)
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
