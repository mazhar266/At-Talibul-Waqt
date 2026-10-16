package fi.mazhar.salat.prayer

import fi.mazhar.salat.model.CalculationSettings
import fi.mazhar.salat.model.Prayer
import fi.mazhar.salat.model.PresetLocations
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyScheduleTest {

    private val zone = TimeZone.of("Europe/Helsinki")
    private val schedule = DailySchedule.forDate(LocalDate(2026, 10, 16), PresetLocations.helsinki, CalculationSettings(), zone)

    private fun at(hour: Int, minute: Int) = LocalDateTime(2026, 10, 16, hour, minute).toInstant(zone)

    @Test
    fun betweenDhuhrAndAsrCountsDownToAsr() {
        val period = schedule.periodAt(at(13, 19))
        assertEquals(Prayer.DHUHR, period.current)
        assertEquals(Prayer.ASR, period.next)
        assertTrue(period.remaining.inWholeMinutes in 128..132, "remaining was ${period.remaining}")
        assertTrue(period.progress in 0.05f..0.12f, "progress was ${period.progress}")
    }

    @Test
    fun beforeFajrTheCurrentPeriodIsYesterdaysIsha() {
        val period = schedule.periodAt(at(3, 0))
        assertEquals(Prayer.ISHA, period.current)
        assertEquals(Prayer.FAJR, period.next)
        assertEquals(schedule.today.fajr, period.nextTime)
        assertTrue(period.progress > 0.5f)
    }

    @Test
    fun afterIshaTheNextPrayerIsTomorrowsFajr() {
        val period = schedule.periodAt(at(23, 0))
        assertEquals(Prayer.ISHA, period.current)
        assertEquals(Prayer.FAJR, period.next)
        assertEquals(schedule.tomorrow.fajr, period.nextTime)
    }

    @Test
    fun upcomingSkipsSunriseAndRespectsWindow() {
        val upcoming = schedule.upcoming(at(7, 0), at(19, 0))
        assertEquals(listOf(Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB), upcoming.map { it.prayer })
    }
}
