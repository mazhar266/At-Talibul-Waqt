package fi.mazhar.prayer.adhan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.mazhar.prayer.adhan.model.Dhikr
import fi.mazhar.prayer.adhan.model.DhikrCategory
import fi.mazhar.prayer.adhan.model.PrayerSlot
import fi.mazhar.prayer.adhan.model.routineFor
import fi.mazhar.prayer.adhan.ui.AppViewModel
import fi.mazhar.prayer.adhan.ui.components.ArabicText
import fi.mazhar.prayer.adhan.ui.components.Muted
import fi.mazhar.prayer.adhan.ui.components.OptionPicker
import fi.mazhar.prayer.adhan.ui.components.Pill
import fi.mazhar.prayer.adhan.ui.components.SectionCard
import fi.mazhar.prayer.adhan.ui.components.StatTile
import fi.mazhar.prayer.adhan.ui.theme.NoorColors

@Composable
fun DhikrLibraryScreen(viewModel: AppViewModel) {
    val library by viewModel.library.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val today by viewModel.todayRecord.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<DhikrCategory?>(null) }
    var creating by remember { mutableStateOf(false) }
    var assigning by remember { mutableStateOf<Dhikr?>(null) }
    var deleting by remember { mutableStateOf<Dhikr?>(null) }

    val visible = library.filter { dhikr ->
        (category == null || dhikr.category == category) &&
            (query.isBlank() || listOf(dhikr.title, dhikr.transliteration, dhikr.translation, dhikr.arabic, dhikr.reference)
                .any { it.contains(query, ignoreCase = true) })
    }
    val percent = (today.totalRecitations * 100 / settings.dailyGoal.coerceAtLeast(1)).coerceAtMost(100)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Today's recitations", "${today.totalRecitations}", "$percent% of daily target (${settings.dailyGoal})", Modifier.weight(1f))
                val streak = viewModel.streakDays()
                StatTile("Streak", "$streak", if (streak == 1) "day in a row" else "days in a row", Modifier.weight(1f), accent = NoorColors.Amber)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search by title, translation or Arabic…") },
                    shape = RoundedCornerShape(14.dp),
                )
                Button(onClick = { creating = true }, shape = RoundedCornerShape(14.dp)) { Text("+ Custom") }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    CategoryChip("All Adhkar", category == null) { category = null }
                }
                items(DhikrCategory.entries) { entry ->
                    CategoryChip(entry.label, category == entry) { category = entry }
                }
            }
        }
        items(visible, key = { it.id }) { dhikr ->
            val inRoutines = PrayerSlot.entries.filter { slot -> playlist.routineFor(slot).any { it.dhikrId == dhikr.id } }
            DhikrCard(
                dhikr = dhikr,
                inRoutines = inRoutines,
                todayCount = today.dhikrCounts[dhikr.id] ?: 0,
                onAssign = { assigning = dhikr },
                onStart = { viewModel.startSingle(dhikr) },
                onDelete = if (dhikr.isCustom) ({ deleting = dhikr }) else null,
            )
        }
        if (visible.isEmpty()) {
            item { SectionCard { Muted("Nothing matches your search.") } }
        }
    }

    if (creating) {
        CustomDhikrDialog(
            onCreate = { title, arabic, translit, translation, target, cat ->
                viewModel.addCustomDhikr(title, arabic, translit, translation, target, cat)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }

    assigning?.let { dhikr ->
        AssignRoutineDialog(
            dhikr = dhikr,
            playlist = playlist,
            onToggle = { slot, enabled -> if (enabled) viewModel.addToRoutine(slot, dhikr.id) else viewModel.removeFromRoutine(slot, dhikr.id) },
            onDismiss = { assigning = null },
        )
    }

    deleting?.let { dhikr ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete “${dhikr.title}”?") },
            text = { Text("It will also be removed from every routine.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteCustomDhikr(dhikr.id); deleting = null }) { Text("Delete", color = NoorColors.Danger) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
            containerColor = NoorColors.Surface,
            titleContentColor = NoorColors.TextPrimary,
            textContentColor = NoorColors.TextSecondary,
        )
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NoorColors.EmeraldDeep,
            selectedLabelColor = NoorColors.EmeraldLight,
            labelColor = NoorColors.TextMuted,
            containerColor = NoorColors.Surface,
        ),
    )
}

@Composable
private fun DhikrCard(
    dhikr: Dhikr,
    inRoutines: List<PrayerSlot>,
    todayCount: Int,
    onAssign: () -> Unit,
    onStart: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(dhikr.title, color = NoorColors.TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dhikr.reference.isNotBlank()) Muted("${dhikr.reference} · ")
                    Muted("Target: ")
                    Text("${dhikr.targetCount}x", color = NoorColors.Amber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
            if (dhikr.isCustom) Pill("Custom", color = NoorColors.Sky)
            if (todayCount > 0) {
                Spacer(Modifier.width(6.dp))
                Pill("$todayCount today", color = NoorColors.Emerald)
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(NoorColors.Background, RoundedCornerShape(14.dp))
                .border(1.dp, NoorColors.Outline.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            ArabicText(dhikr.arabic, fontSize = 22.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text("“${dhikr.transliteration}”", color = NoorColors.TextSecondary, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        Spacer(Modifier.height(4.dp))
        Text(dhikr.translation, color = NoorColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        if (dhikr.benefit.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(NoorColors.EmeraldDeep.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .border(1.dp, NoorColors.Emerald.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Text("Virtue: ${dhikr.benefit}", color = NoorColors.EmeraldLight, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onAssign, modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        inRoutines.isEmpty() -> "Add to routine"
                        PrayerSlot.ALL in inRoutines -> "✓ In routine"
                        else -> "✓ ${inRoutines.joinToString { it.label }}"
                    },
                    maxLines = 1,
                )
            }
            Button(onClick = onStart, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = NoorColors.Emerald)) {
                Text("▶ Recite (${dhikr.targetCount}x)", maxLines = 1)
            }
        }
        if (onDelete != null) {
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) { Text("Delete custom dhikr", color = NoorColors.Danger) }
        }
    }
}

@Composable
private fun AssignRoutineDialog(
    dhikr: Dhikr,
    playlist: Map<PrayerSlot, List<fi.mazhar.prayer.adhan.model.RoutineEntry>>,
    onToggle: (PrayerSlot, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Recite after which prayers?") },
        text = {
            Column {
                Muted(dhikr.title)
                Spacer(Modifier.height(8.dp))
                PrayerSlot.entries.forEach { slot ->
                    val checked = playlist.routineFor(slot).any { it.dhikrId == dhikr.id }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = { onToggle(slot, it) })
                        Column {
                            Text(if (slot == PrayerSlot.ALL) "All prayers (shared list)" else slot.label, color = NoorColors.TextPrimary)
                            if (slot != PrayerSlot.ALL && !playlist.containsKey(slot)) Muted("currently follows the shared list")
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
private fun CustomDhikrDialog(
    onCreate: (String, String, String, String, Int, DhikrCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var arabic by remember { mutableStateOf("") }
    var transliteration by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("33") }
    var category by remember { mutableStateOf(DhikrCategory.DAILY) }
    val valid = title.isNotBlank() && (arabic.isNotBlank() || transliteration.isNotBlank()) && (target.toIntOrNull() ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom dhikr") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(arabic, { arabic = it }, label = { Text("Arabic") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(transliteration, { transliteration = it }, label = { Text("Transliteration") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(translation, { translation = it }, label = { Text("Meaning") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    target, { target = it.filter(Char::isDigit).take(4) },
                    label = { Text("Target count") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OptionPicker("Category", DhikrCategory.entries, category, { it.label }, { category = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, arabic, transliteration, translation, target.toInt(), category) },
                enabled = valid,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = NoorColors.Surface,
        titleContentColor = NoorColors.TextPrimary,
        textContentColor = NoorColors.TextSecondary,
    )
}
