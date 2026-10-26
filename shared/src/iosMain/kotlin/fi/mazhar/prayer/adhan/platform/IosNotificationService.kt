package fi.mazhar.prayer.adhan.platform

import fi.mazhar.prayer.adhan.model.AdhanSounds
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * Local notifications through UNUserNotificationCenter. iOS caps pending requests at 64, so the
 * app plans a week ahead (5 × 7 = 35) and re-plans every time it comes to the foreground.
 */
class IosNotificationService : NotificationService {

    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()

    override val isSupported: Boolean = true
    override val planningHorizonDays: Int = PLAN_DAYS

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { granted, _ ->
            if (continuation.isActive) continuation.resume(granted)
        }
    }

    override fun schedule(notifications: List<PrayerNotification>) {
        cancelAll()
        val timeZone = TimeZone.currentSystemDefault()
        for (notification in notifications) {
            val content = UNMutableNotificationContent().apply {
                setTitle(notification.title)
                setBody(notification.body)
                // iOS caps notification sounds at 30 s, so each adhan has an opening clip in the app
                // bundle (iosApp/iosApp/Sounds); "Beep" keeps the system sound.
                val clip = AdhanSounds.byId(notification.adhanSoundId).iosNotificationClip
                setSound(if (clip == null) UNNotificationSound.defaultSound else UNNotificationSound.soundNamed(clip))
            }
            val local = notification.time.toLocalDateTime(timeZone)
            val components = NSDateComponents().apply {
                year = local.year.toLong()
                month = (local.month.ordinal + 1).toLong()
                day = local.day.toLong()
                hour = local.hour.toLong()
                minute = local.minute.toLong()
                second = local.second.toLong()
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
            val request = UNNotificationRequest.requestWithIdentifier(
                "prayer-${notification.id}", content, trigger,
            )
            center.addNotificationRequest(request) { _ -> }
        }
    }

    override fun cancelAll() {
        center.removeAllPendingNotificationRequests()
    }

    companion object {
        const val PLAN_DAYS = 7
    }
}
