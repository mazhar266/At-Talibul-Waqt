package fi.mazhar.prayer.adhan.platform

import fi.mazhar.prayer.adhan.data.AppRepository
import fi.mazhar.prayer.adhan.data.InMemoryKeyValueStore
import fi.mazhar.prayer.adhan.model.AdhanSounds
import fi.mazhar.prayer.adhan.model.Prayer
import fi.mazhar.prayer.adhan.model.ReminderMode
import fi.mazhar.prayer.adhan.model.ReminderSchedule
import fi.mazhar.prayer.adhan.model.UserSettings
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class NotificationPlannerTest {

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private val zone = TimeZone.of("Europe/Helsinki")

    /** Friday 2026-10-16, 08:00 Helsinki: Fajr is over, the other four prayers are ahead. */
    private val fridayMorning = Instant.parse("2026-10-16T05:00:00Z")
    private val friday = LocalDate(2026, 10, 16)
    private val saturday = LocalDate(2026, 10, 17)

    private fun repository(configure: (UserSettings) -> UserSettings): AppRepository =
        AppRepository(InMemoryKeyValueStore(), FixedClock(fridayMorning)) { zone }.apply {
            updateSettings { configure(it.copy(notificationsEnabled = true)) }
        }

    private fun List<PrayerNotification>.on(date: LocalDate, prayer: Prayer): PrayerNotification? =
        firstOrNull { it.prayer == prayer && it.time.toLocalDateTime(zone).date == date }

    @Test
    fun defaultScheduleUsesTheChosenAdhanForEveryPrayer() {
        val repo = repository { it.copy(adhanSound = "istanbul") }
        val plan = NotificationPlanner.plan(repo, days = 2, now = fridayMorning, timeZone = zone)
        assertEquals(4 + 5, plan.size, "four prayers left today plus five tomorrow")
        assertTrue(plan.all { it.adhanSoundId == "istanbul" })
    }

    @Test
    fun fajrCanUseItsOwnRecording() {
        val repo = repository { it.copy(adhanSound = "istanbul", fajrAdhanSound = AdhanSounds.DEFAULT_FAJR_ID) }
        val plan = NotificationPlanner.plan(repo, days = 2, now = fridayMorning, timeZone = zone)
        assertEquals(AdhanSounds.DEFAULT_FAJR_ID, plan.on(saturday, Prayer.FAJR)?.adhanSoundId)
        assertEquals("istanbul", plan.on(saturday, Prayer.DHUHR)?.adhanSoundId)
        assertEquals("istanbul", plan.on(friday, Prayer.ISHA)?.adhanSoundId)
        assertEquals(AdhanSounds.DEFAULT_ID, UserSettings().adhanSoundFor(Prayer.FAJR), "no override = the shared adhan")
    }

    @Test
    fun officePresetBeepsForDhuhrAndAsrOnWeekdaysOnly() {
        val repo = repository { it.copy(reminders = ReminderSchedule.officeWeekdays()) }
        val plan = NotificationPlanner.plan(repo, days = 2, now = fridayMorning, timeZone = zone)

        assertEquals(AdhanSounds.BEEP_ID, plan.on(friday, Prayer.DHUHR)?.adhanSoundId)
        assertEquals(AdhanSounds.BEEP_ID, plan.on(friday, Prayer.ASR)?.adhanSoundId)
        assertEquals(AdhanSounds.DEFAULT_ID, plan.on(friday, Prayer.MAGHRIB)?.adhanSoundId)
        assertEquals(AdhanSounds.DEFAULT_ID, plan.on(saturday, Prayer.DHUHR)?.adhanSoundId)
        assertEquals(AdhanSounds.DEFAULT_ID, plan.on(saturday, Prayer.ASR)?.adhanSoundId)
    }

    @Test
    fun offPrayersAreNotScheduledAndToneMapsToTheToneClip() {
        val schedule = ReminderSchedule()
            .withMode(Prayer.ISHA, ReminderMode.OFF, weekend = false)
            .withMode(Prayer.MAGHRIB, ReminderMode.TONE, weekend = false)
        val repo = repository { it.copy(reminders = schedule) }
        val plan = NotificationPlanner.plan(repo, days = 1, now = fridayMorning, timeZone = zone)

        assertEquals(listOf(Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB), plan.map { it.prayer })
        assertEquals(AdhanSounds.TONE_ID, plan.on(friday, Prayer.MAGHRIB)?.adhanSoundId)
    }

    @Test
    fun weekendDaysAreConfigurable() {
        val fridaySaturdayWeekend = setOf(DayOfWeek.FRIDAY.isoDayNumber, DayOfWeek.SATURDAY.isoDayNumber)
        val repo = repository { it.copy(reminders = ReminderSchedule.officeWeekdays(fridaySaturdayWeekend)) }
        val plan = NotificationPlanner.plan(repo, days = 1, now = fridayMorning, timeZone = zone)
        assertEquals(AdhanSounds.DEFAULT_ID, plan.on(friday, Prayer.DHUHR)?.adhanSoundId, "Friday counts as weekend here")
    }

    @Test
    fun enablingTheWeekendOverrideCopiesWeekdayModesAndEditsStayIndependent() {
        val schedule = ReminderSchedule.allBeep().withWeekendOverride(true)
        assertEquals(ReminderMode.BEEP, schedule.modes(weekend = true)[Prayer.FAJR])
        val edited = schedule.withMode(Prayer.FAJR, ReminderMode.ADHAN, weekend = true)
        assertEquals(ReminderMode.ADHAN, edited.modes(weekend = true)[Prayer.FAJR])
        assertEquals(ReminderMode.BEEP, edited.modes(weekend = false)[Prayer.FAJR])
        assertEquals(null, edited.withWeekendOverride(false).weekend)
    }

    @Test
    fun settingsWithoutRemindersStillLoad() {
        // A profile saved by the previous version has no `reminders` key at all.
        val store = InMemoryKeyValueStore()
        store.putString(
            "adhan_settings_v1",
            """{"calculation":{},"notificationsEnabled":true,"adhanSound":"doha_qatar","dailyGoal":300,"hapticsEnabled":true}""",
        )
        val repo = AppRepository(store, FixedClock(fridayMorning)) { zone }
        assertEquals("doha_qatar", repo.settings.value.adhanSound)
        assertEquals(ReminderMode.ADHAN, repo.settings.value.reminders.modeFor(Prayer.DHUHR, friday))
    }
}
