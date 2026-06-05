package com.carne.podcast.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ChiliRed = Color(0xFFB4231C)
private val ChiliRedLight = Color(0xFFFF5944)
private val Ember = Color(0xFFE8A13C)
private val EmberLight = Color(0xFFFFC46B)

private val DarkColors = darkColorScheme(
    primary = ChiliRedLight,
    onPrimary = Color(0xFF3A0A06),
    secondary = Ember,
    background = Color(0xFF161210),
    surface = Color(0xFF1F1A18),
    surfaceVariant = Color(0xFF332B28),
)

private val LightColors = lightColorScheme(
    primary = ChiliRed,
    onPrimary = Color.White,
    secondary = Color(0xFFB06A12),
    background = Color(0xFFFFF8F6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3E7E3),
)

private val DarkCarneColors = CarneColors(
    brand = ChiliRedLight,
    onBrand = Color(0xFF3A0A06),
    ember = EmberLight,
    played = ChiliRedLight,
    downloaded = EmberLight,
)

private val LightCarneColors = CarneColors(
    brand = ChiliRed,
    onBrand = Color.White,
    ember = Ember,
    played = ChiliRed,
    downloaded = Color(0xFFB06A12),
)

/**
 * Carne's theme entry point. Wraps [MaterialExpressiveTheme] — which supplies
 * the Material 3 Expressive spring-based [MotionScheme] and component defaults —
 * and provides the brand token layer ([CarneColors], [CarneSpacing], [CarneShapes])
 * on top.
 *
 * Material You dynamic color is kept on by default; the chili-red brand tokens
 * stay fixed so the identity reads through regardless of the device wallpaper.
 */
@Composable
fun CarneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val carneColors = if (darkTheme) DarkCarneColors else LightCarneColors

    CompositionLocalProvider(
        LocalCarneColors provides carneColors,
        LocalCarneSpacing provides CarneSpacing(),
        LocalCarneShapes provides CarneShapes(),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

/**
 * Accessor for Carne tokens, mirroring how [MaterialTheme] exposes its subsystems
 * (e.g. `CarneTheme.colors.brand`). A function and an object can share a name in
 * Kotlin, so `CarneTheme { }` and `CarneTheme.colors` coexist.
 */
object CarneTheme {
    val colors: CarneColors
        @Composable @ReadOnlyComposable get() = LocalCarneColors.current
    val spacing: CarneSpacing
        @Composable @ReadOnlyComposable get() = LocalCarneSpacing.current
    val shapes: CarneShapes
        @Composable @ReadOnlyComposable get() = LocalCarneShapes.current
}
