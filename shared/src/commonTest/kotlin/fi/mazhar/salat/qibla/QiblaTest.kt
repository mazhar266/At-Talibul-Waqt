package fi.mazhar.salat.qibla

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QiblaTest {

    @Test
    fun helsinkiBearingAndDistanceMatchNoorSalat() {
        val info = Qibla.info(60.1699, 24.9384)
        assertTrue(abs(info.bearing - 158.2) < 0.2, "bearing ${info.bearing}")
        assertTrue(abs(info.distanceKm - 4464) < 5, "distance ${info.distanceKm}")
        assertEquals("SSE", info.compassPoint)
    }

    @Test
    fun newYorkFacesNorthEast() {
        val bearing = Qibla.bearing(40.7128, -74.0060)
        assertTrue(bearing in 57.0..59.0, "bearing $bearing")
    }

    @Test
    fun northPoleBearingIsAlongTheKaabaMeridian() {
        val bearing = Qibla.bearing(90.0, 0.0)
        assertTrue(abs(bearing - (180.0 - Qibla.KAABA_LONGITUDE)) < 0.01, "bearing $bearing")
    }

    @Test
    fun relativeBearingWrapsAroundNorth() {
        assertEquals(20.0, Qibla.relativeBearing(target = 10.0, heading = 350.0), 1e-9)
        assertEquals(-20.0, Qibla.relativeBearing(target = 350.0, heading = 10.0), 1e-9)
        assertEquals(180.0, Qibla.relativeBearing(target = 180.0, heading = 0.0), 1e-9)
    }

    @Test
    fun compassPointsCoverTheRose() {
        assertEquals("N", Qibla.compassPoint(359.0))
        assertEquals("E", Qibla.compassPoint(90.0))
        assertEquals("SW", Qibla.compassPoint(225.0))
    }
}
