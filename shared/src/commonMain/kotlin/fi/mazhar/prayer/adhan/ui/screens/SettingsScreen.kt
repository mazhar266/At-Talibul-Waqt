package fi.mazhar.prayer.adhan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fi.mazhar.prayer.adhan.model.AdhanSound
import fi.mazhar.prayer.adhan.model.AdhanSounds
import fi.mazhar.prayer.adhan.model.AsrJuristic
import fi.mazhar.prayer.adhan.model.CalculationMethod
import fi.mazhar.prayer.adhan.model.HighLatitudeRule
import fi.mazhar.prayer.adhan.ui.AppViewModel
import fi.mazhar.prayer.adhan.ui.components.BellIcon
import fi.mazhar.prayer.adhan.ui.components.Eyebrow
import fi.mazhar.prayer.adhan.ui.components.Muted
import fi.mazhar.prayer.adhan.ui.components.OptionPicker
import fi.mazhar.prayer.adhan.ui.components.Pill
import fi.mazhar.prayer.adhan.ui.components.PlayIcon
import fi.mazhar.prayer.adhan.ui.components.SectionCard
import fi.mazhar.prayer.adhan.ui.components.StopIcon
import fi.mazhar.prayer.adhan.ui.theme.NoorColors

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsState()
    val location by viewModel.location.collectAsState()
    val notificationStatus by viewModel.notificationStatus.collectAsState()
    val previewing by viewModel.previewingSound.collectAsState()
    val calc = settings.calculation
    val selectedSound = AdhanSounds.byId(settings.adhanSound)
    var goalText by remember(settings.dailyGoal) { mutableStateOf(settings.dailyGoal.toString()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Calculation & High-Latitude Rules",
                    Modifier.weight(1f),
                    color = NoorColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Pill("ECFR · MWL", color = NoorColors.Emerald)
            }
            Spacer(Modifier.height(14.dp))
            OptionPicker(
                label = "Calculation standard",
                options = CalculationMethod.entries,
                selected = calc.resolvedMethod(location.latitude),
                optionLabel = { it.label },
                onSelect = { method -> viewModel.updateCalculation { it.copy(method = method) } },
            )
            if (calc.method == null) {
                Muted("Chosen automatically: Finnish convention at 55°N and above, MWL elsewhere.", Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            OptionPicker(
                label = "High-latitude adjustment",
                options = HighLatitudeRule.entries,
                selected = calc.highLatitudeRule,
                optionLabel = { it.label },
                onSelect = { rule -> viewModel.updateCalculation { it.copy(highLatitudeRule = rule) } },
            )
            Muted(
                "Applied above 50° latitude and whenever twilight never reaches the required angle (white nights, polar day/night).",
                Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
            OptionPicker(
                label = "Asr jurisprudence (madhhab)",
                options = AsrJuristic.entries,
                selected = calc.asrJuristic,
                optionLabel = { if (it == AsrJuristic.HANAFI) "Hanafi (shadow factor 2)" else "${it.label} (factor 1)" },
                onSelect = { asr -> viewModel.updateCalculation { it.copy(asrJuristic = asr) } },
            )
        }

        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BellIcon(if (settings.notificationsEnabled) NoorColors.Emerald else NoorColors.TextMuted, muted = !settings.notificationsEnabled)
                Spacer(Modifier.padding(horizontal = 6.dp))
                Column(Modifier.weight(1f)) {
                    Text("Adhan notifications", color = NoorColors.TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Muted(
                        if (viewModel.notificationsSupported) "A reminder at the start of Fajr, Dhuhr, Asr, Maghrib and Isha, rescheduled whenever your location or method changes."
                        else "Not available on desktop — use the Android or iOS app.",
                    )
                }
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled,
                    enabled = viewModel.notificationsSupported,
                    colors = SwitchDefaults.colors(checkedTrackColor = NoorColors.Emerald),
                )
            }

            // The picker is part of enabling reminders; desktop has no reminders but can still preview.
            if (settings.notificationsEnabled || !viewModel.notificationsSupported) {
                Spacer(Modifier.height(16.dp))
                Eyebrow("Adhan sound")
                Muted(
                    if (viewModel.notificationsSupported) "Played when each prayer comes in. Tap a row to choose it, or the circle to hear it."
                    else "Preview the sounds here; the choice takes effect in the Android and iOS apps.",
                    Modifier.padding(top = 2.dp, bottom = 6.dp),
                )
                AdhanSounds.all.forEach { sound ->
                    AdhanSoundRow(
                        sound = sound,
                        selected = sound.id == selectedSound.id,
                        playing = previewing == sound.id,
                        previewEnabled = viewModel.adhanTestSupported && !sound.isBeep,
                        onSelect = { viewModel.setAdhanSound(sound.id) },
                        onPreview = { viewModel.togglePreview(sound.id) },
                    )
                }
                Muted(
                    "Recordings come from Wikimedia Commons and the Internet Archive under the licences shown; the full credits ship with the app as adhan/CREDITS.md.",
                    Modifier.padding(top = 6.dp),
                )

                Spacer(Modifier.height(18.dp))
                ReminderScheduleSection(
                    schedule = settings.reminders,
                    defaultAdhanTitle = selectedSound.title,
                    onChange = viewModel::updateReminders,
                )
            }

            Spacer(Modifier.height(12.dp))
            val testPlaying = previewing != null && previewing == selectedSound.id
            val testEnabled = viewModel.adhanTestSupported && !selectedSound.isBeep
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Muted(
                    when {
                        !viewModel.adhanTestSupported -> "No audio output on this device."
                        selectedSound.isBeep -> "Beep uses the system notification sound, so there is nothing to preview."
                        else -> "Play the selected sound, ${selectedSound.title}, to check it can be heard."
                    },
                    Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                val accent = when {
                    !testEnabled -> NoorColors.TextMuted
                    testPlaying -> NoorColors.Amber
                    else -> NoorColors.Emerald
                }
                OutlinedButton(
                    onClick = viewModel::toggleAdhanTest,
                    enabled = testEnabled,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent, disabledContentColor = accent),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                ) {
                    if (testPlaying) StopIcon(accent) else PlayIcon(accent)
                    Spacer(Modifier.width(8.dp))
                    Text(if (testPlaying) "Stop" else "Adhan test", fontWeight = FontWeight.SemiBold)
                }
            }
            notificationStatus?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = NoorColors.EmeraldLight, style = MaterialTheme.typography.bodySmall)
            }
        }

        SectionCard {
            Text("Dhikr tracker", color = NoorColors.TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it.filter(Char::isDigit).take(6) },
                label = { Text("Daily recitation goal") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Muted("The headline bar on the Routine and Dhikr tabs is measured against this.") },
            )
            LaunchedEffect(goalText) {
                goalText.toIntOrNull()?.takeIf { it > 0 && it != settings.dailyGoal }?.let(viewModel::setDailyGoal)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Haptic tick on each count", color = NoorColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Muted("A light vibration every time you tap the tasbeeh counter.")
                }
                Switch(checked = settings.hapticsEnabled, onCheckedChange = viewModel::setHaptics, colors = SwitchDefaults.colors(checkedTrackColor = NoorColors.Emerald))
            }
        }

        AboutSection(
            acknowledgements = listOf("Arabic text is set in Amiri (SIL Open Font License).") +
                AdhanSounds.recordings.map { "${it.title} — ${it.credit}, ${it.licence}" } +
                "Desktop MP3 playback uses mp3spi, JLayer and Tritonus (LGPL).",
        )
    }
}

