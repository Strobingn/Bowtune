package com.strobingn.bowtune.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Grey90,
    onPrimary = Grey10,
    primaryContainer = Grey30,
    onPrimaryContainer = Grey95,
    secondary = Grey80,
    onSecondary = Grey10,
    secondaryContainer = Grey20,
    onSecondaryContainer = Grey90,
    tertiary = Grey70,
    onTertiary = Grey10,
    tertiaryContainer = WarningContainerDark,
    onTertiaryContainer = Grey90,
    error = Grey80,
    onError = Grey10,
    errorContainer = Grey20,
    onErrorContainer = Grey90,
    background = Grey10,
    onBackground = Grey95,
    surface = Grey10,
    onSurface = Grey95,
    surfaceVariant = Grey20,
    onSurfaceVariant = Grey80,
    outline = Grey50,
    outlineVariant = Grey30,
    inverseSurface = Grey90,
    inverseOnSurface = Grey10,
    inversePrimary = Grey40,
    scrim = Grey00
)

private val HighContrastDark = darkColorScheme(
    primary = ContrastPaper,
    onPrimary = ContrastInk,
    primaryContainer = Grey20,
    onPrimaryContainer = ContrastPaper,
    secondary = Grey90,
    onSecondary = ContrastInk,
    secondaryContainer = Grey15,
    onSecondaryContainer = ContrastPaper,
    tertiary = Grey80,
    onTertiary = ContrastInk,
    tertiaryContainer = Grey20,
    onTertiaryContainer = ContrastPaper,
    error = Grey90,
    onError = ContrastInk,
    errorContainer = Grey20,
    onErrorContainer = ContrastPaper,
    background = ContrastInk,
    onBackground = ContrastPaper,
    surface = ContrastDarkSurface,
    onSurface = ContrastPaper,
    surfaceVariant = Grey15,
    onSurfaceVariant = Grey95,
    outline = Grey90,
    outlineVariant = Grey50,
    inverseSurface = ContrastPaper,
    inverseOnSurface = ContrastInk,
    inversePrimary = Grey20,
    scrim = ContrastInk
)

private val HighContrastLight = lightColorScheme(
    primary = ContrastInk,
    onPrimary = ContrastPaper,
    primaryContainer = Grey90,
    onPrimaryContainer = ContrastInk,
    secondary = Grey20,
    onSecondary = ContrastPaper,
    secondaryContainer = Grey95,
    onSecondaryContainer = ContrastInk,
    tertiary = Grey30,
    onTertiary = ContrastPaper,
    tertiaryContainer = Grey90,
    onTertiaryContainer = ContrastInk,
    error = Grey20,
    onError = ContrastPaper,
    errorContainer = Grey90,
    onErrorContainer = ContrastInk,
    background = ContrastPaper,
    onBackground = ContrastInk,
    surface = ContrastLightSurface,
    onSurface = ContrastInk,
    surfaceVariant = Grey95,
    onSurfaceVariant = Grey15,
    outline = ContrastInk,
    outlineVariant = Grey40,
    inverseSurface = ContrastInk,
    inverseOnSurface = ContrastPaper,
    inversePrimary = Grey90,
    scrim = ContrastInk
)

private val LightColors = lightColorScheme(
    primary = Grey20,
    onPrimary = Grey100,
    primaryContainer = Grey90,
    onPrimaryContainer = Grey10,
    secondary = Grey40,
    onSecondary = Grey100,
    secondaryContainer = Grey95,
    onSecondaryContainer = Grey15,
    tertiary = Grey50,
    onTertiary = Grey100,
    tertiaryContainer = WarningContainerLight,
    onTertiaryContainer = Grey15,
    error = Grey40,
    onError = Grey100,
    errorContainer = Grey95,
    onErrorContainer = Grey15,
    background = Grey99,
    onBackground = Grey10,
    surface = Grey99,
    onSurface = Grey10,
    surfaceVariant = Grey95,
    onSurfaceVariant = Grey40,
    outline = Grey60,
    outlineVariant = Grey80,
    inverseSurface = Grey20,
    inverseOnSurface = Grey95,
    inversePrimary = Grey80,
    scrim = Grey00
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
        highContrast && darkTheme -> HighContrastDark
        highContrast && !darkTheme -> HighContrastLight
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(largeText),
        content = content
    )
}
