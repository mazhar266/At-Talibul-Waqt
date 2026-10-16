package fi.mazhar.salat.platform

import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

actual fun createAudioPlayer(): AudioPlayer = JvmAudioPlayer()

/**
 * javax.sound.sampled [Clip] through the default output device. [isSupported] is false on machines
 * without one (CI runners, headless servers), where the Settings button is simply disabled.
 */
class JvmAudioPlayer : AudioPlayer {

    // Written from the caller's thread, read from the audio thread's line listener.
    @Volatile
    private var clip: Clip? = null

    override val isSupported: Boolean by lazy {
        runCatching { AudioSystem.getClip().close() }.isSuccess
    }

    override fun play(wavBytes: ByteArray, onFinished: () -> Unit) {
        stop()
        try {
            // ByteArrayInputStream supports mark/reset, which the WAV parser needs.
            val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(wavBytes))
            val newClip = AudioSystem.getClip()
            clip = newClip
            newClip.addLineListener { event ->
                // STOP fires both at the end of the data and after stop(); only the former still
                // has this clip as the current one.
                if (event.type == LineEvent.Type.STOP && clip === newClip) {
                    clip = null
                    newClip.close()
                    onFinished()
                }
            }
            newClip.open(stream)
            newClip.start()
        } catch (e: Exception) {
            clip = null
            onFinished()
        }
    }

    override fun stop() {
        val current = clip ?: return
        clip = null
        current.stop()
        current.close()
    }

    override fun release() = stop()
}
