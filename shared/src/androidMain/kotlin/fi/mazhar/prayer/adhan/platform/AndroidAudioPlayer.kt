package fi.mazhar.prayer.adhan.platform

import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()

/**
 * MediaPlayer on the alarm stream (USAGE_ALARM), so the test clip sounds at the volume an adhan
 * reminder will use and is not silenced by a muted media volume. The clip is served straight from
 * memory through a [MediaDataSource]; nothing is written to disk.
 *
 * MediaPlayer delivers its callbacks on the looper of the thread that created it, which is the
 * main thread when the view model calls [play] from a button, so no extra hand-off is needed.
 */
class AndroidAudioPlayer : AudioPlayer {

    private var player: MediaPlayer? = null

    override val isSupported: Boolean = true

    override fun play(wavBytes: ByteArray, onFinished: () -> Unit) {
        stop()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        // Only the player that is still current may report completion; a stopped one stays silent.
        val finish = {
            if (player === mediaPlayer) {
                player = null
                mediaPlayer.release()
                onFinished()
            }
        }
        try {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            mediaPlayer.setDataSource(ByteArrayDataSource(wavBytes))
            mediaPlayer.setOnPreparedListener { it.start() }
            mediaPlayer.setOnCompletionListener { finish() }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                finish()
                true
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            finish()
        }
    }

    override fun stop() {
        val mediaPlayer = player ?: return
        player = null
        // reset() is legal in every MediaPlayer state, unlike stop().
        runCatching { mediaPlayer.reset() }
        mediaPlayer.release()
    }

    override fun release() = stop()
}

private class ByteArrayDataSource(private val bytes: ByteArray) : MediaDataSource() {

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= bytes.size) return -1
        val count = minOf(size, (bytes.size - position).toInt())
        System.arraycopy(bytes, position.toInt(), buffer, offset, count)
        return count
    }

    override fun getSize(): Long = bytes.size.toLong()

    override fun close() = Unit
}
