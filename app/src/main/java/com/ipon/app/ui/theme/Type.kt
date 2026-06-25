package com.ipon.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * TODO (before shipping): swap these system-font fallbacks for the real
 * Fraunces / Inter / Roboto Mono families described in Section 4.
 *
 * This prototype ships with zero bundled .ttf files -- adding the actual
 * webfonts requires downloading them from Google Fonts and placing them in
 * res/font/, which this environment has no network access to do. Wiring
 * fake R.font.* references that don't point at real files would fail the
 * build outright, which is worse than an honest fallback. FontFamily.Serif
 * and FontFamily.SansSerif below are real Android system fonts, so the app
 * builds and runs today; once you add the actual font files locally, swap
 * the three FontFamily values below for FontFamily(Font(R.font.xxx, ...))
 * as in the commented example at the bottom of this file.
 */
val FraunceFamily = FontFamily.Serif
val InterFamily = FontFamily.SansSerif
val MonoFamily = FontFamily.Monospace

/**
 * Section 4: "all financial figures are forced into a rigid typography grid
 * using Monospaced Tabular Figures (tnum)". In Compose this is expressed as
 * a TextStyle with a monospace digit font + explicit fontFeatureSettings,
 * since not every monospace family ships tabular figures by default --
 * "tnum" is the OpenType feature tag that forces it even on variable fonts
 * that default to proportional figures.
 */
val TabularNumberStyle = TextStyle(
    fontFamily = MonoFamily,
    fontFeatureSettings = "tnum"
)

val IponTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FraunceFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FraunceFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

/*
Example of the real wiring once you've added .ttf files to res/font/:

import androidx.compose.ui.text.font.Font
import com.ipon.app.R

val FraunceFamily = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_medium, FontWeight.Medium)
)
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)
val MonoFamily = FontFamily(
    Font(R.font.roboto_mono_regular, FontWeight.Normal),
    Font(R.font.roboto_mono_medium, FontWeight.Medium)
)
*/
