package fi.mazhar.prayer.adhan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.mazhar.prayer.adhan.AppInfo
import fi.mazhar.prayer.adhan.appVersion
import fi.mazhar.prayer.adhan.getPlatform
import fi.mazhar.prayer.adhan.ui.components.ClockIcon
import fi.mazhar.prayer.adhan.ui.components.Eyebrow
import fi.mazhar.prayer.adhan.ui.components.Muted
import fi.mazhar.prayer.adhan.ui.components.Pill
import fi.mazhar.prayer.adhan.ui.components.SectionCard
import fi.mazhar.prayer.adhan.ui.theme.NoorColors

/**
 * App identity, version, the developer's credits and third-party acknowledgements.
 * [acknowledgements] lets the caller append font/audio licence notices.
 */
@Composable
fun AboutSection(acknowledgements: List<String> = emptyList()) {
    val version = remember { appVersion() }
    val platform = remember { getPlatform().name }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(NoorColors.EmeraldDeep.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .border(1.dp, NoorColors.Emerald.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { ClockIcon(NoorColors.EmeraldLight) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    AppInfo.NAME,
                    color = NoorColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Muted("Salat times, Qibla and dhikr companion")
            }
            // "v1.0.0 (1)" on a packaged app; a Gradle run reports "dev build" without the prefix.
            Pill(if (version.firstOrNull()?.isDigit() == true) "v$version" else version)
        }

        Spacer(Modifier.height(12.dp))
        Muted(
            "The mobile companion to NoorSalat (${AppInfo.WEBSITE}). Prayer times use the same solar algorithm " +
                "and Finnish / ECFR high-latitude rules as the web app, so both show identical times.",
        )

        Spacer(Modifier.height(16.dp))
        Eyebrow("Credits")
        Spacer(Modifier.height(6.dp))
        Text(
            AppInfo.developer.name,
            color = NoorColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppInfo.developer.credentials.forEach { line ->
                Text(line, color = NoorColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (acknowledgements.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Eyebrow("Acknowledgements", color = NoorColors.TextMuted)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                acknowledgements.forEach { Muted(it) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Muted("Version $version")
            Muted("Running on $platform")
        }
    }
}
