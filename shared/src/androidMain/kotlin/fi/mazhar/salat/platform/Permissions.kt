package fi.mazhar.salat.platform

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal object Permissions {

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Prompts through the foreground activity's result registry so no Activity subclass has to
     * forward callbacks. Without a visible activity the current grant state is returned as-is.
     */
    suspend fun request(permissions: Array<String>): Map<String, Boolean> = withContext(Dispatchers.Main) {
        val app = AndroidAppContext.application
        if (permissions.all { isGranted(app, it) }) return@withContext permissions.associateWith { true }
        val activity = AndroidAppContext.currentActivity
            ?: return@withContext permissions.associateWith { isGranted(app, it) }

        suspendCancellableCoroutine { continuation ->
            lateinit var launcher: ActivityResultLauncher<Array<String>>
            launcher = activity.activityResultRegistry.register(
                "talibulwaqt-permissions-${System.nanoTime()}",
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { result ->
                launcher.unregister()
                if (continuation.isActive) {
                    continuation.resume(permissions.associateWith { result[it] ?: isGranted(activity, it) })
                }
            }
            continuation.invokeOnCancellation { launcher.unregister() }
            launcher.launch(permissions)
        }
    }
}
