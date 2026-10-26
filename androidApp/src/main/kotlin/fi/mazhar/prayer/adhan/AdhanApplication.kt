package fi.mazhar.prayer.adhan

import android.app.Application
import fi.mazhar.prayer.adhan.platform.initAdhanApp

class AdhanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAdhanApp(this)
    }
}
