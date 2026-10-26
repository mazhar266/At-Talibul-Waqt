package fi.mazhar.prayer.adhan.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.mazhar.prayer.adhan.qibla.Qibla
import fi.mazhar.prayer.adhan.ui.AppViewModel
import fi.mazhar.prayer.adhan.ui.components.Muted
import fi.mazhar.prayer.adhan.ui.components.Pill
import fi.mazhar.prayer.adhan.ui.components.SectionCard
import fi.mazhar.prayer.adhan.ui.roundTo
import fi.mazhar.prayer.adhan.ui.theme.NoorColors
import fi.mazhar.prayer.adhan.ui.withThousands
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private fun radians(degrees: Double): Double = degrees * PI / 180.0

@Composable
fun QiblaScreen(viewModel: AppViewModel) {
    val location by viewModel.location.collectAsState()
    val heading by viewModel.headings.collectAsState(initial = null)
    val info = remember(location) { Qibla.info(location.latitude, location.longitude) }
    val hasCompass = viewModel.compassSupported
    val currentHeading = heading ?: 0f
    val relative = Qibla.relativeBearing(info.bearing, currentHeading.toDouble())
    val aligned = heading != null && abs(relative) <= 3.0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Qibla Direction", color = NoorColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("Mecca Al-Mukarramah", color = NoorColors.Emerald, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Muted("From: ${location.name} · Bearing: ${roundTo(info.bearing, 1)}° (${info.compassPoint}) · Distance: ${info.distanceKm.roundToInt().withThousands()} km")
        }

        SectionCard(
            background = if (aligned) NoorColors.EmeraldDeep.copy(alpha = 0.35f) else NoorColors.Surface,
            border = if (aligned) NoorColors.Emerald else NoorColors.Outline.copy(alpha = 0.6f),
            padding = PaddingValues(14.dp),
        ) {
            Text(
                when {
                    !hasCompass -> "No compass sensor here — the dial shows the true bearing from north. Use a physical compass and face ${roundTo(info.bearing, 0)}°."
                    heading == null -> "Waiting for the compass… move the phone in a figure of eight to calibrate."
                    aligned -> "You are facing the Qibla."
                    else -> "Turn ${abs(relative).roundToInt()}° ${if (relative > 0) "right" else "left"} to face the golden Kaaba marker."
                },
                color = if (aligned) NoorColors.EmeraldLight else NoorColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (location.latitude >= 65) {
            SectionCard(background = NoorColors.Sky.copy(alpha = 0.08f), border = NoorColors.Sky.copy(alpha = 0.3f), padding = PaddingValues(14.dp)) {
                Text(
                    "Arctic note: this close to the magnetic pole ordinary compasses drift strongly. The dial uses the true " +
                        "great-circle bearing (${roundTo(info.bearing, 1)}°) with magnetic declination corrected from your position.",
                    color = NoorColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        CompassDial(
            heading = currentHeading,
            qiblaBearing = info.bearing.toFloat(),
            aligned = aligned,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (heading == null) "---°" else "${currentHeading.roundToInt()}°",
                color = NoorColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Muted("Current heading · Target Qibla: ")
                Text("${roundTo(info.bearing, 1)}° ${info.compassPoint}", color = NoorColors.AmberLight, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Pill(if (hasCompass) "Live device sensors" else "Static bearing", color = if (hasCompass) NoorColors.Emerald else NoorColors.TextMuted)
        }
    }
}

/** Tracks a compass angle without spinning the long way round when it crosses 0°/360°. */
@Composable
private fun rememberContinuousAngle(target: Float): Float {
    var accumulated by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        val current = ((accumulated % 360f) + 360f) % 360f
        var delta = target - current
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        accumulated += delta
    }
    return accumulated
}

@Composable
private fun CompassDial(heading: Float, qiblaBearing: Float, aligned: Boolean, modifier: Modifier = Modifier) {
    val continuousHeading = rememberContinuousAngle(heading)
    val dialRotation by animateFloatAsState(-continuousHeading, animationSpec = tween(250), label = "dial")
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = NoorColors.TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    val northStyle = labelStyle.copy(color = NoorColors.Rose)

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val c = center
            drawCircle(Brush.radialGradient(listOf(NoorColors.SurfaceRaised, NoorColors.Background), c, r), r, c)
            drawCircle(NoorColors.Outline, r - 2.dp.toPx(), c, style = Stroke(4.dp.toPx()))
            drawCircle(NoorColors.Outline.copy(alpha = 0.5f), r * 0.78f, c, style = Stroke(1.dp.toPx()))

            rotate(dialRotation, c) {
                for (i in 0 until 72) {
                    val major = i % 18 == 0
                    val mid = i % 6 == 0
                    val len = when { major -> r * 0.12f; mid -> r * 0.08f; else -> r * 0.04f }
                    val a = radians(i * 5.0)
                    val dir = Offset(sin(a).toFloat(), -cos(a).toFloat())
                    drawLine(
                        if (major) NoorColors.TextSecondary else NoorColors.Outline,
                        c + dir * (r * 0.93f - len), c + dir * (r * 0.93f),
                        if (major) 2.5.dp.toPx() else 1.dp.toPx(), StrokeCap.Round,
                    )
                }
                listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f).forEach { (label, angle) ->
                    val a = radians(angle.toDouble())
                    val pos = c + Offset(sin(a).toFloat(), -cos(a).toFloat()) * (r * 0.66f)
                    val layout = measurer.measure(label, if (label == "N") northStyle else labelStyle)
                    drawText(layout, topLeft = pos - Offset(layout.size.width / 2f, layout.size.height / 2f))
                }

                // Qibla pointer: a line from the centre to the rim, ending in the Kaaba marker.
                val qa = radians(qiblaBearing.toDouble())
                val qDir = Offset(sin(qa).toFloat(), -cos(qa).toFloat())
                val tip = c + qDir * (r * 0.8f)
                drawLine(
                    Brush.linearGradient(listOf(NoorColors.Emerald.copy(alpha = 0.2f), NoorColors.Amber), c, tip),
                    c, tip, 6.dp.toPx(), StrokeCap.Round,
                )
                val kaabaSize = r * 0.16f
                rotate(qiblaBearing, tip) {
                    drawRoundRect(NoorColors.Amber, tip - Offset(kaabaSize / 2, kaabaSize / 2), androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize), androidx.compose.ui.geometry.CornerRadius(kaabaSize * 0.18f))
                    drawRect(Color(0xFF111827), tip - Offset(kaabaSize * 0.36f, kaabaSize * 0.36f), androidx.compose.ui.geometry.Size(kaabaSize * 0.72f, kaabaSize * 0.72f))
                    drawRect(NoorColors.Amber, tip - Offset(kaabaSize * 0.36f, kaabaSize * 0.05f), androidx.compose.ui.geometry.Size(kaabaSize * 0.72f, kaabaSize * 0.1f))
                }
            }

            // Fixed device-direction needle pointing "up".
            val needle = Path().apply {
                moveTo(c.x, c.y - r * 0.9f)
                lineTo(c.x + r * 0.05f, c.y - r * 0.72f)
                lineTo(c.x - r * 0.05f, c.y - r * 0.72f)
                close()
            }
            drawPath(needle, if (aligned) NoorColors.Emerald else NoorColors.Rose)
            drawCircle(if (aligned) NoorColors.Emerald else NoorColors.Surface, r * 0.07f, c)
            drawCircle(if (aligned) NoorColors.EmeraldLight else NoorColors.Amber, r * 0.07f, c, style = Stroke(2.dp.toPx()))
        }
    }
}
