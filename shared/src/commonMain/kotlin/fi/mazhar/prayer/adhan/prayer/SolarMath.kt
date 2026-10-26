package fi.mazhar.prayer.adhan.prayer

import kotlinx.datetime.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Low-precision solar position (the algorithm used by praytimes.org and the NoorSalat web app). */
internal object SolarMath {

    data class SunPosition(
        /** Degrees. */
        val declination: Double,
        /** Hours, normalised to (-12, 12]. */
        val equationOfTime: Double,
    )

    /** Julian day at 0h UT of the given calendar date. */
    fun julianDay(date: LocalDate): Double {
        var year = date.year
        var month = date.month.ordinal + 1
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + date.day + b - 1524.5
    }

    fun sunPosition(julianDay: Double): SunPosition {
        val d = julianDay - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d
        val rightAscension = fixHour(atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0)
        val declination = asinDeg(sinDeg(e) * sinDeg(l))
        var eqt = (q / 15.0 - rightAscension) % 24.0
        if (eqt > 12) eqt -= 24
        if (eqt <= -12) eqt += 24
        return SunPosition(declination, eqt)
    }

    /**
     * Hours between solar noon and the moment the sun's centre reaches [angle] degrees of
     * altitude, or null when the sun never reaches that altitude on this day.
     */
    fun hourAngle(latitude: Double, declination: Double, angle: Double): Double? {
        val cosH = (sinDeg(angle) - sinDeg(latitude) * sinDeg(declination)) /
            (cosDeg(latitude) * cosDeg(declination))
        if (cosH > 1 || cosH < -1) return null
        return acosDeg(cosH) / 15.0
    }

    /** Hour angle for Asr: shadow length = [shadowFactor] × object + noon shadow. */
    fun asrHourAngle(latitude: Double, declination: Double, shadowFactor: Int): Double? {
        val altitude = atanDeg(1.0 / (shadowFactor + tanDeg(abs(latitude - declination))))
        return hourAngle(latitude, declination, altitude)
    }

    fun fixAngle(a: Double): Double = ((a % 360.0) + 360.0) % 360.0
    fun fixHour(h: Double): Double = ((h % 24.0) + 24.0) % 24.0

    private fun rad(d: Double) = d * PI / 180.0
    private fun deg(r: Double) = r * 180.0 / PI
    private fun sinDeg(d: Double) = sin(rad(d))
    private fun cosDeg(d: Double) = cos(rad(d))
    private fun tanDeg(d: Double) = tan(rad(d))
    private fun asinDeg(x: Double) = deg(asin(x))
    private fun acosDeg(x: Double) = deg(acos(x))
    private fun atanDeg(x: Double) = deg(atan(x))
    private fun atan2Deg(y: Double, x: Double) = deg(atan2(y, x))
}
