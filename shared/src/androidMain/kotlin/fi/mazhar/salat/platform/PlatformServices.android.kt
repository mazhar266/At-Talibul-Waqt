package fi.mazhar.salat.platform

actual fun createLocationService(): LocationService = AndroidLocationService()
actual fun createCompassService(): CompassService = AndroidCompassService()
actual fun createNotificationService(): NotificationService = AndroidNotificationService()
