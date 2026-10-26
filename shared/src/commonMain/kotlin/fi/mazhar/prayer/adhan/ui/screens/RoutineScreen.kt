package fi.mazhar.prayer.adhan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.mazhar.prayer.adhan.model.Dhikr
import fi.mazhar.prayer.adhan.model.EntryProgress
import fi.mazhar.prayer.adhan.model.PrayerSlot
import fi.mazhar.prayer.adhan.model.SessionProgress
import fi.mazhar.prayer.adhan.model.hasOwnList
import fi.mazhar.prayer.adhan.model.progressFor
import fi.mazhar.prayer.adhan.model.routineFor
import fi.mazhar.prayer.adhan.ui.AppViewModel
import fi.mazhar.prayer.adhan.ui.components.ArabicText
import fi.mazhar.prayer.adhan.ui.components.Muted
import fi.mazhar.prayer.adhan.ui.components.Pill
import fi.mazhar.prayer.adhan.ui.components.ProgressBar
import fi.mazhar.prayer.adhan.ui.components.SectionCard
import fi.mazhar.prayer.adhan.ui.theme.NoorColors

@Composable
fun RoutineScreen(viewModel: AppViewModel) {
    val playlist by viewModel.playlist.collectAsState()
    val library by viewModel.library.collectAsState()
    val today by viewModel.todayRecord.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var slot by rememberSaveable { mutableStateOf(PrayerSlot.ALL) }
    var adding by remember { mutableStateOf(false) }
    var editingCount by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    val routine = playlist.routineFor(slot)
    val progress = today.progressFor(slot, routine, library)
    val sessionProgress = PrayerSlot.prayers.map { today.progressFor(it, playlist.routineFor(it), library) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SectionCard {
                Text("Personalized Post-Prayer Adhkar", color = NoorColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Muted("Customize the sequential dhikr playlist recited after each prayer.")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.weight(1f)) { Text("Reset defaults") }
                    Button(
                        onClick = { viewModel.startRoutine(slot) },
                        enabled = routine.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { Text("▶  Launch routine") }
                }
            }
        }

        item { TodayTracker(today.totalRecitations, settings.dailyGoal, viewModel.streakDays(), sessionProgress, onReset = viewModel::resetToday) }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PrayerSlot.entries) { candidate ->
                    FilterChip(
                        selected = slot == candidate,
                        onClick = { slot = candidate },
                        label = { Text(if (candidate == PrayerSlot.ALL) "All Prayers (Standard)" else candidate.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NoorColors.EmeraldDeep,
                            selectedLabelColor = NoorColors.EmeraldLight,
                            labelColor = NoorColors.TextMuted,
                            containerColor = NoorColors.Surface,
                        ),
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Muted("${routine.size} adhkar in sequence for ${slot.label.uppercase()}")
                    if (!playlist.hasOwnList(slot)) {
                        Muted("Using the shared list — editing here creates a ${slot.label}-only list.", maxLines = 2)
                    } else if (slot != PrayerSlot.ALL) {
                        Text(
                            "Back to shared list",
                            Modifier.clickable { viewModel.resetRoutineToShared(slot) },
                            color = NoorColors.Amber,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                TextButton(onClick = { adding = true }) { Text("+ Add dhikr", color = NoorColors.Emerald) }
            }
        }

        if (slot != PrayerSlot.ALL) {
            item {
                SectionCard(padding = PaddingValues(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Today's ${slot.label} session", color = NoorColors.TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${progress.done} / ${progress.target}", color = if (progress.isComplete) NoorColors.EmeraldLight else NoorColors.TextMuted, style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(progress.fraction, height = 10.dp, color = if (progress.isComplete) NoorColors.EmeraldLight else NoorColors.Emerald)
                }
            }
        }

        itemsIndexed(routine, key = { _, entry -> entry.dhikrId }) { index, entry ->
            val dhikr = library.firstOrNull { it.id == entry.dhikrId } ?: return@itemsIndexed
            val entryProgress = progress.entries.firstOrNull { it.dhikr.id == entry.dhikrId }
            RoutineEntryCard(
                index = index,
                dhikr = dhikr,
                count = entry.count,
                progress = entryProgress,
                showProgress = slot != PrayerSlot.ALL,
                canMoveUp = index > 0,
                canMoveDown = index < routine.lastIndex,
                onEditCount = { editingCount = entry.dhikrId },
                onMoveUp = { viewModel.moveInRoutine(slot, index, index - 1) },
                onMoveDown = { viewModel.moveInRoutine(slot, index, index + 1) },
                onRemove = { viewModel.removeFromRoutine(slot, entry.dhikrId) },
            )
        }

        if (routine.isEmpty()) {
            item { SectionCard { Muted("This routine is empty. Add adhkar from the library to begin.") } }
        }
    }

    if (adding) {
        val available = library.filter { candidate -> routine.none { it.dhikrId == candidate.id } }
        AddDhikrDialog(
            available = available,
            onAdd = { viewModel.addToRoutine(slot, it.id) },
            onDismiss = { adding = false },
        )
    }

    editingCount?.let { id ->
        val entry = routine.firstOrNull { it.dhikrId == id }
        val dhikr = library.firstOrNull { it.id == id }
        if (entry != null && dhikr != null) {
            CountDialog(
                title = dhikr.title,
                initial = entry.count,
                onConfirm = { viewModel.setRoutineCount(slot, id, it); editingCount = null },
                onDismiss = { editingCount = null },
            )
        } else {
            editingCount = null
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all routines?") },
            text = { Text("Every prayer goes back to the default Sunnah sequence. Custom dhikr stay in your library.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAllRoutines(); confirmReset = false }) { Text("Reset", color = NoorColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
            containerColor = NoorColors.Surface,
            titleContentColor = NoorColors.TextPrimary,
            textContentColor = NoorColors.TextSecondary,
        )
    }
}

/** Daily totals plus one progress bar per prayer session — the heart of the tracker. */
@Composable
fun TodayTracker(total: Int, goal: Int, streak: Int, sessions: List<SessionProgress>, onReset: () -> Unit) {
    SectionCard(border = NoorColors.Emerald.copy(alpha = 0.3f)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Muted("Today's recitations")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$total", color = NoorColors.TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(6.dp))
                    Muted("/ $goal goal", Modifier.padding(bottom = 4.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Pill(if (streak == 1) "1 day streak" else "$streak day streak", color = NoorColors.Amber)
                Text(
                    "Reset today",
                    Modifier.clickable(onClick = onReset).padding(top = 6.dp),
                    color = NoorColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar((total.toFloat() / goal).coerceIn(0f, 1f), height = 10.dp, color = NoorColors.Amber)
        Spacer(Modifier.height(16.dp))
        Muted("Post-prayer sessions")
        Spacer(Modifier.height(8.dp))
        sessions.forEach { session ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    session.slot.label,
                    Modifier.width(64.dp),
                    color = if (session.isComplete) NoorColors.EmeraldLight else NoorColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                ProgressBar(
                    session.fraction,
                    Modifier.weight(1f),
                    height = 8.dp,
                    color = if (session.isComplete) NoorColors.EmeraldLight else NoorColors.Emerald,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (session.isComplete) "✓" else "${session.done}/${session.target}",
                    Modifier.width(56.dp),
                    color = if (session.isComplete) NoorColors.EmeraldLight else NoorColors.TextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun RoutineEntryCard(
    index: Int,
    dhikr: Dhikr,
    count: Int,
    progress: EntryProgress?,
    showProgress: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEditCount: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    SectionCard(padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).background(NoorColors.SurfaceRaised, RoundedCornerShape(999.dp)).border(1.dp, NoorColors.Outline, RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("${index + 1}", color = NoorColors.TextMuted, style = MaterialTheme.typography.labelMedium) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(dhikr.title, color = NoorColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ArabicText(dhikr.arabic, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Start, maxLines = 1, color = NoorColors.AmberLight.copy(alpha = 0.9f))
                Muted(dhikr.transliteration, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${count}x ✎",
                    Modifier
                        .background(NoorColors.EmeraldDeep.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onEditCount)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = NoorColors.EmeraldLight,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row {
                    IconText("▲", enabled = canMoveUp, onClick = onMoveUp)
                    IconText("▼", enabled = canMoveDown, onClick = onMoveDown)
                    IconText("✕", enabled = true, color = NoorColors.Danger, onClick = onRemove)
                }
            }
        }
        if (showProgress && progress != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(progress.fraction, Modifier.weight(1f), height = 6.dp, color = if (progress.isComplete) NoorColors.EmeraldLight else NoorColors.Emerald)
                Spacer(Modifier.width(8.dp))
                Muted("${progress.done}/${progress.target}")
            }
        }
    }
}

@Composable
private fun IconText(glyph: String, enabled: Boolean, onClick: () -> Unit, color: androidx.compose.ui.graphics.Color = NoorColors.TextMuted) {
    Text(
        glyph,
        Modifier
            .padding(top = 6.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
        color = if (enabled) color else color.copy(alpha = 0.3f),
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
fun AddDhikrDialog(available: List<Dhikr>, onAdd: (Dhikr) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Add dhikr to this prayer") },
        text = {
            if (available.isEmpty()) {
                Text("Every dhikr in your library is already in this routine.")
            } else {
                LazyColumn(Modifier.height(400.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(available, key = { it.id }) { dhikr ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(NoorColors.SurfaceRaised, RoundedCornerShape(12.dp))
                                .clickable { onAdd(dhikr) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(dhikr.title, color = NoorColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Muted("${dhikr.category.label} · ${dhikr.targetCount}x", maxLines = 1)
                            }
                            Text("+", color = NoorColors.Emerald, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        },
        containerColor = NoorColors.Surface,
        titleContentColor = NoorColors.TextPrimary,
        textContentColor = NoorColors.TextSecondary,
    )
}

@Composable
fun CountDialog(title: String, initial: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial.toString()) }
    val value = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repetitions") },
        text = {
            Column {
                Muted(title)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(4) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Times to recite") },
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 3, 7, 10, 33, 100).forEach { preset ->
                        FilterChip(selected = value == preset, onClick = { text = preset.toString() }, label = { Text("$preset") })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { value?.let(onConfirm) }, enabled = value != null && value > 0) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = NoorColors.Surface,
        titleContentColor = NoorColors.TextPrimary,
        textContentColor = NoorColors.TextSecondary,
    )
}
