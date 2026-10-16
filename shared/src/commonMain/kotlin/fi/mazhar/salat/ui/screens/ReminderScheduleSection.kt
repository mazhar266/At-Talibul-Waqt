package fi.mazhar.salat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.mazhar.salat.model.Prayer
import fi.mazhar.salat.model.ReminderMode
import fi.mazhar.salat.model.ReminderSchedule
import fi.mazhar.salat.ui.components.Eyebrow
import fi.mazhar.salat.ui.components.Muted
import fi.mazhar.salat.ui.theme.NoorColors
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

/**
 * Per-prayer reminder modes with an optional weekend set: a quiet beep for the office prayers on
 * weekdays, the full adhan at home. Lives inside the notifications card on the Settings screen.
 */
@Composable
fun ReminderScheduleSection(
    schedule: ReminderSchedule,
    defaultAdhanTitle: String,
    onChange: (ReminderSchedule) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Eyebrow("Per prayer")
        Muted(
            "Choose how each prayer is announced. \"Adhan\" plays $defaultAdhanTitle; " +
                "\"Beep\" is just the notification sound; \"Off\" skips that prayer.",
            Modifier.padding(top = 2.dp, bottom = 10.dp),
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PresetChip("Office weekdays", description = "Beep for Dhuhr & Asr Mon–Fri") {
                onChange(ReminderSchedule.officeWeekdays(schedule.weekendDays))
            }
            PresetChip("All adhan") { onChange(ReminderSchedule.allAdhan().copy(weekendDays = schedule.weekendDays)) }
            PresetChip("All beep") { onChange(ReminderSchedule.allBeep().copy(weekendDays = schedule.weekendDays)) }
        }

        Spacer(Modifier.height(12.dp))
        ModeTable(
            title = if (schedule.hasWeekendOverride) "Weekdays" else "Every day",
            modes = schedule.modes(weekend = false),
            onSelect = { prayer, mode -> onChange(schedule.withMode(prayer, mode, weekend = false)) },
        )

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Different on weekends", color = NoorColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Muted("A second set of choices for your days off.")
            }
            Switch(
                checked = schedule.hasWeekendOverride,
                onCheckedChange = { onChange(schedule.withWeekendOverride(it)) },
                colors = SwitchDefaults.colors(checkedTrackColor = NoorColors.Emerald),
            )
        }

        if (schedule.hasWeekendOverride) {
            Spacer(Modifier.height(8.dp))
            WeekendDayPicker(schedule, onChange)
            Spacer(Modifier.height(10.dp))
            ModeTable(
                title = "Weekends",
                modes = schedule.modes(weekend = true),
                onSelect = { prayer, mode -> onChange(schedule.withMode(prayer, mode, weekend = true)) },
            )
        }
    }
}

@Composable
private fun PresetChip(label: String, description: String? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NoorColors.SurfaceRaised)
            .border(1.dp, NoorColors.Outline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, color = NoorColors.TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (description != null) Muted(description, maxLines = 1)
    }
}

@Composable
private fun ModeTable(
    title: String,
    modes: Map<Prayer, ReminderMode>,
    onSelect: (Prayer, ReminderMode) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(NoorColors.Background, RoundedCornerShape(14.dp))
            .border(1.dp, NoorColors.Outline.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(title, color = NoorColors.TextSecondary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        modes.forEach { (prayer, mode) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    prayer.displayName,
                    Modifier.width(64.dp),
                    color = if (mode == ReminderMode.OFF) NoorColors.TextMuted else NoorColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ModeSelector(selected = mode, onSelect = { onSelect(prayer, it) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Four segments; the chosen one is filled, "Off" is dimmed so a silenced prayer stands out. */
@Composable
private fun ModeSelector(selected: ReminderMode, onSelect: (ReminderMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NoorColors.SurfaceRaised)
            .border(1.dp, NoorColors.Outline, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ReminderMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val fill = when {
                !isSelected -> Color.Transparent
                mode == ReminderMode.OFF -> NoorColors.Outline
                mode == ReminderMode.BEEP -> NoorColors.Sky.copy(alpha = 0.35f)
                mode == ReminderMode.TONE -> NoorColors.Amber.copy(alpha = 0.3f)
                else -> NoorColors.EmeraldDeep
            }
            val text = when {
                !isSelected -> NoorColors.TextMuted
                mode == ReminderMode.ADHAN -> NoorColors.EmeraldLight
                mode == ReminderMode.OFF -> NoorColors.TextSecondary
                else -> NoorColors.TextPrimary
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(fill)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    mode.label,
                    color = text,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WeekendDayPicker(schedule: ReminderSchedule, onChange: (ReminderSchedule) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Muted("Weekend days", Modifier.width(96.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { day ->
                val selected = day.isoDayNumber in schedule.weekendDays
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (selected) NoorColors.EmeraldDeep else NoorColors.SurfaceRaised)
                        .border(1.dp, if (selected) NoorColors.Emerald else NoorColors.Outline, CircleShape)
                        .clickable { onChange(schedule.withWeekendDay(day, !selected)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.name.take(2).lowercase().replaceFirstChar { it.uppercase() },
                        color = if (selected) NoorColors.EmeraldLight else NoorColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
