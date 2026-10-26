package fi.mazhar.prayer.adhan.prayer

import fi.mazhar.prayer.adhan.model.AsrJuristic
import fi.mazhar.prayer.adhan.model.CalculationMethod
import fi.mazhar.prayer.adhan.model.CalculationSettings
import fi.mazhar.prayer.adhan.model.ExtremeCondition
import fi.mazhar.prayer.adhan.model.GeoLocation
import fi.mazhar.prayer.adhan.model.HighLatitudeRule
import fi.mazhar.prayer.adhan.model.PrayerTimes
import fi.mazhar.prayer.adhan.model.PresetLocations
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Reference values were read from salat.mazhar.fi on 2026-10-16 with the browser in UTC+3
 * (identical to Europe/Helsinki summer time that day), default settings.
 */
class PrayerTimeCalculatorTest {

    private val helsinkiZone = TimeZone.of("Europe/Helsinki")
    private val referenceDate = LocalDate(2026, 10, 16)
    private val defaults = CalculationSettings()

    private fun preset(name: String): GeoLocation = PresetLocations.all.first { it.name.startsWith(name) }

    private fun hhmm(instant: Instant, zone: TimeZone): String {
        val local = instant.toLocalDateTime(zone)
        return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }

    private fun minutesBetween(expected: String, actual: Instant, zone: TimeZone): Int {
        val local = actual.toLocalDateTime(zone)
        val (h, m) = expected.split(":").map { it.toInt() }
        var diff = (local.hour * 60 + local.minute) - (h * 60 + m)
        if (diff > 720) diff -= 1440
        if (diff < -720) diff += 1440
        return diff
    }

    private fun assertTimes(times: PrayerTimes, zone: TimeZone, vararg expected: Pair<String, Instant>) {
        for ((wanted, actual) in expected) {
            val diff = minutesBetween(wanted, actual, zone)
            assertTrue(
                abs(diff) <= 2,
                "expected $wanted but got ${hhmm(actual, zone)} (${diff} min off) in $times",
            )
        }
    }

    @Test
    fun helsinkiMatchesNoorSalat() {
        val times = PrayerTimeCalculator.calculate(referenceDate, PresetLocations.helsinki, defaults, helsinkiZone)
        assertEquals(CalculationMethod.FINLAND, times.method)
        assertEquals(ExtremeCondition.ABNORMAL_TWILIGHT, times.condition)
        assertTimes(
            times, helsinkiZone,
            "06:42" to times.fajr, "08:03" to times.sunrise, "13:07" to times.dhuhr,
            "15:29" to times.asr, "18:12" to times.maghrib, "19:32" to times.isha,
            "01:07" to times.midnight, "03:26" to times.qiyam,
        )
    }

    @Test
    fun utsjokiMatchesNoorSalat() {
        val times = PrayerTimeCalculator.calculate(referenceDate, preset("Utsjoki"), defaults, helsinkiZone)
        assertTimes(
            times, helsinkiZone,
            "07:08" to times.fajr, "08:29" to times.sunrise, "12:59" to times.dhuhr,
            "14:39" to times.asr, "17:30" to times.maghrib, "18:50" to times.isha,
            "00:59" to times.midnight, "03:29" to times.qiyam,
        )
    }

    @Test
    fun longyearbyenMatchesNoorSalat() {
        val times = PrayerTimeCalculator.calculate(referenceDate, preset("Longyearbyen"), defaults, helsinkiZone)
        assertTimes(
            times, helsinkiZone,
            "09:13" to times.fajr, "10:33" to times.sunrise, "13:45" to times.dhuhr,
            "14:21" to times.asr, "16:56" to times.maghrib, "18:17" to times.isha,
        )
    }

    @Test
    fun northPoleInOctoberIsPolarNightButStillWorkable() {
        val times = PrayerTimeCalculator.calculate(referenceDate, preset("North Pole"), defaults, helsinkiZone)
        assertEquals(ExtremeCondition.POLAR_NIGHT, times.condition)
        assertTimes(
            times, helsinkiZone,
            "07:57" to times.fajr, "09:18" to times.sunrise, "14:47" to times.dhuhr,
            "17:46" to times.asr, "20:16" to times.maghrib, "21:37" to times.isha,
        )
        assertOrdered(times)
    }

