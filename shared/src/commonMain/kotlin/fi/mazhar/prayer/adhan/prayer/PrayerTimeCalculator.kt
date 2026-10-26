package fi.mazhar.prayer.adhan.prayer

import fi.mazhar.prayer.adhan.model.CalculationMethod
import fi.mazhar.prayer.adhan.model.CalculationSettings
import fi.mazhar.prayer.adhan.model.ExtremeCondition
import fi.mazhar.prayer.adhan.model.GeoLocation
import fi.mazhar.prayer.adhan.model.HighLatitudeRule
import fi.mazhar.prayer.adhan.model.PrayerTimes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.math.abs
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Prayer times with the high-latitude handling used by the NoorSalat web app:
 *
 * - Above 50° the chosen [HighLatitudeRule] is always applied (ECFR / Islamic Cultural Centre of
 *   Finland practice), not only when twilight fails to occur.
 * - "Nearest latitude" borrows the twilight *durations* of 45° on the same day and attaches them
 *   to the real local sunrise/sunset.
 * - During polar day or polar night there is no sunrise/sunset at all, so the whole day shape is
 *   taken from 45° and centred on local solar noon. This keeps the North Pole workable.
 */
object PrayerTimeCalculator {
    private const val SUNRISE_ANGLE = -0.833
    private const val REFERENCE_LATITUDE = 45.0
    private const val HIGH_LATITUDE_THRESHOLD = 50.0

    /** Latitudes are clamped so the hour-angle maths stays finite exactly at the poles. */
    private const val MAX_LATITUDE = 89.99

    fun calculate(
        date: LocalDate,
        location: GeoLocation,
        settings: CalculationSettings,
        timeZone: TimeZone,
    ): PrayerTimes {
        val method = settings.resolvedMethod(location.latitude)
        val rule = settings.highLatitudeRule
        val shadowFactor = settings.asrJuristic.shadowFactor
        val lat = location.latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)
        val sun = SolarMath.sunPosition(SolarMath.julianDay(date))
        val decl = sun.declination

        // Solar noon as an instant: computed in UTC, then snapped to the occurrence nearest the
        // civil noon of `date` in the user's zone so far-east/far-west zones land on the right day.
        val noonUtcHours = SolarMath.fixHour(12.0 - location.longitude / 15.0 - sun.equationOfTime)
        val utcMidnight = date.atStartOfDayIn(TimeZone.UTC)
        val civilNoon = date.atStartOfDayIn(timeZone) + 12.hours
        val solarNoon = (-1..1)
            .map { k -> utcMidnight + (noonUtcHours + 24.0 * k).hours }
            .minBy { abs((it - civilNoon).inWholeSeconds) }
        // Dhuhr begins once the sun has visibly left the meridian; all other times are measured
        // from the same reference so they match the web app minute for minute.
        val base = solarNoon + 2.minutes

        val fajrAngle = -method.fajrAngle
        val ishaAngle = -method.ishaAngle
        val ishaInterval = method.ishaIntervalMinutes
        val sunriseHa = SolarMath.hourAngle(lat, decl, SUNRISE_ANGLE)
        val fajrHa = SolarMath.hourAngle(lat, decl, fajrAngle)
        val asrHa = SolarMath.asrHourAngle(lat, decl, shadowFactor)
        val ishaHa = if (ishaInterval != null) null else SolarMath.hourAngle(lat, decl, ishaAngle)
        val ishaAvailable = ishaInterval != null || ishaHa != null

        val polarDay = sunriseHa == null && lat * decl > 0
        val polarNight = sunriseHa == null && lat * decl <= 0
        var condition: ExtremeCondition? = when {
            polarDay -> ExtremeCondition.POLAR_DAY
            polarNight -> ExtremeCondition.POLAR_NIGHT
            fajrHa == null || !ishaAvailable -> ExtremeCondition.WHITE_NIGHTS
            abs(location.latitude) >= HIGH_LATITUDE_THRESHOLD -> ExtremeCondition.ABNORMAL_TWILIGHT
            else -> null
        }

        val refLat = if (location.latitude >= 0) REFERENCE_LATITUDE else -REFERENCE_LATITUDE
        val refSunriseHa = SolarMath.hourAngle(refLat, decl, SUNRISE_ANGLE) ?: 6.0
        val refFajrHa = SolarMath.hourAngle(refLat, decl, fajrAngle) ?: 7.5
        val refIshaHa = SolarMath.hourAngle(refLat, decl, ishaAngle) ?: 7.5

        val core: Core = if (sunriseHa == null) {
            val refAsrHa = SolarMath.asrHourAngle(refLat, decl, shadowFactor) ?: 3.5
            val maghrib = base + refSunriseHa.hours
            Core(
                fajr = base - refFajrHa.hours,
                sunrise = base - refSunriseHa.hours,
                asr = base + refAsrHa.hours,
                maghrib = maghrib,
                isha = ishaInterval?.let { maghrib + it.minutes } ?: (base + refIshaHa.hours),
            )
        } else {
            val sunrise = base - sunriseHa.hours
            val maghrib = base + sunriseHa.hours
            // When even the Asr altitude is never reached, place Asr a little past mid-afternoon.
            val asr = base + (asrHa ?: (sunriseHa * 0.55)).hours
            val night = 24.0 - sunriseHa * 2
            if (fajrHa != null && ishaAvailable && condition == null) {
                Core(
                    fajr = base - fajrHa.hours,
                    sunrise = sunrise,
                    asr = asr,
                    maghrib = maghrib,
                    isha = ishaInterval?.let { maghrib + it.minutes } ?: (base + ishaHa!!.hours),
                )
            } else {
                if (condition == null) condition = ExtremeCondition.WHITE_NIGHTS
                val (fajrOffset, ishaOffset) = when (rule) {
                    HighLatitudeRule.NEAREST_LATITUDE ->
                        (refFajrHa - refSunriseHa) to (refIshaHa - refSunriseHa)
                    HighLatitudeRule.ONE_SEVENTH -> (night / 7) to (night / 7)
                    HighLatitudeRule.MIDDLE_OF_NIGHT -> (night / 2) to (night / 2)
                    HighLatitudeRule.ANGLE_BASED ->
                        (method.fajrAngle / 60.0 * night) to (method.ishaAngle / 60.0 * night)
                }
                Core(
                    fajr = sunrise - fajrOffset.hours,
                    sunrise = sunrise,
                    asr = asr,
                    maghrib = maghrib,
                    isha = if (ishaInterval != null && rule != HighLatitudeRule.MIDDLE_OF_NIGHT &&
                        rule != HighLatitudeRule.ANGLE_BASED
                    ) {
                        maghrib + ishaInterval.minutes
                    } else {
                        maghrib + ishaOffset.hours
                    },
                )
            }
        }

        val nextSunrise = core.sunrise + 24.hours
        val nightLength = nextSunrise - core.maghrib
        return PrayerTimes(
            date = date,
            fajr = core.fajr,
            sunrise = core.sunrise,
            dhuhr = base,
            asr = core.asr,
            maghrib = core.maghrib,
            isha = core.isha,
            midnight = core.maghrib + nightLength / 2,
            qiyam = nextSunrise - nightLength / 3,
            method = method,
            appliedRule = rule,
            condition = condition,
        )
    }

    private class Core(
        val fajr: Instant,
        val sunrise: Instant,
        val asr: Instant,
        val maghrib: Instant,
        val isha: Instant,
    )
}
