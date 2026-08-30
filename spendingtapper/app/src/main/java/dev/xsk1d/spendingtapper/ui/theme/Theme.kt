package dev.xsk1d.spendingtapper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Amber = Color(0xFFF5C542)
private val AmberDark = Color(0xFF8A6A00)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF221B00),
    secondary = Color(0xFFD5C5A1),
    background = Color(0xFF12100E),
    onBackground = Color(0xFFEDE5DA),
    surface = Color(0xFF12100E),
    onSurface = Color(0xFFEDE5DA),
    surfaceVariant = Color(0xFF2A2621),
    onSurfaceVariant = Color(0xFFCFC6BA),
    error = Color(0xFFFF8A80),
)

private val LightColors = lightColorScheme(
    primary = AmberDark,
    onPrimary = Color.White,
    secondary = Color(0xFF6B5D3F),
    background = Color(0xFFFDFAF3),
    onBackground = Color(0xFF1D1B16),
    surface = Color(0xFFFDFAF3),
    onSurface = Color(0xFF1D1B16),
    surfaceVariant = Color(0xFFECE3D2),
    onSurfaceVariant = Color(0xFF4C463A),
    error = Color(0xFFB3261E),
)

/** Amber-on-near-black by default; follows One UI's Material You palette when available. */
@Composable
fun SpendingTapperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        // minSdk 33 is well past the API 31 that introduced dynamic colour.
        dynamicColor ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpendingTapperTypography,
        content = content,
    )
}
