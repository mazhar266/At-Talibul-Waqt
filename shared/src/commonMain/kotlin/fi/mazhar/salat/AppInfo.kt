package fi.mazhar.salat

object AppInfo {
    const val NAME = "TalibulWaqt"
    const val WEBSITE = "salat.mazhar.fi"

    /** The person behind the app, as shown on the Settings screen. */
    val developer = Developer(
        name = "Mazhar Ahmed",
        credentials = listOf(
            "BA in Islamic Studies from IOU",
            "Dawra-e-Hadith from Qawmi Madrasa, Bangladesh",
        ),
    )
}

data class Developer(val name: String, val credentials: List<String>)

/** Marketing version (and build number where the platform has one), e.g. "1.0.0 (1)". */
expect fun appVersion(): String
