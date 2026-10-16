package fi.mazhar.salat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Palette lifted from the NoorSalat web app (Tailwind slate / emerald / amber). */
object NoorColors {
    val Background = Color(0xFF020617)
    val Surface = Color(0xFF0F172A)
    val SurfaceRaised = Color(0xFF1E293B)
    val Outline = Color(0xFF334155)
    val Emerald = Color(0xFF10B981)
    val EmeraldDeep = Color(0xFF064E3B)
    val EmeraldLight = Color(0xFF6EE7B7)
    val EmeraldGlow = Color(0xFF34D399)
    val Amber = Color(0xFFFBBF24)
    val AmberLight = Color(0xFFFCD34D)
    val AmberDeep = Color(0xFF78350F)
    val Rose = Color(0xFFFB7185)
    val Sky = Color(0xFF60A5FA)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextMuted = Color(0xFF94A3B8)
    val Danger = Color(0xFFF87171)
}

private val colorScheme = darkColorScheme(
    primary = NoorColors.Emerald,
    onPrimary = Color.White,
    primaryContainer = NoorColors.EmeraldDeep,
    onPrimaryContainer = NoorColors.EmeraldLight,
    secondary = NoorColors.EmeraldLight,
    onSecondary = NoorColors.Background,
    secondaryContainer = NoorColors.SurfaceRaised,
    onSecondaryContainer = NoorColors.TextPrimary,
    tertiary = NoorColors.Amber,
    onTertiary = NoorColors.Background,
    tertiaryContainer = NoorColors.AmberDeep,
    onTertiaryContainer = NoorColors.AmberLight,
    background = NoorColors.Background,
    onBackground = NoorColors.TextPrimary,
    surface = NoorColors.Surface,
    onSurface = NoorColors.TextPrimary,
    surfaceVariant = NoorColors.SurfaceRaised,
    onSurfaceVariant = NoorColors.TextMuted,
    surfaceContainer = NoorColors.Surface,
    surfaceContainerHigh = NoorColors.SurfaceRaised,
    surfaceContainerHighest = NoorColors.SurfaceRaised,
    surfaceContainerLow = NoorColors.Surface,
    surfaceContainerLowest = NoorColors.Background,
    outline = NoorColors.Outline,
    outlineVariant = NoorColors.Outline,
    error = NoorColors.Danger,
    onError = NoorColors.Background,
)

@Composable
fun TalibulWaqtTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
