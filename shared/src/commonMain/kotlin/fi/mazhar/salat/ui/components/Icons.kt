package fi.mazhar.salat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.mazhar.salat.model.Prayer
import fi.mazhar.salat.ui.theme.NoorColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Hand-drawn line icons so the shared module needs no icon-font dependency.

private fun DrawScope.strokeWidthFor(size: Size) = size.minDimension / 11f

@Composable
fun ClockIcon(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) = Canvas(modifier.size(size)) {
    val w = strokeWidthFor(this.size)
    val r = this.size.minDimension / 2 - w
    drawCircle(tint, r, center, style = Stroke(w))
    drawLine(tint, center, center + Offset(0f, -r * 0.55f), w, StrokeCap.Round)
    drawLine(tint, center, center + Offset(r * 0.4f, r * 0.2f), w, StrokeCap.Round)
}

@Composable
fun CompassIcon(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) = Canvas(modifier.size(size)) {
    val w = strokeWidthFor(this.size)
    val r = this.size.minDimension / 2 - w
    drawCircle(tint, r, center, style = Stroke(w))
    val needle = Path().apply {
        moveTo(center.x, center.y - r * 0.6f)
        lineTo(center.x + r * 0.22f, center.y)
        lineTo(center.x, center.y + r * 0.6f)
        lineTo(center.x - r * 0.22f, center.y)
        close()
    }
    rotate(35f, center) { drawPath(needle, tint, style = Stroke(w * 0.9f)) }
}

@Composable
fun ListIcon(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) = Canvas(modifier.size(size)) {
    val w = strokeWidthFor(this.size)
    val s = this.size.minDimension
    for (i in 0..2) {
        val y = s * (0.25f + i * 0.25f)
        drawCircle(tint, w * 0.9f, Offset(s * 0.17f, y))
        drawLine(tint, Offset(s * 0.36f, y), Offset(s * 0.88f, y), w, StrokeCap.Round)
    }
}

@Composable
fun BookIcon(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) = Canvas(modifier.size(size)) {
    val w = strokeWidthFor(this.size)
    val s = this.size.minDimension
    val page = Path().apply {
        moveTo(s * 0.5f, s * 0.25f)
        quadraticTo(s * 0.3f, s * 0.14f, s * 0.1f, s * 0.22f)
        lineTo(s * 0.1f, s * 0.82f)
        quadraticTo(s * 0.3f, s * 0.74f, s * 0.5f, s * 0.86f)
        quadraticTo(s * 0.7f, s * 0.74f, s * 0.9f, s * 0.82f)
        lineTo(s * 0.9f, s * 0.22f)
        quadraticTo(s * 0.7f, s * 0.14f, s * 0.5f, s * 0.25f)
        close()
    }
    drawPath(page, tint, style = Stroke(w))
    drawLine(tint, Offset(s * 0.5f, s * 0.25f), Offset(s * 0.5f, s * 0.86f), w * 0.8f)
}

@Composable
fun SlidersIcon(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) = Canvas(modifier.size(size)) {
    val w = strokeWidthFor(this.size)
    val s = this.size.minDimension
    val knobs = listOf(0.3f, 0.7f, 0.45f)
    for (i in 0..2) {
        val y = s * (0.25f + i * 0.25f)
        drawLine(tint, Offset(s * 0.12f, y), Offset(s * 0.88f, y), w, StrokeCap.Round)
        drawCircle(NoorColors.Background, w * 1.6f, Offset(s * knobs[i], y))
        drawCircle(tint, w * 1.6f, Offset(s * knobs[i], y), style = Stroke(w))
    }
}

@Composable
fun PinIcon(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) = Canvas(modifier.size(size)) {
    val w = strokeWidthFor(this.size)
    val s = this.size.minDimension
    val pin = Path().apply {
        moveTo(s * 0.5f, s * 0.92f)
        cubicTo(s * 0.5f, s * 0.92f, s * 0.16f, s * 0.55f, s * 0.16f, s * 0.38f)
        cubicTo(s * 0.16f, s * 0.19f, s * 0.31f, s * 0.08f, s * 0.5f, s * 0.08f)
        cubicTo(s * 0.69f, s * 0.08f, s * 0.84f, s * 0.19f, s * 0.84f, s * 0.38f)
        cubicTo(s * 0.84f, s * 0.55f, s * 0.5f, s * 0.92f, s * 0.5f, s * 0.92f)
        close()
    }
    drawPath(pin, tint, style = Stroke(w))
    drawCircle(tint, s * 0.1f, Offset(s * 0.5f, s * 0.38f), style = Stroke(w))
}

