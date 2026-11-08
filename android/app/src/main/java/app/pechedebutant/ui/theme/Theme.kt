package app.pechedebutant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pêche Débutant v0.1 palette: sea blue, sand, light theme following the system.
val SeaBlue = Color(0xFF0B5A7A)
val SeaBlueLight = Color(0xFF2E86AB)
val Sand = Color(0xFFE8D9A8)
val SandDark = Color(0xFFC9A86A)

private val LightColors = lightColorScheme(
    primary = SeaBlue,
    onPrimary = Color.White,
    secondary = SeaBlueLight,
    onSecondary = Color.White,
    tertiary = SandDark,
    background = Color(0xFFF6F3EA),
    onBackground = Color(0xFF14201F),
    surface = Color.White,
    onSurface = Color(0xFF14201F),
    surfaceVariant = Color(0xFFEDE8D6),
    onSurfaceVariant = Color(0xFF5A5A52),
    outline = Color(0xFFD8D2BE)
)

private val DarkColors = darkColorScheme(
    primary = SeaBlueLight,
    onPrimary = Color(0xFF06222E),
    secondary = SeaBlueLight,
    onSecondary = Color(0xFF06222E),
    tertiary = Sand,
    background = Color(0xFF0A1E26),
    onBackground = Color(0xFFF2F2EC),
    surface = Color(0xFF123240),
    onSurface = Color(0xFFF2F2EC),
    surfaceVariant = Color(0xFF1D4254),
    onSurfaceVariant = Color(0xFFC8C8BC),
    outline = Color(0xFF3A5A68)
)

@Composable
fun PecheDebutantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
