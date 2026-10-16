package fi.mazhar.salat.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import fi.mazhar.salat.model.AdhanSounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import talibulwaqt.shared.generated.resources.Res

/**
 * Plays the selected adhan recording when a prayer alarm fires. A foreground service is the only
 * way to keep the audio alive for its three minutes once the alarm receiver has returned; the
 * notification it shows carries a Stop action. Playback uses [AndroidAudioPlayer], so it sounds on
 * the alarm stream like the in-app test.
 *
 * Start it with [start] from `PrayerAlarmReceiver` when the chosen sound is not "Beep". The manifest
 * needs, next to the receivers:
 *
 *     <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
 *     <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
 *     <service android:name="fi.mazhar.salat.platform.AdhanPlaybackService"
 *              android:exported="false"
 *              android:foregroundServiceType="mediaPlayback" />
 *
 * Starting a foreground service from an exact alarm's receiver is one of the documented
 * exemptions from the background-start restrictions (Android 12+).
 */
class AdhanPlaybackService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player = AndroidAudioPlayer()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finish()
            return START_NOT_STICKY
        }
        val sound = AdhanSounds.byId(intent?.getStringExtra(EXTRA_SOUND_ID) ?: AdhanSounds.DEFAULT_ID)
        val path = sound.resourcePath
        if (path == null) {
            // "Beep" has no audio of its own; the notification's channel sound covers it.
            stopSelf()
            return START_NOT_STICKY
        }
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Adhan"
        ensureChannel(this)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(title, sound.title),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        scope.launch {
            val bytes = runCatching { Res.readBytes(path) }.getOrNull()
            if (bytes == null) {
                finish()
                return@launch
            }
            player.play(bytes) { finish() }
        }
        return START_NOT_STICKY
    }

    private fun finish() {
        player.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        player.release()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(title: String, soundTitle: String): android.app.Notification {
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, AdhanPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Adhan · $soundTitle")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(launch)
            .addAction(0, "Stop", stop)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "adhan_playback"
        private const val NOTIFICATION_ID = 7001
        private const val ACTION_STOP = "fi.mazhar.salat.ADHAN_STOP"
        private const val EXTRA_SOUND_ID = "soundId"
        private const val EXTRA_TITLE = "title"

        /** Starts playback of [soundId] (an [AdhanSounds] id) with [title] on the service notification. */
        fun start(context: Context, soundId: String, title: String) {
            val intent = Intent(context, AdhanPlaybackService::class.java)
                .putExtra(EXTRA_SOUND_ID, soundId)
                .putExtra(EXTRA_TITLE, title)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AdhanPlaybackService::class.java).setAction(ACTION_STOP))
        }

        /** A silent, low-importance channel: the adhan itself is the sound, the notification only hosts Stop. */
        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(CHANNEL_ID, "Adhan playback", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows while an adhan recording is playing, with a Stop button"
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
