package fi.mazhar.prayer.adhan.data

import android.content.Context
import android.content.SharedPreferences
import fi.mazhar.prayer.adhan.platform.AndroidAppContext

actual fun createKeyValueStore(): KeyValueStore = SharedPreferencesKeyValueStore(
    AndroidAppContext.application.getSharedPreferences("adhan", Context.MODE_PRIVATE),
)

class SharedPreferencesKeyValueStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun remove(key: String) = prefs.edit().remove(key).apply()
}
