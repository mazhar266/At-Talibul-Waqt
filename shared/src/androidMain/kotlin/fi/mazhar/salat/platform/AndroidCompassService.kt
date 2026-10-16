package fi.mazhar.salat.platform

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidCompassService : CompassService {

    private val sensorManager: SensorManager
        get() = AndroidAppContext.application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override val isSupported: Boolean
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
            sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null

    override fun headings(latitude: Double, longitude: Double): Flow<Float> = callbackFlow {
        val manager = sensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        // The rotation vector is referenced to magnetic north; declination turns it into true north.
        val declination = GeomagneticField(
            latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis(),
        ).declination
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                trySend(((azimuth + declination) % 360f + 360f) % 360f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { manager.unregisterListener(listener) }
    }
}
