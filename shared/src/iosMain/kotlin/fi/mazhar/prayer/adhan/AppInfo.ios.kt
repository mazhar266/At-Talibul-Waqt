package fi.mazhar.prayer.adhan

import platform.Foundation.NSBundle

actual fun appVersion(): String {
    val info = NSBundle.mainBundle.infoDictionary ?: return "unknown"
    val version = info["CFBundleShortVersionString"] as? String ?: "?"
    val build = info["CFBundleVersion"] as? String
    return if (build == null) version else "$version ($build)"
}
