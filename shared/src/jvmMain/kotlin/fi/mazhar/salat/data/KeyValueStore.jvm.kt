package fi.mazhar.salat.data

import java.io.File
import java.util.Properties

actual fun createKeyValueStore(): KeyValueStore = FileKeyValueStore(
    File(System.getProperty("user.home"), ".talibulwaqt/store.properties"),
)

/** Properties file in the user's home; the registry-backed java.util.prefs caps values at 8 KB. */
class FileKeyValueStore(private val file: File) : KeyValueStore {
    private val props = Properties().apply {
        if (file.exists()) file.inputStream().use { load(it) }
    }

    override fun getString(key: String): String? = props.getProperty(key)

    override fun putString(key: String, value: String) {
        props.setProperty(key, value)
        persist()
    }

    override fun remove(key: String) {
        props.remove(key)
        persist()
    }

    private fun persist() {
        file.parentFile?.mkdirs()
        file.outputStream().use { props.store(it, "TalibulWaqt") }
    }
}
