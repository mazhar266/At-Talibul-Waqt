package fi.mazhar.salat.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.time.Instant

@Serializable
enum class Prayer(val displayName: String, val arabicName: String) {
    FAJR("Fajr", "الفجر"),
    SUNRISE("Sunrise", "الشروق"),
    DHUHR("Dhuhr", "الظهر"),
    ASR("Asr", "العصر"),
    MAGHRIB("Maghrib", "المغرب"),
    ISHA("Isha", "العشاء");

    /** Sunrise marks the end of Fajr; it is not a prayer with its own adhkar session. */
    val isObligatory: Boolean get() = this != SUNRISE
}

/**
 * Twilight angles per convention. Umm al-Qura fixes Isha at a 90-minute interval after
 * Maghrib instead of an angle.
 */
@Serializable
enum class CalculationMethod(
    val label: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int? = null,
) {
    FINLAND("Islamic Cultural Centre of Finland", 15.0, 15.0),
    ECFR("European Council for Fatwa & Research (ECFR)", 18.0, 17.0),
    MWL("Muslim World League (MWL)", 18.0, 17.0),
    MAKKAH("Umm al-Qura University, Makkah", 18.5, 0.0, ishaIntervalMinutes = 90),
    ISNA("Islamic Society of North America (ISNA)", 15.0, 15.0),
    EGYPT("Egyptian General Authority of Survey", 19.5, 17.5),
    KARACHI("University of Islamic Sciences, Karachi", 18.0, 18.0),
}

@Serializable
enum class HighLatitudeRule(val label: String, val shortLabel: String) {
    NEAREST_LATITUDE("Nearest Latitude 45° (ECFR Standard)", "Nearest latitude"),
    ONE_SEVENTH("One-Seventh of Night Rule", "One-seventh"),
    MIDDLE_OF_NIGHT("Middle of the Night Rule", "Middle of night"),
    ANGLE_BASED("Angle-Based Rule (Angle / 60)", "Angle-based"),
}

@Serializable
enum class AsrJuristic(val label: String, val shadowFactor: Int) {
    STANDARD("Standard (Shafi'i, Maliki, Hanbali)", 1),
    HANAFI("Hanafi", 2),
}

/** Why the standard angle-based twilight could not be used for this day and place. */
enum class ExtremeCondition(val label: String) {
    POLAR_DAY("Polar Day"),
    POLAR_NIGHT("Polar Night"),
    WHITE_NIGHTS("White Nights"),
    ABNORMAL_TWILIGHT("Abnormal Twilight"),
}

@Serializable
data class GeoLocation(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
) {
    val isHighLatitude: Boolean get() = abs(latitude) >= 50.0

    val displayName: String get() = if (country.isBlank()) name else "$name, $country"
}

object PresetLocations {
    val helsinki = GeoLocation("Helsinki", "Finland", 60.1699, 24.9384)

    val all: List<GeoLocation> = listOf(
        helsinki,
        GeoLocation("Tampere", "Finland", 61.4978, 23.7610),
        GeoLocation("Oulu", "Finland", 65.0121, 25.4651),
        GeoLocation("Rovaniemi (Lapland)", "Finland", 66.5039, 25.7294),
        GeoLocation("Utsjoki (Arctic Circle)", "Finland", 69.9086, 27.0284),
        GeoLocation("North Pole", "Arctic", 90.0, 0.0),
        GeoLocation("Longyearbyen", "Svalbard", 78.2232, 15.6267),
        GeoLocation("Mecca", "Saudi Arabia", 21.4225, 39.8262),
        GeoLocation("Medina", "Saudi Arabia", 24.5247, 39.5692),
        GeoLocation("London", "United Kingdom", 51.5074, -0.1278),
        GeoLocation("New York", "United States", 40.7128, -74.0060),
        GeoLocation("Istanbul", "Turkey", 41.0082, 28.9784),
        GeoLocation("Dubai", "United Arab Emirates", 25.2048, 55.2708),
        GeoLocation("Kuala Lumpur", "Malaysia", 3.1390, 101.6869),
    )
}

@Serializable
data class CalculationSettings(
    /** null = pick automatically: the Finnish convention at or above 55°N, MWL elsewhere. */
    val method: CalculationMethod? = null,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.NEAREST_LATITUDE,
    val asrJuristic: AsrJuristic = AsrJuristic.STANDARD,
) {
    fun resolvedMethod(latitude: Double): CalculationMethod =
        method ?: if (latitude >= 55.0) CalculationMethod.FINLAND else CalculationMethod.MWL
}

data class PrayerTimes(
    val date: LocalDate,
    val fajr: Instant,
    val sunrise: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val maghrib: Instant,
    val isha: Instant,
    /** Halfway between tonight's Maghrib and tomorrow's sunrise. */
    val midnight: Instant,
    /** Start of the last third of tonight. */
    val qiyam: Instant,
    val method: CalculationMethod,
    val appliedRule: HighLatitudeRule,
    val condition: ExtremeCondition?,
) {
    val isExtremeLatitude: Boolean get() = condition != null

    fun timeOf(prayer: Prayer): Instant = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }

    val ordered: List<Pair<Prayer, Instant>> get() = Prayer.entries.map { it to timeOf(it) }
}
