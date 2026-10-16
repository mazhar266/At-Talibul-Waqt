package fi.mazhar.salat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fi.mazhar.salat.getPlatform
import fi.mazhar.salat.model.AsrJuristic
import fi.mazhar.salat.model.CalculationMethod
import fi.mazhar.salat.model.HighLatitudeRule
import fi.mazhar.salat.ui.AppViewModel
import fi.mazhar.salat.ui.components.BellIcon
import fi.mazhar.salat.ui.components.Muted
import fi.mazhar.salat.ui.components.OptionPicker
import fi.mazhar.salat.ui.components.Pill
import fi.mazhar.salat.ui.components.PlayIcon
import fi.mazhar.salat.ui.components.SectionCard
import fi.mazhar.salat.ui.components.StopIcon
import fi.mazhar.salat.ui.theme.NoorColors

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsState()
    val location by viewModel.location.collectAsState()
    val notificationStatus by viewModel.notificationStatus.collectAsState()
    val adhanPlaying by viewModel.adhanPlaying.collectAsState()
    val calc = settings.calculation
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
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Muted(
                    if (viewModel.adhanTestSupported) "Preview the adhan tone in the app to check it can be heard."
                    else "No audio output on this device.",
                    Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                val accent = when {
                    !viewModel.adhanTestSupported -> NoorColors.TextMuted
                    adhanPlaying -> NoorColors.Amber
                    else -> NoorColors.Emerald
                }
                OutlinedButton(
                    onClick = viewModel::toggleAdhanTest,
                    enabled = viewModel.adhanTestSupported,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent, disabledContentColor = accent),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                ) {
                    if (adhanPlaying) StopIcon(accent) else PlayIcon(accent)
                    Spacer(Modifier.width(8.dp))
                    Text(if (adhanPlaying) "Stop" else "Adhan test", fontWeight = FontWeight.SemiBold)
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

        SectionCard {
            Text("About", color = NoorColors.TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Muted("TalibulWaqt · the mobile companion to NoorSalat (salat.mazhar.fi). Prayer times use the same solar algorithm and Finnish / ECFR high-latitude rules as the web app, so both show identical times.")
            Spacer(Modifier.height(6.dp))
            Muted("Running on ${getPlatform().name}")
        }
    }
}
