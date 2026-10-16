package fi.mazhar.salat.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.mazhar.salat.model.ExtremeCondition
import fi.mazhar.salat.model.GeoLocation
import fi.mazhar.salat.model.Prayer
import fi.mazhar.salat.model.PrayerSlot
import fi.mazhar.salat.model.PresetLocations
import fi.mazhar.salat.ui.AppViewModel
import fi.mazhar.salat.ui.components.Muted
import fi.mazhar.salat.ui.components.Pill
import fi.mazhar.salat.ui.components.PinIcon
import fi.mazhar.salat.ui.components.PrayerGlyph
import fi.mazhar.salat.ui.components.ProgressBar
import fi.mazhar.salat.ui.components.SectionCard
import fi.mazhar.salat.ui.formatCoordinate
import fi.mazhar.salat.ui.formatCountdown
import fi.mazhar.salat.ui.formatLong
import fi.mazhar.salat.ui.formatTime
import fi.mazhar.salat.ui.roundTo
import fi.mazhar.salat.ui.theme.NoorColors
import kotlin.math.abs

@Composable
fun PrayersScreen(viewModel: AppViewModel, onOpenSettings: () -> Unit) {
    val location by viewModel.location.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    val period by viewModel.period.collectAsState()
    val locating by viewModel.locating.collectAsState()
    val locationStatus by viewModel.locationStatus.collectAsState()
    val today = schedule.today
    val zone = viewModel.timeZone
    var pickingLocation by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(NoorColors.EmeraldDeep.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .border(1.dp, NoorColors.Emerald.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) { PinIcon(NoorColors.EmeraldLight) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                location.name,
                                color = NoorColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (location.country.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Muted("(${location.country})")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Muted(formatCoordinate(location.latitude, location.longitude))
                            if (location.isHighLatitude) {
                                Spacer(Modifier.width(8.dp))
                                Pill("Nordic / High Lat", color = NoorColors.Amber)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickingLocation = true }, modifier = Modifier.weight(1f)) {
                        Text("Change city")
                    }
                    Button(
                        onClick = viewModel::useDeviceLocation,
                        enabled = viewModel.locationSupported && !locating,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NoorColors.SurfaceRaised, contentColor = NoorColors.TextPrimary),
                    ) {
                        if (locating) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = NoorColors.Emerald)
                        } else {
                            Text(if (viewModel.locationSupported) "Use GPS" else "GPS n/a")
                        }
                    }
                }
                locationStatus?.let {
                    Spacer(Modifier.height(8.dp))
                    Muted(it)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Muted("${today.method.label} · ${today.appliedRule.shortLabel}", Modifier.weight(1f), maxLines = 2)
                    TextButton(onClick = onOpenSettings) { Text("Fiqh method", color = NoorColors.Emerald) }
                }
            }
        }

        today.condition?.let { condition ->
            item { HighLatitudeBanner(condition, location, today.appliedRule.label) }
        }

        item {
            val current = period.current
            val next = period.next
            SectionCard(
                background = NoorColors.Surface,
                border = NoorColors.Emerald.copy(alpha = 0.35f),
                padding = PaddingValues(20.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(NoorColors.EmeraldDeep.copy(alpha = 0.25f), NoorColors.Surface)),
                            RoundedCornerShape(16.dp),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Pill("Next Prayer: ${next.displayName} · ${period.nextTime.formatTime(zone)}")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        period.remaining.formatCountdown(),
                        color = NoorColors.TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 52.sp,
                        letterSpacing = 2.sp,
                    )
                    Muted("Remaining until ${next.displayName} prayer in ${location.name}", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Muted(current.displayName)
                        Muted("${(period.progress * 100).toInt()}% elapsed")
                        Muted(next.displayName)
                    }
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(period.progress, color = NoorColors.Amber)
                    if (current.isObligatory) {
                        Spacer(Modifier.height(16.dp))
                        val slot = PrayerSlot.of(current)
                        Button(
                            onClick = { slot?.let(viewModel::startRoutine) },
                            colors = ButtonDefaults.buttonColors(containerColor = NoorColors.EmeraldDeep, contentColor = NoorColors.EmeraldLight),
                        ) {
                            Text("Post-${current.displayName} Dhikr  →", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Today's Prayer Schedule", color = NoorColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Muted(today.date.formatLong())
            }
        }

        items(today.ordered, key = { it.first.name }) { (prayer, time) ->
            val isNext = prayer == period.next && period.nextTime == time
            val isCurrent = prayer == period.current && period.currentStart == time
            PrayerRow(
                prayer = prayer,
                time = time.formatTime(zone),
                isNext = isNext,
                isCurrent = isCurrent,
                onStartDhikr = PrayerSlot.of(prayer)?.let { slot -> { viewModel.startRoutine(slot) } },
            )
        }

        item {
            SectionCard(padding = PaddingValues(14.dp)) {
                NightRow("Islamic Midnight", "Halfway between sunset & sunrise", today.midnight.formatTime(zone), NoorColors.Sky)
                Spacer(Modifier.height(10.dp))
                NightRow("Qiyam al-Layl (Tahajjud)", "Last third of the blessed night", today.qiyam.formatTime(zone), NoorColors.Amber)
            }
        }
    }

    if (pickingLocation) {
        LocationPickerDialog(
            current = location,
            onPick = { viewModel.setLocation(it); pickingLocation = false },
            onDismiss = { pickingLocation = false },
        )
    }
}

