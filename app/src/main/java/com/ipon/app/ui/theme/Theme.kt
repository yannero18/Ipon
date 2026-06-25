package com.ipon.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Ipon is intentionally light-mode-only at the design-system level for v1.
 * Section 4's palette (rice paper background, kape brown text) is built
 * around a warm, low-contrast print-lookbook feel that does not translate
 * to a simple inverted dark theme -- a true dark mode would need its own
 * considered palette, not an automatic Material "dark" flip. Out of scope
 * for this prototype; flagged here so it is not silently forgotten.
 */
private val IponColorScheme = lightColorScheme(
    primary = OceanTeal,
    onPrimary = RicePaper,
    secondary = JeepneyOrange,
    onSecondary = RicePaper,
    error = Terracotta,
    onError = RicePaper,
    background = RicePaper,
    onBackground = KapeBrown,
    surface = Color.White,
    onSurface = KapeBrown,
    surfaceVariant = RicePaperDeep,
    onSurfaceVariant = KapeBrownSoft,
    outline = HairlineBorder
)

@Composable
fun IponTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IponColorScheme,
        typography = IponTypography,
        content = content
    )
}
