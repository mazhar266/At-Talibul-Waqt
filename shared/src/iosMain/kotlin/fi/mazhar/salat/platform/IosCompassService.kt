package fi.mazhar.salat.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.darwin.NSObject

class IosCompassService : CompassService {

    override val isSupported: Boolean
        get() = CLLocationManager.headingAvailable()

    override fun headings(latitude: Double, longitude: Double): Flow<Float> = callbackFlow {
        val manager = CLLocationManager()
        val delegate = HeadingDelegate { trySend(it) }
        manager.delegate = delegate
        manager.headingFilter = 1.0
        manager.startUpdatingHeading()
        awaitClose {
            manager.stopUpdatingHeading()
            // Referencing `delegate` here keeps it alive for as long as the manager needs it.
            if (manager.delegate === delegate) manager.delegate = null
        }
    }.flowOn(Dispatchers.Main)

    private class HeadingDelegate(private val onHeading: (Float) -> Unit) : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
            // trueHeading is negative when Core Location cannot determine it (no location fix yet).
            val heading = if (didUpdateHeading.trueHeading >= 0) didUpdateHeading.trueHeading else didUpdateHeading.magneticHeading
            onHeading(heading.toFloat())
        }
    }
}
