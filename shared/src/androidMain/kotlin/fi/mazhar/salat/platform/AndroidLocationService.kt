package fi.mazhar.salat.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Framework LocationManager only — no Play Services, so it also runs on de-Googled devices. */
class AndroidLocationService : LocationService {

    override val isSupported: Boolean = true

    @SuppressLint("MissingPermission")
    override suspend fun requestCurrentLocation(): LocationResult {
        val context = AndroidAppContext.application
        val granted = Permissions.request(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
        if (granted.values.none { it }) return LocationResult.PermissionDenied

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return LocationResult.Unavailable

        val recent = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos < RECENT_FIX_NANOS }
            .maxByOrNull { it.time }
        if (recent != null) return LocationResult.Success(recent.latitude, recent.longitude)

        for (provider in providers) {
            val fix = withTimeoutOrNull(FIX_TIMEOUT_MS) { currentLocation(context, manager, provider) }
            if (fix != null) return LocationResult.Success(fix.latitude, fix.longitude)
        }
        val stale = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        return stale?.let { LocationResult.Success(it.latitude, it.longitude) } ?: LocationResult.Unavailable
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(context: Context, manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) {
                if (continuation.isActive) continuation.resume(it)
            }
        }

    private companion object {
        const val RECENT_FIX_NANOS = 10L * 60 * 1_000_000_000
        const val FIX_TIMEOUT_MS = 15_000L
    }
}
