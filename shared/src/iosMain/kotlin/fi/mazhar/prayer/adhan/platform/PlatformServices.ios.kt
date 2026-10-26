package fi.mazhar.prayer.adhan.platform

actual fun createLocationService(): LocationService = IosLocationService()
actual fun createCompassService(): CompassService = IosCompassService()
actual fun createNotificationService(): NotificationService = IosNotificationService()
