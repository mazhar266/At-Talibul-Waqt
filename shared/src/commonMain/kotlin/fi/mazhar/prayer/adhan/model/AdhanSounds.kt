package fi.mazhar.prayer.adhan.model

/**
 * One entry of the "Adhan sound" picker. Recordings live under composeResources/files/adhan with
 * their provenance in CREDITS.md next to them; [resourcePath] is null only for the platform beep.
 */
data class AdhanSound(
    /** Stable id stored in [UserSettings.adhanSound]; never rename one that has shipped. */
    val id: String,
    /** Muadhdhin, or the place when the source does not name them. */
    val title: String,
    /** Place, occasion and style on the line under the title. */
    val subtitle: String,
    /** Who to credit, as the licence requires; blank for the built-ins. */
    val credit: String,
    val licence: String,
    val sourceUrl: String?,
    val durationSeconds: Int,
    /** Compose resource path readable with `Res.readBytes`; null for the beep. */
    val resourcePath: String?,
    /** File in the iOS app bundle (≤ 30 s) used as the notification sound; null means the default sound. */
    val iosNotificationClip: String?,
) {
    val isBeep: Boolean get() = resourcePath == null

    /** "3:17" style label. */
    val durationLabel: String
        get() = "${durationSeconds / 60}:${(durationSeconds % 60).toString().padStart(2, '0')}"
}

/**
 * The bundled adhan recordings plus the two built-ins. Only recordings with an explicit
 * redistributable licence are included; famous studio adhans without one were deliberately left out.
 */
object AdhanSounds {
    const val BEEP_ID = "beep"
    const val TONE_ID = "tone"
    const val DEFAULT_ID = "makkah_haram"

    val recordings: List<AdhanSound> = listOf(
        AdhanSound(
            id = "makkah_haram",
            title = "Masjid al-Haram, Makkah",
            subtitle = "Adhan at the Great Mosque, January 2013 · Hijazi style",
            credit = "Seyfula Islam via Wikimedia Commons",
            licence = "CC BY 3.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Adhan,_Great_Mosque_of_Mecca_-_Jan_21,_2013.webm",
            durationSeconds = 197,
            resourcePath = "files/adhan/makkah_haram.mp3",
            iosNotificationClip = "adhan_makkah_haram.caf",
        ),
        AdhanSound(
            id = "madinah_nabawi",
            title = "Masjid an-Nabawi, Madinah",
            subtitle = "Adhan at the Prophet's Mosque · Hijazi style",
            credit = "ejaz215 (Freesound) via Wikimedia Commons",
            licence = "CC BY 3.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:33937_ejaz215_call-to-prayer-from-the-prophet-s-mo.ogg",
            durationSeconds = 186,
            resourcePath = "files/adhan/madinah_nabawi.mp3",
            iosNotificationClip = "adhan_madinah_nabawi.caf",
        ),
        AdhanSound(
            id = "malmo_besim_azemi",
            title = "Besim Azemi",
            subtitle = "Fajr adhan, Malmö Mosque, Eid al-Fitr 2012 · Balkan style",
            credit = "Islamic Center Malmö via Wikimedia Commons",
            licence = "CC BY 3.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Eid_al-Fitr_Fajr_azan_at_Malm%C3%B6_Mosque_-_19_August_2012.webm",
            durationSeconds = 245,
            resourcePath = "files/adhan/malmo_besim_azemi.mp3",
            iosNotificationClip = "adhan_malmo_besim_azemi.caf",
        ),
        AdhanSound(
            id = "doha_qatar",
            title = "Doha, Qatar",
            subtitle = "Dhuhr adhan recorded in Doha, 2013–14 · Gulf style",
            credit = "Internet Archive, uploader's own recording",
            licence = "Public Domain Mark",
            sourceUrl = "https://archive.org/details/adhan.recordings.from.doha.qatar",
            durationSeconds = 179,
            resourcePath = "files/adhan/doha_qatar.mp3",
            iosNotificationClip = "adhan_doha_qatar.caf",
        ),
        AdhanSound(
            id = "istanbul",
            title = "Istanbul, Türkiye",
            subtitle = "Ezan recorded in Istanbul, September 2025 (opening excerpt) · Turkish style",
            credit = "Viceskeeni2 via Wikimedia Commons",
            licence = "CC0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Adhan_in_Istanbul.webm",
            durationSeconds = 50,
            resourcePath = "files/adhan/istanbul.mp3",
            iosNotificationClip = "adhan_istanbul.caf",
        ),
        AdhanSound(
            id = "adam_synagda",
            title = "Adam-synagda",
            subtitle = "Studio recitation of the full adhan, 2022",
            credit = "Adam-synagda via Wikimedia Commons",
            licence = "CC0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg",
            durationSeconds = 154,
            resourcePath = "files/adhan/adam_synagda.mp3",
            iosNotificationClip = "adhan_adam_synagda.caf",
        ),
    )

    /** The synthesised sequence that mirrors the web app's adhan sample (tools/make_adhan_clip.py). */
    val tone = AdhanSound(
        id = TONE_ID,
        title = "Tone",
        subtitle = "Synthesised tone sequence, as on salat.mazhar.fi",
        credit = "",
        licence = "Generated in-project",
        sourceUrl = null,
        durationSeconds = 14,
        resourcePath = "files/adhan/tone.wav",
        iosNotificationClip = "adhan_tone.caf",
    )

    /** The platform's default notification sound; nothing of ours is played. */
    val beep = AdhanSound(
        id = BEEP_ID,
        title = "Beep",
        subtitle = "Notification sound only — the device's default alert",
        credit = "",
        licence = "",
        sourceUrl = null,
        durationSeconds = 0,
        resourcePath = null,
        iosNotificationClip = null,
    )

    val all: List<AdhanSound> = recordings + tone + beep

    /** Resolves a stored id, falling back to the default for ids from a build that no longer has them. */
    fun byId(id: String?): AdhanSound = all.firstOrNull { it.id == id } ?: all.first { it.id == DEFAULT_ID }
}
