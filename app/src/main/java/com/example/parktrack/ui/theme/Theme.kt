
package com.example.parktrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = BackgroundColorDark,
    surface = SurfaceColorDark,
    error = ErrorColor,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = BackgroundColorLight,
    surface = SurfaceColorLight,
    error = ErrorColor,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6C6C70)
)

@Composable
fun ParkTrackTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}