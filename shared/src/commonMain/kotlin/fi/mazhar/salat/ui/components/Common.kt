package fi.mazhar.salat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.mazhar.salat.ui.theme.NoorColors

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    background: Color = NoorColors.Surface,
    border: Color = NoorColors.Outline.copy(alpha = 0.6f),
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(20.dp))
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NoorColors.Emerald,
    background: Color = color.copy(alpha = 0.15f),
) {
    Text(
        text,
        modifier
            .background(background, RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun Eyebrow(text: String, color: Color = NoorColors.Emerald) {
    Text(
        text.uppercase(),
        color = color,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null, maxLines: Int = Int.MAX_VALUE) {
    Text(
        text,
        modifier,
        color = NoorColors.TextMuted,
        style = MaterialTheme.typography.bodySmall,
        textAlign = textAlign,
        maxLines = maxLines,
    )
}

/** Rounded progress track; drawn by hand so the look is identical on every target. */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = NoorColors.Emerald,
    track: Color = NoorColors.SurfaceRaised,
    glow: Boolean = true,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(track, RoundedCornerShape(999.dp)),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        if (glow) Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)) else Brush.horizontalGradient(listOf(color, color)),
                        RoundedCornerShape(999.dp),
                    ),
            )
        }
    }
}

/** Arabic script is always laid out right-to-left and given generous line height. */
@Composable
fun ArabicText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 26.sp,
    color: Color = NoorColors.AmberLight,
    textAlign: TextAlign = TextAlign.Center,
    maxLines: Int = Int.MAX_VALUE,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text,
            modifier.fillMaxWidth(),
            color = color,
            fontSize = fontSize,
            lineHeight = fontSize * 1.9f,
            textAlign = textAlign,
            maxLines = maxLines,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif),
        )
    }
}

@Composable
fun StatTile(label: String, value: String, hint: String, modifier: Modifier = Modifier, accent: Color = NoorColors.Emerald) {
    SectionCard(modifier, padding = PaddingValues(14.dp)) {
        Muted(label)
        Text(
            value,
            color = NoorColors.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        Text(hint, color = accent, style = MaterialTheme.typography.labelSmall)
    }
}

/** A labelled dropdown that renders the same on phone and desktop. */
@Composable
fun <T> OptionPicker(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Muted(label, Modifier.padding(bottom = 6.dp))
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(NoorColors.SurfaceRaised, RoundedCornerShape(12.dp))
                    .border(1.dp, NoorColors.Outline, RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(optionLabel(selected), color = NoorColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text("▾", color = NoorColors.TextMuted)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                optionLabel(option),
                                color = if (option == selected) NoorColors.Emerald else NoorColors.TextPrimary,
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
    }
}
