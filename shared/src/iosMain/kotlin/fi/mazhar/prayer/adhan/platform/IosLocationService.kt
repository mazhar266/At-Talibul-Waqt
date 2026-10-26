package fi.mazhar.prayer.adhan.platform

import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

class IosLocationService : LocationService {

    override val isSupported: Boolean = true

    // Strong references: CLLocationManager only holds its delegate weakly.
    private var manager: CLLocationManager? = null
    private var delegate: LocationDelegate? = null

    override suspend fun requestCurrentLocation(): LocationResult = withContext(Dispatchers.Main) {
        val result = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val manager = CLLocationManager()
                val delegate = LocationDelegate(manager) { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
                this@IosLocationService.manager = manager
                this@IosLocationService.delegate = delegate
                manager.delegate = delegate
                manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
                delegate.start()
                continuation.invokeOnCancellation { manager.stopUpdatingLocation() }
            }
        }
        manager?.delegate = null
        manager = null
        delegate = null
        result ?: LocationResult.Unavailable
    }

    private class LocationDelegate(
        private val manager: CLLocationManager,
        private val onResult: (LocationResult) -> Unit,
    ) : NSObject(), CLLocationManagerDelegateProtocol {

        fun start() = handle(manager.authorizationStatus)

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) =
            handle(manager.authorizationStatus)

        private fun handle(status: CLAuthorizationStatus) {
            when (status) {
                kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
                kCLAuthorizationStatusAuthorizedWhenInUse,
                kCLAuthorizationStatusAuthorizedAlways,
                -> manager.requestLocation()
                else -> onResult(LocationResult.PermissionDenied)
            }
        }

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            location.coordinate.useContents { onResult(LocationResult.Success(latitude, longitude)) }
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            onResult(LocationResult.Error(didFailWithError.localizedDescription))
        }
    }

    private companion object {
        const val TIMEOUT_MS = 20_000L
    }
}
