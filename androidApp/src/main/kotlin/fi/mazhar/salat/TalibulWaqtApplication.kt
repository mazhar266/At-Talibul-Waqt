package fi.mazhar.salat

import android.app.Application
import fi.mazhar.salat.platform.initTalibulWaqt

class TalibulWaqtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initTalibulWaqt(this)
    }
}
