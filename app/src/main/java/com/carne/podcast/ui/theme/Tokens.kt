package com.carne.podcast.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Carne's design-system tokens — the named values the UI is built from.
 *
 * Material 3 (via [CarneTheme]) still owns the base color scheme, typography and
 * motion. These three token sets layer the *brand* on top: a fixed chili-red
 * identity, a consistent spacing rhythm and a shared shape vocabulary, so a
 * change here ripples to every screen instead of being re-tuned per call site.
 */

/** Brand-specific semantic colors that Material's [androidx.compose.material3.ColorScheme] doesn't model. */
@Immutable
data class CarneColors(
    /** The chili-red signature accent — stays constant even under Material You dynamic color. */
    val brand: Color,
    val onBrand: Color,
    /** Warm secondary accent used for highlights. */
    val ember: Color,
    /** "Played / finished" affordance tint. */
    val played: Color,
    /** "Downloaded for offline" affordance tint. */
    val downloaded: Color,
)

/** Consistent spacing scale (4dp rhythm). Use these instead of raw dp paddings. */
@Immutable
data class CarneSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

/** Shared shape vocabulary for artwork and surfaces. */
@Immutable
data class CarneShapes(
    val artworkSmall: RoundedCornerShape = RoundedCornerShape(8.dp),
    val artworkMedium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val artworkLarge: RoundedCornerShape = RoundedCornerShape(24.dp),
)

internal val LocalCarneColors = staticCompositionLocalOf<CarneColors> {
    error("CarneColors not provided — wrap content in CarneTheme { }")
}
internal val LocalCarneSpacing = staticCompositionLocalOf { CarneSpacing() }
internal val LocalCarneShapes = staticCompositionLocalOf { CarneShapes() }
