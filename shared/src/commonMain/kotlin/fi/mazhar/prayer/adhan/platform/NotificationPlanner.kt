package fi.mazhar.prayer.adhan.platform

import fi.mazhar.prayer.adhan.data.AppRepository
import fi.mazhar.prayer.adhan.model.ReminderMode
import fi.mazhar.prayer.adhan.prayer.DailySchedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Turns the user's location and settings into the concrete adhan notifications for the next
 * [days]. Shared by the app and by the Android alarm receivers, so a device reboot or a fired
 * alarm can rebuild the schedule without the UI.
 *
 * Each prayer's [ReminderMode] (per weekday/weekend) decides whether a notification exists at all
 * and which sound it carries; the platforms only ever look at [PrayerNotification.adhanSoundId].
 */
object NotificationPlanner {

    fun plan(
        repository: AppRepository,
        days: Int,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<PrayerNotification> {
        val settings = repository.settings.value
        if (!settings.notificationsEnabled) return emptyList()
        val location = repository.location.value
        val startDate = now.toLocalDateTime(timeZone).date
        val result = mutableListOf<PrayerNotification>()
        for (offset in 0 until days) {
            val date = startDate.plus(offset, DateTimeUnit.DAY)
            val times = DailySchedule.forDate(date, location, settings.calculation, timeZone).today
            for ((prayer, time) in times.ordered) {
                if (!prayer.isObligatory || time <= now) continue
                val mode = settings.reminders.modeFor(prayer, date)
                val soundId = mode.soundId(settings.adhanSound) ?: continue
                result += PrayerNotification(
                    id = offset * 10 + prayer.ordinal,
                    prayer = prayer,
                    time = time,
                    title = "Time for ${prayer.displayName} Prayer",
                    body = if (mode == ReminderMode.ADHAN) {
                        "Adhan has arrived for ${prayer.displayName} in ${location.name}. Hayya 'alas-Salah!"
                    } else {
                        "${prayer.displayName} prayer time in ${location.name}. Hayya 'alas-Salah!"
                    },
                    adhanSoundId = soundId,
                )
            }
        }
        return result.sortedBy { it.time }
    }
}
