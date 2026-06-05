package com.carne.podcast.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ChiliRed = Color(0xFFB4231C)
private val ChiliRedLight = Color(0xFFFF5944)
private val Ember = Color(0xFFE8A13C)

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

@Composable
fun CarneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
