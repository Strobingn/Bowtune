package com.strobingn.bowtune.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class GrayscaleThemeTest {
    private fun roles(cs: ColorScheme): Map<String, Color> = mapOf(
        "primary" to cs.primary,
        "onPrimary" to cs.onPrimary,
        "primaryContainer" to cs.primaryContainer,
        "onPrimaryContainer" to cs.onPrimaryContainer,
        "inversePrimary" to cs.inversePrimary,
        "secondary" to cs.secondary,
        "onSecondary" to cs.onSecondary,
        "secondaryContainer" to cs.secondaryContainer,
        "onSecondaryContainer" to cs.onSecondaryContainer,
        "tertiary" to cs.tertiary,
        "onTertiary" to cs.onTertiary,
        "tertiaryContainer" to cs.tertiaryContainer,
        "onTertiaryContainer" to cs.onTertiaryContainer,
        "background" to cs.background,
        "onBackground" to cs.onBackground,
        "surface" to cs.surface,
        "onSurface" to cs.onSurface,
        "surfaceVariant" to cs.surfaceVariant,
        "onSurfaceVariant" to cs.onSurfaceVariant,
        "surfaceTint" to cs.surfaceTint,
        "inverseSurface" to cs.inverseSurface,
        "inverseOnSurface" to cs.inverseOnSurface,
        "error" to cs.error,
        "onError" to cs.onError,
        "errorContainer" to cs.errorContainer,
        "onErrorContainer" to cs.onErrorContainer,
        "outline" to cs.outline,
        "outlineVariant" to cs.outlineVariant,
        "scrim" to cs.scrim,
        "surfaceBright" to cs.surfaceBright,
        "surfaceDim" to cs.surfaceDim,
        "surfaceContainer" to cs.surfaceContainer,
        "surfaceContainerHigh" to cs.surfaceContainerHigh,
        "surfaceContainerHighest" to cs.surfaceContainerHighest,
        "surfaceContainerLow" to cs.surfaceContainerLow,
        "surfaceContainerLowest" to cs.surfaceContainerLowest
    )

    private fun Color.isGrey(): Boolean {
        val r = (red * 255).toInt()
        val g = (green * 255).toInt()
        val b = (blue * 255).toInt()
        return r == g && g == b
    }

    @Test
    fun everyShippedColorRoleIsPureGrey() {
        val failures = mutableListOf<String>()
        for (dark in listOf(false, true)) {
            for (contrast in listOf(false, true)) {
                roles(bowTuneColorScheme(dark, contrast)).forEach { (name, color) ->
                    if (!color.isGrey()) failures += "dark=$dark contrast=$contrast $name=$color"
                }
            }
        }
        assertTrue("Chromatic roles found:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun paletteConstantsAreGrey() {
        val palette = listOf(
            Grey00, Grey05, Grey07, Grey08, Grey10, Grey12, Grey15, Grey17, Grey20, Grey22, Grey25, Grey30,
            Grey40, Grey50, Grey60, Grey70, Grey80, Grey85, Grey88, Grey90, Grey93, Grey95, Grey97, Grey99,
            Grey100, WarningContainerDark, WarningContainerLight, ContrastInk, ContrastPaper,
            ContrastDarkSurface, ContrastLightSurface
        )
        assertTrue(palette.all { it.isGrey() })
    }
}
