package fi.mazhar.salat

import fi.mazhar.salat.data.AppRepository
import fi.mazhar.salat.data.InMemoryKeyValueStore
import fi.mazhar.salat.ui.AppViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import talibulwaqt.shared.generated.resources.Res
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdhanClipTest {

    /** The bundled clip must be plain PCM so every platform's player can decode it without codecs. */
    @Test
    fun bundledClipIsPlainPcmWav() = runBlocking {
        val bytes = Res.readBytes(AppViewModel.ADHAN_CLIP_PATH)
        val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
        val format = stream.format
        assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.encoding)
        assertEquals(16, format.sampleSizeInBits)
        assertEquals(1, format.channels)
        val seconds = stream.frameLength / format.frameRate
        assertTrue(seconds in 12f..16f, "clip should be the web app's 13.7 s sequence, was $seconds s")
    }

    /**
     * Actually sounds the clip through the default output and waits for it to end, which proves the
     * whole path from Res.readBytes through the JVM AudioPlayer. Needs a sound device, so it only
     * runs when TALIBULWAQT_PLAY_ADHAN is set.
     */
    @Test
    fun adhanTestPlaysToCompletion() {
        if (System.getenv("TALIBULWAQT_PLAY_ADHAN").isNullOrEmpty()) return
        val viewModel = AppViewModel(AppRepository(InMemoryKeyValueStore()))
        assertTrue(viewModel.adhanTestSupported, "no audio output on this machine")
        val started = System.currentTimeMillis()
        viewModel.toggleAdhanTest()
        assertTrue(viewModel.adhanPlaying.value)
        runBlocking { withTimeout(30_000) { viewModel.adhanPlaying.first { !it } } }
        val elapsed = (System.currentTimeMillis() - started) / 1000.0
        assertTrue(elapsed in 12.0..20.0, "clip should have taken about 13.7 s to play, took $elapsed s")
    }
}