    @Test
    fun northPoleInJuneIsPolarDay() {
        val times = PrayerTimeCalculator.calculate(LocalDate(2026, 6, 21), preset("North Pole"), defaults, helsinkiZone)
        assertEquals(ExtremeCondition.POLAR_DAY, times.condition)
        assertOrdered(times)
    }

    @Test
    fun meccaUsesMwlWithoutAdjustment() {
        val meccaZone = TimeZone.of("Asia/Riyadh")
        val times = PrayerTimeCalculator.calculate(referenceDate, preset("Mecca"), defaults, meccaZone)
        assertEquals(CalculationMethod.MWL, times.method)
        assertNull(times.condition)
        // Sun-based times are method independent; MWL's 18° twilight puts Fajr/Isha further from them.
        assertTimes(
            times, meccaZone,
            "06:18" to times.sunrise, "12:08" to times.dhuhr, "15:28" to times.asr, "17:58" to times.maghrib,
        )
        assertTrue(times.fajr < times.sunrise && times.isha > times.maghrib)
    }

    @Test
    fun meccaWithFinnishAnglesMatchesNoorSalatDefaults() {
        // The web app ships with the Finnish 15°/15° convention pre-selected everywhere.
        val meccaZone = TimeZone.of("Asia/Riyadh")
        val settings = defaults.copy(method = CalculationMethod.FINLAND)
        val times = PrayerTimeCalculator.calculate(referenceDate, preset("Mecca"), settings, meccaZone)
        assertTimes(times, meccaZone, "05:17" to times.fajr, "18:59" to times.isha)
    }

    @Test
    fun hanafiAsrIsLaterThanStandard() {
        val standard = PrayerTimeCalculator.calculate(referenceDate, PresetLocations.helsinki, defaults, helsinkiZone)
        val hanafi = PrayerTimeCalculator.calculate(
            referenceDate, PresetLocations.helsinki, defaults.copy(asrJuristic = AsrJuristic.HANAFI), helsinkiZone,
        )
        assertTrue(hanafi.asr > standard.asr)
        assertEquals(standard.maghrib, hanafi.maghrib)
    }

    @Test
    fun everyHighLatitudeRuleKeepsMidsummerHelsinkiOrdered() {
        val midsummer = LocalDate(2026, 6, 21)
        for (rule in HighLatitudeRule.entries) {
            val times = PrayerTimeCalculator.calculate(
                midsummer, PresetLocations.helsinki, defaults.copy(highLatitudeRule = rule), helsinkiZone,
            )
            assertTrue(times.isExtremeLatitude, "$rule should flag midsummer Helsinki as extreme")
            assertOrdered(times, "rule $rule")
        }
    }

    @Test
    fun ummAlQuraIshaIsNinetyMinutesAfterMaghrib() {
        val settings = defaults.copy(method = CalculationMethod.MAKKAH)
        val times = PrayerTimeCalculator.calculate(referenceDate, preset("Mecca"), settings, TimeZone.of("Asia/Riyadh"))
        assertEquals(90, (times.isha - times.maghrib).inWholeMinutes)
    }

    @Test
    fun farEastZoneLandsOnTheRightCivilDay() {
        val zone = TimeZone.of("Pacific/Kiritimati") // UTC+14, far ahead of its solar time
        val kiritimati = GeoLocation("Kiritimati", "Kiribati", 1.87, -157.4)
        val times = PrayerTimeCalculator.calculate(referenceDate, kiritimati, defaults, zone)
        assertEquals(referenceDate, times.dhuhr.toLocalDateTime(zone).date)
        val noon = times.dhuhr.toLocalDateTime(zone)
        assertTrue(noon.hour in 11..14, "dhuhr should be around local noon, was ${noon.hour}:${noon.minute}")
    }

    private fun assertOrdered(times: PrayerTimes, label: String = "") {
        val sequence = listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha)
        for (i in 0 until sequence.lastIndex) {
            assertTrue(sequence[i] < sequence[i + 1], "$label times out of order: $times")
        }
        assertTrue(times.midnight > times.maghrib && times.qiyam > times.midnight, "$label night times out of order")
    }
}
