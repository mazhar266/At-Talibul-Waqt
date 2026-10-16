package fi.mazhar.salat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import talibulwaqt.shared.generated.resources.Res
import talibulwaqt.shared.generated.resources.amiri_bold
import talibulwaqt.shared.generated.resources.amiri_regular

/**
 * Amiri 1.003 (https://github.com/aliftype/amiri), the Naskh face bundled under composeResources/font,
 * so Arabic renders identically on Android, iOS and desktop instead of falling back to whichever
 * serif the system happens to have. It is under the SIL Open Font License; the licence text ships
 * with the app as composeResources/files/Amiri-OFL.txt (kept out of font/ so the resource generator
 * does not mistake it for a typeface).
 *
 * Fonts load synchronously on every non-web target, so the first frame already uses Amiri.
 */
@Composable
fun rememberArabicFontFamily(): FontFamily {
    val regular = Font(Res.font.amiri_regular, FontWeight.Normal)
    val bold = Font(Res.font.amiri_bold, FontWeight.Bold)
    return remember(regular, bold) { FontFamily(regular, bold) }
}