@Composable
private fun HighLatitudeBanner(condition: ExtremeCondition, location: GeoLocation, rule: String) {
    SectionCard(
        background = NoorColors.AmberDeep.copy(alpha = 0.25f),
        border = NoorColors.Amber.copy(alpha = 0.4f),
        padding = PaddingValues(14.dp),
    ) {
        Text(
            "High-Latitude Islamic Adjustment Active (${condition.label.uppercase()})",
            color = NoorColors.AmberLight,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "At ${roundTo(abs(location.latitude), 1)}°${if (location.latitude >= 0) "N" else "S"} standard astronomical " +
                "twilight does not reach the required solar depression during summer (White Nights) or winter (Polar Night). " +
                "Times follow European Council for Fatwa and Research (ECFR) and Islamic Cultural Centre of Finland practice " +
                "using the $rule.",
            color = NoorColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PrayerRow(prayer: Prayer, time: String, isNext: Boolean, isCurrent: Boolean, onStartDhikr: (() -> Unit)?) {
    val border = when {
        isNext -> NoorColors.Emerald
        isCurrent -> NoorColors.Amber.copy(alpha = 0.5f)
        else -> NoorColors.Outline.copy(alpha = 0.6f)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isNext) NoorColors.EmeraldDeep.copy(alpha = 0.3f) else NoorColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PrayerGlyph(prayer)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(prayer.displayName, color = NoorColors.TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(prayer.arabicName, color = NoorColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                if (isNext) {
                    Spacer(Modifier.width(8.dp))
                    Pill("NEXT")
                } else if (isCurrent) {
                    Spacer(Modifier.width(8.dp))
                    Pill("NOW", color = NoorColors.Amber)
                }
            }
            if (onStartDhikr != null) {
                Text(
                    "Dhikr ›",
                    Modifier.clickable(onClick = onStartDhikr).padding(top = 2.dp),
                    color = NoorColors.Emerald,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(
            time,
            color = if (isNext) NoorColors.EmeraldLight else NoorColors.TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )
    }
}

@Composable
private fun NightRow(title: String, subtitle: String, time: String, accent: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(accent, RoundedCornerShape(999.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = NoorColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Muted(subtitle)
        }
        Text(time, color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LocationPickerDialog(current: GeoLocation, onPick: (GeoLocation) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Choose a location") },
        text = {
            LazyColumn(Modifier.height(380.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(PresetLocations.all, key = { it.name }) { preset ->
                    val selected = preset.name == current.name && preset.latitude == current.latitude
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) NoorColors.EmeraldDeep.copy(alpha = 0.5f) else NoorColors.SurfaceRaised,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { onPick(preset) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(preset.name, color = NoorColors.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Muted("${preset.country} · ${formatCoordinate(preset.latitude, preset.longitude)}")
                        }
                        if (preset.isHighLatitude) Pill("High lat", color = NoorColors.Amber)
                    }
                }
            }
        },
        containerColor = NoorColors.Surface,
        titleContentColor = NoorColors.TextPrimary,
        textContentColor = NoorColors.TextSecondary,
    )
}
