package fi.mazhar.prayer.adhan.platform

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

/**
 * Process-wide handles the platform services need: the application context for system services
 * and the foreground activity for runtime-permission prompts. Call [initAdhanApp] from
 * `Application.onCreate`.
 */
object AndroidAppContext {
    lateinit var application: Application
        private set

    @Volatile
    private var activityRef: WeakReference<ComponentActivity>? = null

    val currentActivity: ComponentActivity? get() = activityRef?.get()

    val isInitialized: Boolean get() = ::application.isInitialized

    fun init(app: Application) {
        if (isInitialized) return
        application = app
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                track(activity)
                // Lets a permission prompt that outlived its activity (rotation) deliver its result.
                if (activity is ComponentActivity) Permissions.onActivityCreated(activity)
            }
            override fun onActivityStarted(activity: Activity) = track(activity)
            override fun onActivityResumed(activity: Activity) = track(activity)
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) {
                if (activityRef?.get() === activity) activityRef = null
            }

            private fun track(activity: Activity) {
                if (activity is ComponentActivity) activityRef = WeakReference(activity)
            }
        })
    }

    /** Receivers may run before any Activity exists; make sure the context is available. */
    fun ensureInit(context: Context) {
        if (!isInitialized) init(context.applicationContext as Application)
    }
}

fun initAdhanApp(app: Application) = AndroidAppContext.init(app)
