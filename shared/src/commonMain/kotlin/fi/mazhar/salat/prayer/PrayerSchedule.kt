package fi.mazhar.salat.prayer

import fi.mazhar.salat.model.CalculationSettings
import fi.mazhar.salat.model.GeoLocation
import fi.mazhar.salat.model.Prayer
import fi.mazhar.salat.model.PrayerTimes
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Instant

data class PrayerPeriod(
    val current: Prayer,
    val currentStart: Instant,
    val next: Prayer,
    val nextTime: Instant,
    val remaining: Duration,
    /** 0f..1f of the way from [currentStart] to [nextTime]. */
    val progress: Float,
)

data class TimedPrayer(val prayer: Prayer, val time: Instant, val date: LocalDate)

/** Today's times together with the neighbouring days needed for countdowns across midnight. */
class DailySchedule(
    val yesterday: PrayerTimes,
    val today: PrayerTimes,
    val tomorrow: PrayerTimes,
) {
    val timeline: List<TimedPrayer> = listOf(yesterday, today, tomorrow)
        .flatMap { day -> day.ordered.map { (prayer, time) -> TimedPrayer(prayer, time, day.date) } }
        .sortedBy { it.time }

    fun periodAt(now: Instant): PrayerPeriod {
        val index = timeline.indexOfLast { it.time <= now }.coerceAtLeast(0)
        val current = timeline[index]
        val next = timeline.getOrNull(index + 1) ?: current
        val span = next.time - current.time
        val elapsed = now - current.time
        val progress = if (span.inWholeSeconds <= 0) 0f else {
            (elapsed.inWholeSeconds.toDouble() / span.inWholeSeconds).toFloat().coerceIn(0f, 1f)
        }
        return PrayerPeriod(
            current = current.prayer,
            currentStart = current.time,
            next = next.prayer,
            nextTime = next.time,
            remaining = (next.time - now).coerceAtLeast(Duration.ZERO),
            progress = progress,
        )
    }

    /** Obligatory prayers in [from, to), in order — the set worth scheduling notifications for. */
    fun upcoming(from: Instant, to: Instant): List<TimedPrayer> =
        timeline.filter { it.prayer.isObligatory && it.time >= from && it.time < to }

    companion object {
        fun forDate(
            date: LocalDate,
            location: GeoLocation,
            settings: CalculationSettings,
            timeZone: TimeZone,
        ): DailySchedule = DailySchedule(
            yesterday = PrayerTimeCalculator.calculate(date.minus(1, DateTimeUnit.DAY), location, settings, timeZone),
            today = PrayerTimeCalculator.calculate(date, location, settings, timeZone),
            tomorrow = PrayerTimeCalculator.calculate(date.plus(1, DateTimeUnit.DAY), location, settings, timeZone),
        )

        fun forInstant(
            now: Instant,
            location: GeoLocation,
            settings: CalculationSettings,
            timeZone: TimeZone,
        ): DailySchedule = forDate(now.toLocalDateTime(timeZone).date, location, settings, timeZone)
    }
}
