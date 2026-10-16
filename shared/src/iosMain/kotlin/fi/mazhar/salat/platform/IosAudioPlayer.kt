package fi.mazhar.salat.platform

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.darwin.NSObject

actual fun createAudioPlayer(): AudioPlayer = IosAudioPlayer()

/**
 * AVAudioPlayer fed straight from memory. The audio session is put in the playback category so the
 * clip is heard even with the ring/silent switch on silent, which is what one expects of an adhan.
 *
 * Written on Windows and not yet compiled against the iOS SDK; verify on a Mac.
 */
class IosAudioPlayer : AudioPlayer {

    private var player: AVAudioPlayer? = null
    // AVAudioPlayer only holds its delegate weakly, so keep the strong reference here.
    private var delegate: CompletionDelegate? = null

    override val isSupported: Boolean = true

    override fun play(wavBytes: ByteArray, onFinished: () -> Unit) {
        stop()
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)

        val data = wavBytes.usePinned { pinned ->
            // dataWithBytes:length: copies, so the pin may end right after this call.
            NSData.create(bytes = pinned.addressOf(0), length = wavBytes.size.toULong())
        }
        val avPlayer = runCatching { AVAudioPlayer(data = data, error = null) }.getOrNull()
        if (avPlayer == null) {
            onFinished()
            return
        }
        val completion = CompletionDelegate { finished ->
            if (player === finished) {
                player = null
                delegate = null
                onFinished()
            }
        }
        avPlayer.delegate = completion
        delegate = completion
        player = avPlayer
        if (!avPlayer.prepareToPlay() || !avPlayer.play()) {
            player = null
            delegate = null
            onFinished()
        }
    }

    override fun stop() {
        val avPlayer = player ?: return
        player = null
        delegate = null
        avPlayer.delegate = null
        avPlayer.stop()
    }

    override fun release() = stop()

    private class CompletionDelegate(private val onDone: (AVAudioPlayer) -> Unit) :
        NSObject(), AVAudioPlayerDelegateProtocol {

        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) = onDone(player)

        override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) = onDone(player)
    }
}
