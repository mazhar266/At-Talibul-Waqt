package fi.mazhar.salat.platform

/**
 * Plays one short in-memory clip at a time: the bundled adhan tone behind the "Adhan test" button.
 * Playback is fire-and-forget; the view model only needs to know when the clip has ended.
 */
interface AudioPlayer {
    /** False where there is no audio output at all (headless JVMs); the UI disables the button. */
    val isSupported: Boolean

    /**
     * Starts [wavBytes] (a RIFF/WAVE PCM clip) from the beginning, cutting off anything already
     * playing. [onFinished] is invoked exactly once when playback ends on its own, either at the
     * end of the clip or because it failed. It is not invoked after [stop].
     */
    fun play(wavBytes: ByteArray, onFinished: () -> Unit)

    /** Stops playback immediately; a no-op when nothing is playing. */
    fun stop()

    /** Stops playback and frees the platform resources. The player must not be used afterwards. */
    fun release()
}

expect fun createAudioPlayer(): AudioPlayer
