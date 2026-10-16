package fi.mazhar.salat.platform

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal object Permissions {

    /**
     * One fixed registry key. ActivityResultRegistry keeps results for keys that have no callback
     * yet, so a recreated activity that registers the same key is handed a result that arrived
     * while it was being rebuilt.
     */
    private const val KEY = "talibulwaqt-permissions"

    private var pending: CancellableContinuation<Map<String, Boolean>>? = null
    private var pendingPermissions: Array<String>? = null
    private var launcher: ActivityResultLauncher<Array<String>>? = null

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Prompts through the foreground activity's result registry so no Activity subclass has to
     * forward callbacks. Without a visible activity, or while another prompt is already up (the
     * framework only allows one at a time), the current grant state is returned as-is.
     */
    suspend fun request(permissions: Array<String>): Map<String, Boolean> = withContext(Dispatchers.Main) {
        val app = AndroidAppContext.application
        if (permissions.all { isGranted(app, it) }) return@withContext permissions.associateWith { true }
        val activity = AndroidAppContext.currentActivity
        if (activity == null || pending != null) return@withContext permissions.associateWith { isGranted(app, it) }

        suspendCancellableCoroutine { continuation ->
            pending = continuation
            pendingPermissions = permissions
            continuation.invokeOnCancellation { clear() }
            register(activity).launch(permissions)
        }
    }

    /**
     * A configuration change (rotation, dark mode, …) while the system dialog is showing recreates
     * the activity underneath it, and the result is then delivered to the new instance's registry.
     * [AndroidAppContext] calls this for every new activity so the waiting request still gets its
     * answer instead of hanging for the rest of the process lifetime.
     */
    fun onActivityCreated(activity: ComponentActivity) {
        if (pending != null) register(activity)
    }

    private fun register(activity: ComponentActivity): ActivityResultLauncher<Array<String>> {
        launcher?.unregister()
        return activity.activityResultRegistry.register(
            KEY, ActivityResultContracts.RequestMultiplePermissions(),
        ) { result -> deliver(result) }.also { launcher = it }
    }

    private fun deliver(result: Map<String, Boolean>) {
        val continuation = pending ?: return
        val permissions = pendingPermissions ?: return
        clear()
        if (continuation.isActive) {
            val app = AndroidAppContext.application
            continuation.resume(permissions.associateWith { result[it] ?: isGranted(app, it) })
        }
    }

    private fun clear() {
        launcher?.unregister()
        launcher = null
        pending = null
        pendingPermissions = null
    }
}
