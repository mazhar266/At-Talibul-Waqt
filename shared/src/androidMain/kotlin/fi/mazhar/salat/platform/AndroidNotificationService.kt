package fi.mazhar.salat.platform

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import fi.mazhar.salat.data.AppRepository
import fi.mazhar.salat.data.createKeyValueStore

/**
 * Exact alarms via AlarmManager, one per upcoming prayer, each re-planning the next two days
 * when it fires. Local alarms are the reliable way to deliver adhan reminders on Android;
 * they need no server and survive Doze.
 */
class AndroidNotificationService : NotificationService {

    private val context: Context get() = AndroidAppContext.application

    override val isSupported: Boolean = true
    override val planningHorizonDays: Int = PLAN_DAYS

    override suspend fun requestPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = Permissions.request(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            if (granted.values.none { it }) return false
        }
        ensureChannel(context)
        return true
    }

    override fun schedule(notifications: List<PrayerNotification>) {
        ensureChannel(context)
        cancelAll()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        for (notification in notifications) {
            val intent = PrayerAlarmReceiver.intent(context, notification)
            val triggerAt = notification.time.toEpochMilliseconds()
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
            }
        }
    }

    override fun cancelAll() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (id in 0 until MAX_ALARM_ID) {
            alarmManager.cancel(PrayerAlarmReceiver.intent(context, id))
        }
    }

    companion object {
        const val CHANNEL_ID = "prayer_times"
        /** Ids are `dayOffset * 10 + prayer.ordinal`; two days of planning never exceed this. */
        const val MAX_ALARM_ID = 30
        const val PLAN_DAYS = 2

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(CHANNEL_ID, "Prayer times", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Adhan reminders at the start of each prayer"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            manager.createNotificationChannel(channel)
        }

        /** Rebuilds the alarm set from persisted settings; safe to call from any receiver. */
        fun replan(context: Context) {
            AndroidAppContext.ensureInit(context)
            val repository = AppRepository(createKeyValueStore())
            val plan = NotificationPlanner.plan(repository, days = PLAN_DAYS)
            AndroidNotificationService().schedule(plan)
        }
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AndroidAppContext.ensureInit(context)
        val title = intent.getStringExtra(EXTRA_TITLE)
        val body = intent.getStringExtra(EXTRA_BODY)
        val id = intent.getIntExtra(EXTRA_ID, 0)
        if (title != null && body != null) show(context, id, title, body)
        AndroidNotificationService.replan(context)
    }

    private fun show(context: Context, id: Int, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Permissions.isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        ) return
        AndroidNotificationService.ensureChannel(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(context, AndroidNotificationService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(launch)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_TAG, id, notification)
    }

    companion object {
        private const val ACTION = "fi.mazhar.salat.PRAYER_ALARM"
        private const val EXTRA_ID = "id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BODY = "body"
        private const val NOTIFICATION_TAG = "prayer"

        fun intent(context: Context, notification: PrayerNotification): PendingIntent =
            pending(context, notification.id, Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION
                putExtra(EXTRA_ID, notification.id)
                putExtra(EXTRA_TITLE, notification.title)
                putExtra(EXTRA_BODY, notification.body)
            })

        fun intent(context: Context, id: Int): PendingIntent =
            pending(context, id, Intent(context, PrayerAlarmReceiver::class.java).apply { action = ACTION })

        private fun pending(context: Context, id: Int, intent: Intent): PendingIntent =
            PendingIntent.getBroadcast(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}

/**
 * Alarms do not survive reboots or clock/zone changes; rebuild them. On Android 12/12L the user can
 * also withdraw the exact-alarm permission (the system then drops every exact alarm) and later give
 * it back, which the system announces with [AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED];
 * re-planning on that broadcast turns the reminders exact again without waiting for the next launch.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> AndroidNotificationService.replan(context)
        }
    }
}
