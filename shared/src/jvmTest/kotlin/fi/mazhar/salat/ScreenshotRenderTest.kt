package fi.mazhar.salat

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import fi.mazhar.salat.data.AppRepository
import fi.mazhar.salat.data.InMemoryKeyValueStore
import fi.mazhar.salat.model.PrayerSlot
import fi.mazhar.salat.ui.AppViewModel
import fi.mazhar.salat.ui.theme.TalibulWaqtTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Renders every tab off-screen to PNG when TALIBULWAQT_SCREENSHOT_DIR is set, so the UI can be
 * eyeballed without an emulator. A no-op in normal test runs.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ScreenshotRenderTest {

    private val outputDir: File? = System.getenv("TALIBULWAQT_SCREENSHOT_DIR")?.let(::File)

    /** Taller captures show content below the fold of long screens such as Settings. */
    private val heightPx: Int = System.getenv("TALIBULWAQT_SCREENSHOT_HEIGHT")?.toIntOrNull() ?: 1640

    @Test
    fun renderAllTabs() {
        val dir = outputDir ?: return
        dir.mkdirs()
        val viewModel = AppViewModel(AppRepository(InMemoryKeyValueStore()))
        // Some recorded recitations so the tracker bars have something to show.
        repeat(3) { viewModel.repository.recordTap("post-istighfar", PrayerSlot.DHUHR) }
        repeat(20) { viewModel.repository.recordTap("post-subhanallah", PrayerSlot.DHUHR) }
        repeat(33) { viewModel.repository.recordTap("post-subhanallah", PrayerSlot.FAJR) }

        for (tab in Tab.entries) {
            render(File(dir, "${tab.name.lowercase()}.png")) { TalibulWaqtTheme { AppContent(viewModel, tab) } }
        }
        viewModel.startRoutine(PrayerSlot.DHUHR)
        repeat(2) { viewModel.tap() }
        render(File(dir, "session.png")) { TalibulWaqtTheme { AppContent(viewModel, Tab.ROUTINE) } }
        viewModel.closeSession()
    }

    private fun render(target: File, content: @Composable () -> Unit) {
        ImageComposeScene(width = 820, height = heightPx, density = Density(2f), content = content).use { scene ->
            // A few frames so LaunchedEffects and state flows settle before the capture.
            var time = 0L
            repeat(4) {
                scene.render(time)
                time += 16_000_000L
            }
            val image = scene.render(time)
            val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
            target.writeBytes(bytes)
        }
    }
}
