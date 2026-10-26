package fi.mazhar.prayer.adhan.qibla

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class QiblaInfo(
    /** True (geodesic) bearing from the user to the Kaaba, 0..360. */
    val bearing: Double,
    val distanceKm: Double,
    val compassPoint: String,
)

object Qibla {
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0

    fun info(latitude: Double, longitude: Double): QiblaInfo {
        val bearing = bearing(latitude, longitude)
        return QiblaInfo(bearing, distanceKm(latitude, longitude), compassPoint(bearing))
    }

    /** Great-circle initial bearing. At the poles every direction is "south"/"north", so the
     *  bearing degenerates to a longitude difference measured from the 0° meridian. */
    fun bearing(latitude: Double, longitude: Double): Double {
        if (latitude >= 89.99) return normalize(180.0 - (KAABA_LONGITUDE - longitude))
        if (latitude <= -89.99) return normalize(KAABA_LONGITUDE - longitude)
        val phi = rad(latitude)
        val phiK = rad(KAABA_LATITUDE)
        val dLambda = rad(KAABA_LONGITUDE - longitude)
        val y = sin(dLambda)
        val x = cos(phi) * tan(phiK) - sin(phi) * cos(dLambda)
        return normalize(deg(atan2(y, x)))
    }

    fun distanceKm(latitude: Double, longitude: Double): Double {
        val phi1 = rad(latitude)
        val phi2 = rad(KAABA_LATITUDE)
        val dPhi = rad(KAABA_LATITUDE - latitude)
        val dLambda = rad(KAABA_LONGITUDE - longitude)
        val a = sin(dPhi / 2) * sin(dPhi / 2) + cos(phi1) * cos(phi2) * sin(dLambda / 2) * sin(dLambda / 2)
        return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun compassPoint(bearing: Double): String {
        val points = listOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
        )
        return points[((normalize(bearing) / 22.5) + 0.5).toInt() % 16]
    }

    /** Degrees the user must turn: positive = clockwise/right, in (-180, 180]. */
    fun relativeBearing(target: Double, heading: Double): Double {
        var delta = (target - heading) % 360.0
        if (delta > 180) delta -= 360
        if (delta <= -180) delta += 360
        return delta
    }

    fun normalize(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

    private fun rad(d: Double) = d * PI / 180.0
    private fun deg(r: Double) = r * 180.0 / PI
}