/** One selectable adhan sound: radio, name with duration, origin and credit, and a preview control. */
@Composable
private fun AdhanSoundRow(
    sound: AdhanSound,
    selected: Boolean,
    playing: Boolean,
    previewEnabled: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) NoorColors.EmeraldDeep.copy(alpha = 0.35f) else Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(start = 2.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = NoorColors.Emerald, unselectedColor = NoorColors.TextMuted),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    sound.title,
                    Modifier.weight(1f),
                    color = if (selected) NoorColors.EmeraldLight else NoorColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!sound.isBeep) {
                    Text(sound.durationLabel, color = NoorColors.TextMuted, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                }
            }
            Muted(sound.subtitle)
            if (sound.credit.isNotBlank()) {
                Text("${sound.credit} · ${sound.licence}", color = NoorColors.TextMuted.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (!sound.isBeep) {
            Spacer(Modifier.width(10.dp))
            val accent = when {
                !previewEnabled -> NoorColors.TextMuted
                playing -> NoorColors.Amber
                else -> NoorColors.Emerald
            }
            Box(
                Modifier
                    .size(38.dp)
                    .border(1.dp, accent.copy(alpha = 0.6f), CircleShape)
                    .clip(CircleShape)
                    .clickable(enabled = previewEnabled, onClick = onPreview),
                contentAlignment = Alignment.Center,
            ) {
                if (playing) StopIcon(accent) else PlayIcon(accent)
            }
        }
    }
}
