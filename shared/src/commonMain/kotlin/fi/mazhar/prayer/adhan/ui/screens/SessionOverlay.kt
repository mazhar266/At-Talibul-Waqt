package fi.mazhar.prayer.adhan.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.mazhar.prayer.adhan.model.progressFor
import fi.mazhar.prayer.adhan.model.routineFor
import fi.mazhar.prayer.adhan.ui.AppViewModel
import fi.mazhar.prayer.adhan.ui.SessionState
import fi.mazhar.prayer.adhan.ui.components.ArabicText
import fi.mazhar.prayer.adhan.ui.components.Eyebrow
import fi.mazhar.prayer.adhan.ui.components.Muted
import fi.mazhar.prayer.adhan.ui.components.ProgressBar
import fi.mazhar.prayer.adhan.ui.theme.NoorColors

/** Full-screen tasbeeh counter: tap anywhere to count, with the routine's progress underneath. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SessionOverlay(viewModel: AppViewModel, state: SessionState) {
    val library by viewModel.library.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val today by viewModel.todayRecord.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val haptics = LocalHapticFeedback.current
    val dhikr = library.firstOrNull { it.id == state.current.dhikrId }

    // System back closes the counter instead of leaving the app.
    BackHandler(enabled = true) { viewModel.closeSession() }

    Box(
        Modifier
            .fillMaxSize()
            .background(NoorColors.Background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!state.finished) {
                    if (settings.hapticsEnabled && !state.isStepComplete) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.tap()
                }
            }
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(if (state.finished) "Routine complete" else "Session ${state.index + 1} of ${state.steps.size}")
                    Text(
                        if (state.finished) state.title else dhikr?.title ?: "",
                        color = NoorColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (!state.finished) {
                    TextButton(onClick = viewModel::resetStep) { Text("↺", color = NoorColors.TextMuted, fontSize = 20.sp) }
                }
                TextButton(onClick = viewModel::closeSession) { Text("✕", color = NoorColors.TextMuted, fontSize = 20.sp) }
            }

            if (state.finished) {
                CompletionSummary(viewModel, state, library, today, playlist)
                return@Column
            }

            if (dhikr != null) {
                Spacer(Modifier.height(16.dp))
                ArabicText(dhikr.arabic, fontSize = if (dhikr.arabic.length > 120) 22.sp else 30.sp)
                Spacer(Modifier.height(12.dp))
                Text("“${dhikr.transliteration}”", color = NoorColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Muted(dhikr.translation, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(28.dp))
            CounterRing(state.count, state.target, state.isStepComplete)
            Spacer(Modifier.height(28.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Muted("${(state.count * 100 / state.target.coerceAtLeast(1)).coerceAtMost(100)}% done")
                Muted("Target: ${state.target}x")
            }
            Spacer(Modifier.height(6.dp))
            ProgressBar(state.count.toFloat() / state.target.coerceAtLeast(1), color = if (state.isStepComplete) NoorColors.EmeraldLight else NoorColors.Emerald)

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val nextTitle = state.steps.getOrNull(state.index + 1)?.let { next -> library.firstOrNull { it.id == next.dhikrId }?.title }
                Column(Modifier.weight(1f)) {
                    if (state.index > 0) {
                        Text("‹ Previous", Modifier.clickable(onClick = viewModel::previousStep), color = NoorColors.TextMuted, style = MaterialTheme.typography.labelLarge)
                    }
                    Muted(if (nextTitle != null) "Next: $nextTitle" else "Last dhikr in this routine", maxLines = 2)
                }
                Button(
                    onClick = viewModel::nextStep,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isStepComplete) NoorColors.Emerald else NoorColors.SurfaceRaised,
                        contentColor = if (state.isStepComplete) NoorColors.Background else NoorColors.TextPrimary,
                    ),
                ) { Text(if (state.isLastStep) "Finish  ✓" else "Next dhikr  ›", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CounterRing(count: Int, target: Int, complete: Boolean) {
    val fraction by animateFloatAsState((count.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f), tween(200), label = "ring")
    Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2 + 4.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(NoorColors.SurfaceRaised.copy(alpha = 0.7f), NoorColors.Background)), size.minDimension / 2 - inset, center)
            drawArc(NoorColors.SurfaceRaised, -90f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset), style = Stroke(stroke))
            drawArc(
                if (complete) NoorColors.EmeraldLight else NoorColors.Emerald, -90f, 360f * fraction, false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$count",
                color = NoorColors.TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 64.sp,
            )
            Muted("of $target")
            Spacer(Modifier.height(4.dp))
            Text(
                if (complete) "Complete — tap for next" else "Tap anywhere",
                color = if (complete) NoorColors.EmeraldLight else NoorColors.Emerald,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun CompletionSummary(
    viewModel: AppViewModel,
    state: SessionState,
    library: List<fi.mazhar.prayer.adhan.model.Dhikr>,
    today: fi.mazhar.prayer.adhan.model.DailyRecord,
    playlist: Map<fi.mazhar.prayer.adhan.model.PrayerSlot, List<fi.mazhar.prayer.adhan.model.RoutineEntry>>,
) {
    Spacer(Modifier.height(24.dp))
    Box(
        Modifier
            .size(110.dp)
            .background(NoorColors.EmeraldDeep.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
            .border(2.dp, NoorColors.Emerald, RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center,
    ) { Text("✓", color = NoorColors.EmeraldLight, fontSize = 56.sp, fontWeight = FontWeight.Black) }
    Spacer(Modifier.height(16.dp))
    Text("Taqabbal Allahu minna wa minkum", color = NoorColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Muted("May Allah accept it from us and from you.", textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    val slot = state.slot
    if (slot != null) {
        val progress = today.progressFor(slot, playlist.routineFor(slot), library)
        Column(Modifier.fillMaxWidth()) {
            progress.entries.forEach { entry ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.dhikr.title, Modifier.weight(1f), color = NoorColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Spacer(Modifier.width(10.dp))
                    ProgressBar(entry.fraction, Modifier.width(90.dp), height = 6.dp, color = if (entry.isComplete) NoorColors.EmeraldLight else NoorColors.Emerald)
                    Spacer(Modifier.width(8.dp))
                    Text("${entry.done}/${entry.target}", color = NoorColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    } else {
        val total = state.steps.sumOf { it.count }
        Muted("$total recitations recorded for today.", textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(20.dp))
    Muted("Today's total: ${today.totalRecitations} recitations", textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = viewModel::closeSession) { Text("Close") }
}
