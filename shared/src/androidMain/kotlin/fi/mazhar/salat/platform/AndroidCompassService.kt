package fi.mazhar.salat.platform

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Surface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class AndroidCompassService : CompassService {

    private val context: Context get() = AndroidAppContext.application

    private val sensorManager: SensorManager
        get() = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override val isSupported: Boolean get() = headingSensor(sensorManager) != null

    override fun headings(latitude: Double, longitude: Double): Flow<Float> = callbackFlow {
        val manager = sensorManager
        val sensor = headingSensor(manager)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        // The rotation vector is referenced to magnetic north; declination turns it into true north.
        val declination = GeomagneticField(
            latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis(),
        ).declination
        val rotation = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                // Measure the heading of "up on the screen", not of the device's long edge, so the
                // dial stays right when the phone is held in landscape.
                val (x, y) = screenAxes(displayRotation())
                SensorManager.remapCoordinateSystem(rotation, x, y, remapped)
                SensorManager.getOrientation(remapped, orientation)
                trySend(trueHeading(Math.toDegrees(orientation[0].toDouble()).toFloat(), declination))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { manager.unregisterListener(listener) }
    }.conflate() // the UI only needs the latest reading; never let slow frames queue stale ones

    private fun headingSensor(manager: SensorManager): Sensor? =
        manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

    private fun displayRotation(): Int {
        val activity = AndroidAppContext.currentActivity
        val display: Display? = when {
            activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> activity.display
            activity != null -> @Suppress("DEPRECATION") activity.windowManager.defaultDisplay
            // An Application context has no display of its own; fall back to the default one.
            else -> (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY)
        }
        return display?.rotation ?: Surface.ROTATION_0
    }

    companion object {
        /** Device axes to treat as screen X and screen Y for a [Surface] rotation (remapCoordinateSystem arguments). */
        internal fun screenAxes(rotation: Int): Pair<Int, Int> = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }

        /** Magnetic azimuth corrected by declination, normalised to [0, 360). */
        internal fun trueHeading(magneticAzimuth: Float, declination: Float): Float =
            ((magneticAzimuth + declination) % 360f + 360f) % 360f
    }
}
