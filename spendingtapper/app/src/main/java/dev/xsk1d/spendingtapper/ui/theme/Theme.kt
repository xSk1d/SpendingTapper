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

// Selection state is drawn from the *container* roles. Leaving them at the Material
// baseline meant "which one is picked" rendered in default purple over amber-on-black,
// so every role a segmented button, chip or filled button reads from is set here.
private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF221B00),
    primaryContainer = Color(0xFF5A4700),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Color(0xFFD5C5A1),
    onSecondary = Color(0xFF383020),
    secondaryContainer = Color(0xFF554824),
    onSecondaryContainer = Color(0xFFFFE49B),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF633F00),
    onTertiaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF12100E),
    onBackground = Color(0xFFEDE5DA),
    surface = Color(0xFF12100E),
    onSurface = Color(0xFFEDE5DA),
    surfaceVariant = Color(0xFF2A2621),
    onSurfaceVariant = Color(0xFFCFC6BA),
    outline = Color(0xFF9A9083),
    outlineVariant = Color(0xFF4A443B),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF4A0A05),
    errorContainer = Color(0xFF7A1810),
    onErrorContainer = Color(0xFFFFDAD5),
)

private val LightColors = lightColorScheme(
    primary = AmberDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE08A),
    onPrimaryContainer = Color(0xFF2A2000),
    secondary = Color(0xFF6B5D3F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E3B8),
    onSecondaryContainer = Color(0xFF2A2000),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF2C1700),
    background = Color(0xFFFDFAF3),
    onBackground = Color(0xFF1D1B16),
    surface = Color(0xFFFDFAF3),
    onSurface = Color(0xFF1D1B16),
    surfaceVariant = Color(0xFFECE3D2),
    onSurfaceVariant = Color(0xFF4C463A),
    outline = Color(0xFF7C7566),
    outlineVariant = Color(0xFFD6CDBA),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

/**
 * Amber-on-near-black, and only Material You if it is asked for explicitly.
 *
 * This used to default [dynamicColor] to true, which meant the branch below always took
 * the wallpaper-derived palette and the two schemes above were unreachable. The app
 * inherited whatever contrast One UI happened to derive, which is how selected states
 * ended up invisible on some wallpapers.
 */
@Composable
fun SpendingTapperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