@Composable
fun BellIcon(tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp, muted: Boolean = false) =
    Canvas(modifier.size(size)) {
        val w = strokeWidthFor(this.size)
        val s = this.size.minDimension
        val bell = Path().apply {
            moveTo(s * 0.2f, s * 0.7f)
            lineTo(s * 0.8f, s * 0.7f)
            lineTo(s * 0.72f, s * 0.58f)
            lineTo(s * 0.72f, s * 0.4f)
            cubicTo(s * 0.72f, s * 0.25f, s * 0.62f, s * 0.15f, s * 0.5f, s * 0.15f)
            cubicTo(s * 0.38f, s * 0.15f, s * 0.28f, s * 0.25f, s * 0.28f, s * 0.4f)
            lineTo(s * 0.28f, s * 0.58f)
            close()
        }
        drawPath(bell, tint, style = Stroke(w))
        drawLine(tint, Offset(s * 0.42f, s * 0.82f), Offset(s * 0.58f, s * 0.82f), w, StrokeCap.Round)
        if (muted) drawLine(tint, Offset(s * 0.15f, s * 0.15f), Offset(s * 0.85f, s * 0.85f), w, StrokeCap.Round)
    }

@Composable
fun KaabaIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) = Canvas(modifier.size(size)) {
    val s = this.size.minDimension
    drawRoundRect(NoorColors.Amber, Offset(s * 0.12f, s * 0.12f), Size(s * 0.76f, s * 0.76f),
        androidx.compose.ui.geometry.CornerRadius(s * 0.12f))
    drawRect(Color(0xFF111827), Offset(s * 0.2f, s * 0.2f), Size(s * 0.6f, s * 0.6f))
    drawRect(NoorColors.Amber, Offset(s * 0.2f, s * 0.38f), Size(s * 0.6f, s * 0.06f))
    drawRect(NoorColors.Amber, Offset(s * 0.56f, s * 0.44f), Size(s * 0.08f, s * 0.36f))
}

/** A small weather-style glyph for each prayer, matching the web app's sun/moon markers. */
@Composable
fun PrayerGlyph(prayer: Prayer, modifier: Modifier = Modifier, size: Dp = 26.dp) = Canvas(modifier.size(size)) {
    val s = this.size.minDimension
    val w = s / 12f
    val c = center
    when (prayer) {
        Prayer.FAJR -> {
            val tint = NoorColors.Sky
            drawArc(tint, 180f, 180f, false, Offset(c.x - s * 0.26f, c.y - s * 0.26f + s * 0.1f), Size(s * 0.52f, s * 0.52f), style = Stroke(w))
            drawLine(tint, Offset(s * 0.1f, c.y + s * 0.1f), Offset(s * 0.9f, c.y + s * 0.1f), w, StrokeCap.Round)
            drawLine(tint, Offset(c.x, c.y - s * 0.45f), Offset(c.x, c.y - s * 0.3f), w, StrokeCap.Round)
        }
        Prayer.SUNRISE -> {
            val tint = NoorColors.Amber
            drawArc(tint, 180f, 180f, false, Offset(c.x - s * 0.24f, c.y - s * 0.24f + s * 0.1f), Size(s * 0.48f, s * 0.48f), style = Stroke(w))
            drawLine(tint, Offset(s * 0.1f, c.y + s * 0.1f), Offset(s * 0.9f, c.y + s * 0.1f), w, StrokeCap.Round)
            rays(tint, c + Offset(0f, s * 0.1f), s * 0.3f, s * 0.42f, w, from = 200f, to = 340f, count = 4)
        }
        Prayer.DHUHR -> {
            val tint = NoorColors.AmberLight
            drawCircle(tint, s * 0.2f, c, style = Stroke(w))
            rays(tint, c, s * 0.3f, s * 0.44f, w, from = 0f, to = 360f, count = 8)
        }
        Prayer.ASR -> {
            val tint = NoorColors.Amber
            drawCircle(tint, s * 0.18f, c + Offset(0f, s * 0.05f), style = Stroke(w))
            rays(tint, c + Offset(0f, s * 0.05f), s * 0.28f, s * 0.4f, w, from = 180f, to = 360f, count = 5)
        }
        Prayer.MAGHRIB -> {
            val tint = NoorColors.Rose
            drawArc(tint, 180f, 180f, false, Offset(c.x - s * 0.24f, c.y - s * 0.24f + s * 0.12f), Size(s * 0.48f, s * 0.48f), style = Stroke(w))
            drawLine(tint, Offset(s * 0.1f, c.y + s * 0.12f), Offset(s * 0.9f, c.y + s * 0.12f), w, StrokeCap.Round)
            drawLine(tint, Offset(c.x, c.y - s * 0.3f), Offset(c.x, c.y - s * 0.45f), w, StrokeCap.Round)
        }
        Prayer.ISHA -> {
            val tint = NoorColors.Sky
            drawArc(tint, 40f, 260f, false, Offset(c.x - s * 0.3f, c.y - s * 0.3f), Size(s * 0.6f, s * 0.6f), style = Stroke(w * 1.6f, cap = StrokeCap.Round))
            drawCircle(tint, w * 0.8f, c + Offset(s * 0.28f, -s * 0.3f))
        }
    }
}

private fun DrawScope.rays(tint: Color, c: Offset, inner: Float, outer: Float, w: Float, from: Float, to: Float, count: Int) {
    val step = if (to - from >= 360f) 360f / count else (to - from) / (count - 1)
    for (i in 0 until count) {
        val a = (from + i * step) * (PI / 180.0)
        val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
        drawLine(tint, c + dir * inner, c + dir * outer, w, StrokeCap.Round)
    }
}
