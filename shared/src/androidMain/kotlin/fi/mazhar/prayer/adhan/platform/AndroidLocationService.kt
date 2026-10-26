package fi.mazhar.prayer.adhan.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
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
        val fine = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!fine && !coarse) return LocationResult.PermissionDenied

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = usableProviders(
            fineGranted = fine,
            sdkInt = Build.VERSION.SDK_INT,
            enabled = { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) },
        )
        if (providers.isEmpty()) return LocationResult.Unavailable

        val recent = lastKnown(manager, providers)
            .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos < RECENT_FIX_NANOS }
            .maxByOrNull { it.time }
        if (recent != null) return LocationResult.Success(recent.latitude, recent.longitude)

        for (provider in providers) {
            val fix = withTimeoutOrNull(FIX_TIMEOUT_MS) { currentLocation(context, manager, provider) }
            if (fix != null) return LocationResult.Success(fix.latitude, fix.longitude)
        }
        val stale = lastKnown(manager, providers).maxByOrNull { it.time }
        return stale?.let { LocationResult.Success(it.latitude, it.longitude) } ?: LocationResult.Unavailable
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager, providers: List<String>): List<Location> =
        providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }

    /**
     * One fix from [provider], or null when it times out, when the provider refuses the caller
     * (a SecurityException: e.g. GPS with approximate-only permission before Android 12) or when
     * it disappeared since the enabled check. A refusal must not escape: this runs inside the
     * view model's scope, where an uncaught exception takes the whole app down.
     */
    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(context: Context, manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) {
                    if (continuation.isActive) continuation.resume(it)
                }
            } catch (e: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            } catch (e: IllegalArgumentException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    companion object {
        private const val RECENT_FIX_NANOS = 10L * 60 * 1_000_000_000
        private const val FIX_TIMEOUT_MS = 15_000L

        /**
         * Providers worth asking, best first, given what the user granted. Before Android 12 the
         * GPS and passive providers throw a SecurityException for callers that hold only
         * ACCESS_COARSE_LOCATION, so with approximate-only permission just the network provider
         * is used there; Android 12+ serves coarsened fixes from every provider.
         */
        internal fun usableProviders(fineGranted: Boolean, sdkInt: Int, enabled: (String) -> Boolean): List<String> {
            val fineOnlyProviders = setOf(LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            val allowFineOnly = fineGranted || sdkInt >= Build.VERSION_CODES.S
            return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                .filter { allowFineOnly || it !in fineOnlyProviders }
                .filter(enabled)
        }
    }
}
