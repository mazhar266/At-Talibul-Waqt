package fi.mazhar.salat

/** jpackage stamps the installer's version into this property; a `gradle run` has none. */
actual fun appVersion(): String = System.getProperty("jpackage.app-version") ?: "dev build"
