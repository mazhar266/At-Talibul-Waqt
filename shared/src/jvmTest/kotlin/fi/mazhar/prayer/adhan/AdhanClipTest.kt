package fi.mazhar.prayer.adhan

import fi.mazhar.prayer.adhan.data.AppRepository
import fi.mazhar.prayer.adhan.data.InMemoryKeyValueStore
import fi.mazhar.prayer.adhan.model.AdhanSounds
import fi.mazhar.prayer.adhan.ui.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import talibulwaqt.shared.generated.resources.Res
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdhanClipTest {

    @Test
    fun catalogueIsConsistent() {
        val ids = AdhanSounds.all.map { it.id }
        assertEquals(ids.distinct(), ids, "ids must be unique")
        assertEquals(AdhanSounds.DEFAULT_ID, AdhanSounds.byId(null).id)
        assertEquals(AdhanSounds.DEFAULT_ID, AdhanSounds.byId("no-such-sound").id)
        assertTrue(AdhanSounds.beep.isBeep)
        assertNull(AdhanSounds.beep.iosNotificationClip)
        assertTrue(AdhanSounds.recordings.size in 4..6, "expected 4–6 recordings")
        AdhanSounds.recordings.forEach { assertNotNull(it.sourceUrl, "${it.id} needs a source URL") }
    }

    /**
     * Every bundled clip must be readable from the resources, decodable to PCM on this JVM (MP3 goes
     * through mp3spi), and as long as the catalogue claims, so the picker's durations are honest.
     */
    @Test
    fun everyBundledClipDecodesAndMatchesItsDuration() = runBlocking {
        for (sound in AdhanSounds.all) {
            val path = sound.resourcePath ?: continue
            val bytes = Res.readBytes(path)
            val encoded = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
            val pcm = AudioFormat(encoded.format.sampleRate, 16, encoded.format.channels, true, false)
            // Decode everything: the byte count is the only trustworthy length for an in-memory MP3.
            val decoded = AudioSystem.getAudioInputStream(pcm, encoded)
            val buffer = ByteArray(1 shl 16)
            var total = 0L
            while (true) {
                val n = decoded.read(buffer)
                if (n < 0) break
                total += n
            }
            val seconds = total / (pcm.frameRate.toDouble() * pcm.frameSize)
            assertTrue(
                abs(seconds - sound.durationSeconds) <= 2.0,
                "${sound.id}: catalogue says ${sound.durationSeconds} s, decoded ${"%.1f".format(seconds)} s",
            )
        }
    }

    @Test
    fun toneIsPlainPcmWav() = runBlocking {
        val bytes = Res.readBytes(AdhanSounds.tone.resourcePath!!)
        val format = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes)).format
        assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.encoding)
        assertEquals(16, format.sampleSizeInBits)
        assertEquals(1, format.channels)
    }

    /**
     * Actually sounds the clips through the default output: the tone to completion, and the first
     * seconds of an MP3 recording to prove the decoder path. Needs a sound device, so it only runs
     * when ADHAN_PLAY_ADHAN is set.
     */
    @Test
    fun previewsPlayThroughTheViewModel() {
        if (System.getenv("ADHAN_PLAY_ADHAN").isNullOrEmpty()) return
        val viewModel = AppViewModel(AppRepository(InMemoryKeyValueStore()))
        assertTrue(viewModel.adhanTestSupported, "no audio output on this machine")

        val started = System.currentTimeMillis()
        viewModel.togglePreview(AdhanSounds.TONE_ID)
        assertEquals(AdhanSounds.TONE_ID, viewModel.previewingSound.value)
        runBlocking { withTimeout(30_000) { viewModel.previewingSound.first { it == null } } }
        val elapsed = (System.currentTimeMillis() - started) / 1000.0
        assertTrue(elapsed in 12.0..20.0, "tone should have taken about 13.7 s to play, took $elapsed s")

        viewModel.setAdhanSound(AdhanSounds.DEFAULT_ID)
        viewModel.toggleAdhanTest()
        runBlocking { delay(4_000) }
        assertEquals(AdhanSounds.DEFAULT_ID, viewModel.previewingSound.value, "recording should still be playing after 4 s")
        viewModel.stopPreview()
        assertNull(viewModel.previewingSound.value)
    }
}
