package fi.mazhar.salat.platform

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
 * way to keep the audio alive for its three minutes once the alarm receiver has returned. Playback
 * uses [AndroidAudioPlayer], so it sounds on the alarm stream like the in-app test.
 *
 * The service's own notification *is* the prayer reminder: title and body as for "Beep", posted on
 * the silent high-importance channel so it pops up as a heads-up with a visible Stop button while
 * nothing overlaps the adhan. Swiping it away (possible on Android 14+, where foreground-service
 * notifications are dismissible) stops the adhan too. When playback ends, the reminder stays in
 * the shade without the Stop button, updated in place so it does not alert a second time.
 *
 * `PrayerAlarmReceiver` calls [start] when the chosen sound is not "Beep"; the service is declared in
 * androidMain/AndroidManifest.xml with the mediaPlayback type. Starting a foreground service from an
 * exact alarm's receiver is one of the documented exemptions from the background-start restrictions
 * (Android 12+): the alarm's allow-while-idle delivery puts the app on the temporary allowlist.
 */
class AdhanPlaybackService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player = AndroidAudioPlayer()

    /** The reminder this service currently fronts; null once playback has finished or was stopped. */
    private var current: Reminder? = null

    private class Reminder(val title: String, val body: String, val soundTitle: String)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                finish(keepReminder = true)
                return START_NOT_STICKY
            }
            ACTION_DISMISSED -> {
                finish(keepReminder = false)
                return START_NOT_STICKY
            }
        }
        val sound = AdhanSounds.byId(intent?.getStringExtra(EXTRA_SOUND_ID))
        val path = sound.resourcePath
        if (path == null) {
            // "Beep" has no audio of its own; the receiver posts the audible reminder instead.
            stopSelf()
            return START_NOT_STICKY
        }
        val reminder = Reminder(
            title = intent?.getStringExtra(EXTRA_TITLE) ?: "Time for prayer",
            body = intent?.getStringExtra(EXTRA_BODY) ?: "",
            soundTitle = sound.title,
        )
        AndroidNotificationService.ensureChannel(this)
        try {
            ServiceCompat.startForeground(
                this,
                AndroidNotificationService.REMINDER_NOTIFICATION_ID,
                buildNotification(reminder, playing = true),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
        } catch (e: RuntimeException) {
            // ForegroundServiceStartNotAllowedException, or a missing-type/permission SecurityException on
            // Android 14+: no adhan, but the user still gets the audible reminder.
            PrayerAlarmReceiver.showReminder(this, reminder.title, reminder.body)
            stopSelf()
            return START_NOT_STICKY
        }
        current = reminder
        scope.launch {
            val bytes = runCatching { Res.readBytes(path) }.getOrNull()
            // Stop may have arrived while the clip was being read.
            if (current !== reminder) return@launch
            if (bytes == null) {
                finish(keepReminder = true)
                return@launch
            }
            player.play(bytes) { if (current === reminder) finish(keepReminder = true) }
        }
        return START_NOT_STICKY
    }

    /**
     * Ends playback. With [keepReminder] the notification is detached from the service and rewritten
     * without the Stop button under the same id, so the reminder stays in the shade like a "Beep" one
     * would and does not alert again; without it (the user swiped it away) nothing is left behind.
     */
    private fun finish(keepReminder: Boolean) {
        player.stop()
        val reminder = current
        current = null
        if (keepReminder && reminder != null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            NotificationManagerCompat.from(this)
                .notify(AndroidNotificationService.REMINDER_NOTIFICATION_ID, buildNotification(reminder, playing = false))
        } else {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    override fun onDestroy() {
        player.release()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(reminder: Reminder, playing: Boolean): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val builder = NotificationCompat.Builder(this, AndroidNotificationService.ADHAN_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(reminder.title)
            .setContentText(reminder.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
            .setContentIntent(launch)
        if (playing) {
            // Replaces the previous prayer's reminder under the same id and must alert again.
            builder
                .setSubText("Adhan · ${reminder.soundTitle}")
                .setOngoing(true)
                .setDeleteIntent(servicePendingIntent(ACTION_DISMISSED, requestCode = 1))
                .addAction(0, "Stop", servicePendingIntent(ACTION_STOP, requestCode = 0))
        } else {
            // The in-place rewrite after playback: no second buzz, dismissible.
            builder.setOnlyAlertOnce(true).setAutoCancel(true)
        }
        return builder.build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, AdhanPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val ACTION_STOP = "fi.mazhar.salat.ADHAN_STOP"
        private const val ACTION_DISMISSED = "fi.mazhar.salat.ADHAN_DISMISSED"
        private const val EXTRA_SOUND_ID = "soundId"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BODY = "body"

        /**
         * Starts playback of [soundId] (an [AdhanSounds] id) fronted by a reminder notification with
         * [title] and [body]. Returns false when the system refuses a background start (Android 12+
         * without an exemption), so the caller can fall back to an audible notification.
         */
        fun start(context: Context, soundId: String, title: String, body: String): Boolean {
            val intent = Intent(context, AdhanPlaybackService::class.java)
                .putExtra(EXTRA_SOUND_ID, soundId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_BODY, body)
            return try {
                ContextCompat.startForegroundService(context, intent)
                true
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException extends IllegalStateException.
                false
            } catch (e: SecurityException) {
                false
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AdhanPlaybackService::class.java).setAction(ACTION_STOP))
        }
    }
}
