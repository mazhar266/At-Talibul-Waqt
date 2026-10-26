package fi.mazhar.prayer.adhan.platform

import fi.mazhar.prayer.adhan.model.AdhanSounds
import fi.mazhar.prayer.adhan.model.Prayer
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

sealed interface LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult
    data object PermissionDenied : LocationResult
    data object Unavailable : LocationResult
    data class Error(val message: String) : LocationResult
}

interface LocationService {
    val isSupported: Boolean
    /** Asks for permission if needed, then resolves a single fix. */
    suspend fun requestCurrentLocation(): LocationResult
}

interface CompassService {
    val isSupported: Boolean
    /**
     * True heading in degrees (0 = geographic north), as fast as the sensors deliver it.
     * The user's position is needed to correct magnetic declination, which is large in the Arctic.
     */
    fun headings(latitude: Double, longitude: Double): Flow<Float>
}

data class PrayerNotification(
    /** Stable per slot so re-scheduling replaces rather than duplicates. */
    val id: Int,
    val prayer: Prayer,
    val time: Instant,
    val title: String,
    val body: String,
    /** [AdhanSounds] id to sound with it; "beep" means the platform's default notification sound. */
    val adhanSoundId: String = AdhanSounds.DEFAULT_ID,
)

interface NotificationService {
    val isSupported: Boolean
    /** How many days ahead to plan; bounded by platform limits on pending notifications. */
    val planningHorizonDays: Int
    suspend fun requestPermission(): Boolean
    /** Replace every pending prayer notification with [notifications]. */
    fun schedule(notifications: List<PrayerNotification>)
    fun cancelAll()
}

expect fun createLocationService(): LocationService
expect fun createCompassService(): CompassService
expect fun createNotificationService(): NotificationService
