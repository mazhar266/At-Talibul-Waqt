package fi.mazhar.salat.platform

import android.hardware.SensorManager
import android.view.Surface
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidCompassServiceTest {

    @Test
    fun portraitKeepsDeviceAxes() {
        assertEquals(SensorManager.AXIS_X to SensorManager.AXIS_Y, AndroidCompassService.screenAxes(Surface.ROTATION_0))
    }

    @Test
    fun landscapeRotationsRemapToScreenUp() {
        assertEquals(SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X, AndroidCompassService.screenAxes(Surface.ROTATION_90))
        assertEquals(SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y, AndroidCompassService.screenAxes(Surface.ROTATION_180))
        assertEquals(SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X, AndroidCompassService.screenAxes(Surface.ROTATION_270))
    }

    @Test
    fun trueHeadingAddsDeclinationAndWraps() {
        assertEquals(10f, AndroidCompassService.trueHeading(magneticAzimuth = 0f, declination = 10f))
        assertEquals(5f, AndroidCompassService.trueHeading(magneticAzimuth = 350f, declination = 15f))
        // getOrientation reports azimuth in (-180, 180]; negative values must land in [0, 360).
        assertEquals(350f, AndroidCompassService.trueHeading(magneticAzimuth = -10f, declination = 0f))
        assertEquals(355f, AndroidCompassService.trueHeading(magneticAzimuth = 10f, declination = -15f))
    }
}
