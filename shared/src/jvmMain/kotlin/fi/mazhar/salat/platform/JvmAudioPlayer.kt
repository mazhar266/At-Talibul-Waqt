package fi.mazhar.salat.platform

import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

actual fun createAudioPlayer(): AudioPlayer = JvmAudioPlayer()

/**
 * javax.sound.sampled [Clip] through the default output device. WAV plays natively; MP3 is decoded
 * by the mp3spi/JLayer service provider on the jvmMain classpath. [isSupported] is false on machines
 * without an output device (CI runners, headless servers), where the Settings previews are disabled.
 */
class JvmAudioPlayer : AudioPlayer {

    // Written from the caller's thread, read from the audio thread's line listener.
    @Volatile
    private var clip: Clip? = null

    override val isSupported: Boolean by lazy {
        runCatching { AudioSystem.getClip().close() }.isSuccess
    }

    override fun play(bytes: ByteArray, onFinished: () -> Unit) {
        stop()
        try {
            val pcm = decode(bytes)
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
            newClip.open(pcm)
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

    /** A Clip needs linear PCM; compressed input (MP3) is converted through the registered codecs. */
    private fun decode(bytes: ByteArray): AudioInputStream {
        // ByteArrayInputStream supports mark/reset, which the format readers need.
        val encoded = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
        val source = encoded.format
        if (source.encoding == AudioFormat.Encoding.PCM_SIGNED || source.encoding == AudioFormat.Encoding.PCM_UNSIGNED) {
            return encoded
        }
        val target = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            source.sampleRate,
            16,
            source.channels,
            source.channels * 2,
            source.sampleRate,
            false,
        )
        return AudioSystem.getAudioInputStream(target, encoded)
    }
}
