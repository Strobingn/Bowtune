package com.strobingn.bowtune.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Every role is set explicitly: Material 3 fills unspecified roles (surfaceContainer*, surfaceTint,
// surfaceBright/Dim) from its baseline palette, which is tinted purple.
private val DarkColors = darkColorScheme(
    primary = Grey90,
    onPrimary = Grey10,
    primaryContainer = Grey30,
    onPrimaryContainer = Grey95,
    inversePrimary = Grey40,
    secondary = Grey80,
    onSecondary = Grey10,
    secondaryContainer = Grey25,
    onSecondaryContainer = Grey90,
    tertiary = Grey70,
    onTertiary = Grey10,
    tertiaryContainer = WarningContainerDark,
    onTertiaryContainer = Grey90,
    background = Grey07,
    onBackground = Grey95,
    surface = Grey07,
    onSurface = Grey95,
    surfaceVariant = Grey20,
    onSurfaceVariant = Grey80,
    surfaceTint = Grey90,
    inverseSurface = Grey90,
    inverseOnSurface = Grey10,
    error = Grey80,
    onError = Grey10,
    errorContainer = Grey20,
    onErrorContainer = Grey90,
    outline = Grey50,
    outlineVariant = Grey30,
    scrim = Grey00,
    surfaceBright = Grey25,
    surfaceContainer = Grey12,
    surfaceContainerHigh = Grey17,
    surfaceContainerHighest = Grey22,
    surfaceContainerLow = Grey10,
    surfaceContainerLowest = Grey05,
    surfaceDim = Grey07
)

private val HighContrastDark = darkColorScheme(
    primary = ContrastPaper,
    onPrimary = ContrastInk,
    primaryContainer = Grey20,
    onPrimaryContainer = ContrastPaper,
    inversePrimary = Grey20,
    secondary = Grey90,
    onSecondary = ContrastInk,
    secondaryContainer = Grey15,
    onSecondaryContainer = ContrastPaper,
    tertiary = Grey80,
    onTertiary = ContrastInk,
    tertiaryContainer = Grey20,
    onTertiaryContainer = ContrastPaper,
    background = ContrastInk,
    onBackground = ContrastPaper,
    surface = ContrastDarkSurface,
    onSurface = ContrastPaper,
    surfaceVariant = Grey15,
    onSurfaceVariant = Grey95,
    surfaceTint = ContrastPaper,
    inverseSurface = ContrastPaper,
    inverseOnSurface = ContrastInk,
    error = Grey90,
    onError = ContrastInk,
    errorContainer = Grey20,
    onErrorContainer = ContrastPaper,
    outline = Grey90,
    outlineVariant = Grey50,
    scrim = ContrastInk,
    surfaceBright = Grey20,
    surfaceContainer = Grey08,
    surfaceContainerHigh = Grey12,
    surfaceContainerHighest = Grey15,
    surfaceContainerLow = Grey05,
    surfaceContainerLowest = ContrastInk,
    surfaceDim = ContrastInk
)

private val HighContrastLight = lightColorScheme(
    primary = ContrastInk,
    onPrimary = ContrastPaper,
    primaryContainer = Grey90,
    onPrimaryContainer = ContrastInk,
    inversePrimary = Grey90,
    secondary = Grey20,
    onSecondary = ContrastPaper,
    secondaryContainer = Grey95,
    onSecondaryContainer = ContrastInk,
    tertiary = Grey30,
    onTertiary = ContrastPaper,
    tertiaryContainer = Grey90,
    onTertiaryContainer = ContrastInk,
    background = ContrastPaper,
    onBackground = ContrastInk,
    surface = ContrastLightSurface,
    onSurface = ContrastInk,
    surfaceVariant = Grey95,
    onSurfaceVariant = Grey15,
    surfaceTint = ContrastInk,
    inverseSurface = ContrastInk,
    inverseOnSurface = ContrastPaper,
    error = Grey20,
    onError = ContrastPaper,
    errorContainer = Grey90,
    onErrorContainer = ContrastInk,
    outline = ContrastInk,
    outlineVariant = Grey40,
    scrim = ContrastInk,
    surfaceBright = ContrastPaper,
    surfaceContainer = Grey93,
    surfaceContainerHigh = Grey95,
    surfaceContainerHighest = Grey88,
    surfaceContainerLow = Grey97,
    surfaceContainerLowest = ContrastPaper,
    surfaceDim = Grey85
)

private val LightColors = lightColorScheme(
    primary = Grey20,
    onPrimary = Grey100,
    primaryContainer = Grey90,
    onPrimaryContainer = Grey10,
    inversePrimary = Grey80,
    secondary = Grey40,
    onSecondary = Grey100,
    secondaryContainer = Grey88,
    onSecondaryContainer = Grey15,
    tertiary = Grey50,
    onTertiary = Grey100,
    tertiaryContainer = WarningContainerLight,
    onTertiaryContainer = Grey15,
    background = Grey99,
    onBackground = Grey10,
    surface = Grey99,
    onSurface = Grey10,
    surfaceVariant = Grey95,
    onSurfaceVariant = Grey40,
    surfaceTint = Grey20,
    inverseSurface = Grey20,
    inverseOnSurface = Grey95,
    error = Grey40,
    onError = Grey100,
    errorContainer = Grey95,
    onErrorContainer = Grey15,
    outline = Grey60,
    outlineVariant = Grey80,
    scrim = Grey00,
    surfaceBright = Grey100,
    surfaceContainer = Grey93,
    surfaceContainerHigh = Grey95,
    surfaceContainerHighest = Grey88,
    surfaceContainerLow = Grey97,
    surfaceContainerLowest = Grey100,
    surfaceDim = Grey85
)

/** The non-dynamic schemes the app actually ships. All roles are pure greys. */
fun bowTuneColorScheme(darkTheme: Boolean, highContrast: Boolean): ColorScheme = when {
    highContrast && darkTheme -> HighContrastDark
    highContrast -> HighContrastLight
    darkTheme -> DarkColors
    else -> LightColors
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BowTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disabled by default so Material You cannot inject chromatic colors.
    dynamicColor: Boolean = false,
    highContrast: Boolean = false,
    largeText: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        !highContrast && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> bowTuneColorScheme(darkTheme, highContrast)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(largeText),
        shapes = AppShapes,
        content = content
    )
}
