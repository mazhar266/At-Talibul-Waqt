package fi.mazhar.salat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.mazhar.salat.ui.AppViewModel
import fi.mazhar.salat.ui.components.BookIcon
import fi.mazhar.salat.ui.components.ClockIcon
import fi.mazhar.salat.ui.components.CompassIcon
import fi.mazhar.salat.ui.components.ListIcon
import fi.mazhar.salat.ui.components.SlidersIcon
import fi.mazhar.salat.ui.screens.DhikrLibraryScreen
import fi.mazhar.salat.ui.screens.PrayersScreen
import fi.mazhar.salat.ui.screens.QiblaScreen
import fi.mazhar.salat.ui.screens.RoutineScreen
import fi.mazhar.salat.ui.screens.SessionOverlay
import fi.mazhar.salat.ui.screens.SettingsScreen
import fi.mazhar.salat.ui.theme.NoorColors
import fi.mazhar.salat.ui.theme.TalibulWaqtTheme

enum class Tab(val label: String) {
    PRAYERS("Prayers"),
    QIBLA("Qibla"),
    ROUTINE("Routine"),
    DHIKR("Dhikr & Dua"),
    SETTINGS("Settings"),
}

@Composable
@Preview
fun App() {
    TalibulWaqtTheme {
        val viewModel: AppViewModel = viewModel { AppViewModel() }
        AppContent(viewModel)
    }
}

/** The whole UI for a given view model; split out so previews and screenshots can inject state. */
@Composable
fun AppContent(viewModel: AppViewModel, initialTab: Tab = Tab.PRAYERS) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val session by viewModel.session.collectAsState()

    Box {
        Scaffold(
            containerColor = NoorColors.Background,
            bottomBar = {
                NavigationBar(containerColor = NoorColors.Surface, tonalElevation = 0.dp) {
                    Tab.entries.forEach { entry ->
                        val selected = tab == entry
                        val tint = if (selected) NoorColors.Emerald else NoorColors.TextMuted
                        NavigationBarItem(
                            selected = selected,
                            onClick = { tab = entry },
                            icon = {
                                when (entry) {
                                    Tab.PRAYERS -> ClockIcon(tint)
                                    Tab.QIBLA -> CompassIcon(tint)
                                    Tab.ROUTINE -> ListIcon(tint)
                                    Tab.DHIKR -> BookIcon(tint)
                                    Tab.SETTINGS -> SlidersIcon(tint)
                                }
                            },
                            label = { Text(entry.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedTextColor = NoorColors.Emerald,
                                unselectedTextColor = NoorColors.TextMuted,
                                indicatorColor = NoorColors.EmeraldDeep.copy(alpha = 0.6f),
                                selectedIconColor = NoorColors.Emerald,
                                unselectedIconColor = NoorColors.TextMuted,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    Tab.PRAYERS -> PrayersScreen(viewModel, onOpenSettings = { tab = Tab.SETTINGS })
                    Tab.QIBLA -> QiblaScreen(viewModel)
                    Tab.ROUTINE -> RoutineScreen(viewModel)
                    Tab.DHIKR -> DhikrLibraryScreen(viewModel)
                    Tab.SETTINGS -> SettingsScreen(viewModel)
                }
            }
        }

        session?.let { SessionOverlay(viewModel, it) }
    }
}
