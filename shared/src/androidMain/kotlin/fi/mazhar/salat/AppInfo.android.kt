package fi.mazhar.salat

import androidx.core.content.pm.PackageInfoCompat
import fi.mazhar.salat.platform.AndroidAppContext

actual fun appVersion(): String {
    val context = AndroidAppContext.application
    @Suppress("DEPRECATION")
    val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        ?: return "unknown"
    return "${info.versionName ?: "?"} (${PackageInfoCompat.getLongVersionCode(info)})"
}
