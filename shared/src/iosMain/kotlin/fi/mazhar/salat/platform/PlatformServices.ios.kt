package fi.mazhar.salat.platform

actual fun createLocationService(): LocationService = IosLocationService()
actual fun createCompassService(): CompassService = IosCompassService()
actual fun createNotificationService(): NotificationService = IosNotificationService()
