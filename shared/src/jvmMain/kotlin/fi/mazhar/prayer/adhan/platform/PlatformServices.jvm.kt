package fi.mazhar.prayer.adhan.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// Desktop has no GPS, compass or lock-screen alarms: the UI falls back to preset locations,
// a static Qibla dial and in-app countdowns.

actual fun createLocationService(): LocationService = object : LocationService {
    override val isSupported: Boolean = false
    override suspend fun requestCurrentLocation(): LocationResult = LocationResult.Unavailable
}

actual fun createCompassService(): CompassService = object : CompassService {
    override val isSupported: Boolean = false
    override fun headings(latitude: Double, longitude: Double): Flow<Float> = emptyFlow()
}

actual fun createNotificationService(): NotificationService = object : NotificationService {
    override val isSupported: Boolean = false
    override val planningHorizonDays: Int = 0
    override suspend fun requestPermission(): Boolean = false
    override fun schedule(notifications: List<PrayerNotification>) = Unit
    override fun cancelAll() = Unit
}
