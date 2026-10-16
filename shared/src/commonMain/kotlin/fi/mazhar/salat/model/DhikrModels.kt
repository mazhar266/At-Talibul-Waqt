package fi.mazhar.salat.model

import kotlinx.serialization.Serializable

@Serializable
enum class DhikrCategory(val label: String) {
    AFTER_PRAYER("After Prayer (Sunnah)"),
    MORNING("Morning Adhkar"),
    EVENING("Evening Adhkar"),
    DAILY("Daily Remembrance"),
    QURANIC("Quranic Duas"),
    HARDSHIP("Relief & Hardship"),
}

@Serializable
data class Dhikr(
    val id: String,
    val title: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val benefit: String = "",
    val reference: String = "",
    val targetCount: Int = 1,
    val category: DhikrCategory = DhikrCategory.DAILY,
    val isCustom: Boolean = false,
)

/**
 * Which post-prayer routine a list belongs to. [ALL] is the fallback used by any prayer that
 * has no list of its own.
 */
@Serializable
enum class PrayerSlot(val label: String, val prayer: Prayer?) {
    ALL("All Prayers", null),
    FAJR("Fajr", Prayer.FAJR),
    DHUHR("Dhuhr", Prayer.DHUHR),
    ASR("Asr", Prayer.ASR),
    MAGHRIB("Maghrib", Prayer.MAGHRIB),
    ISHA("Isha", Prayer.ISHA);

    companion object {
        val prayers: List<PrayerSlot> = entries.filter { it.prayer != null }

        fun of(prayer: Prayer): PrayerSlot? = entries.firstOrNull { it.prayer == prayer }
    }
}

/** One step of a routine: a dhikr and how many times to recite it in this routine. */
@Serializable
data class RoutineEntry(val dhikrId: String, val count: Int)

typealias Playlist = Map<PrayerSlot, List<RoutineEntry>>

/** Resolve the routine a prayer actually uses: its own list if present, otherwise the shared one. */
fun Playlist.routineFor(slot: PrayerSlot): List<RoutineEntry> =
    this[slot] ?: this[PrayerSlot.ALL] ?: emptyList()

fun Playlist.hasOwnList(slot: PrayerSlot): Boolean = slot == PrayerSlot.ALL || containsKey(slot)

/** Everything recited on one calendar day. */
@Serializable
data class DailyRecord(
    /** ISO-8601 date, e.g. 2026-10-16. */
    val date: String,
    /** Total taps per dhikr id, across every session and free recitation. */
    val dhikrCounts: Map<String, Int> = emptyMap(),
    /** Taps per dhikr id inside each prayer's post-prayer session. */
    val sessionCounts: Map<PrayerSlot, Map<String, Int>> = emptyMap(),
    val completedSessions: Set<PrayerSlot> = emptySet(),
) {
    val totalRecitations: Int get() = dhikrCounts.values.sum()

    fun recordTap(dhikrId: String, slot: PrayerSlot?, amount: Int = 1): DailyRecord {
        val counts = dhikrCounts + (dhikrId to (dhikrCounts[dhikrId] ?: 0) + amount)
        val sessions = if (slot == null) sessionCounts else {
            val session = sessionCounts[slot] ?: emptyMap()
            sessionCounts + (slot to session + (dhikrId to (session[dhikrId] ?: 0) + amount))
        }
        return copy(dhikrCounts = counts, sessionCounts = sessions)
    }

    fun markSessionComplete(slot: PrayerSlot): DailyRecord = copy(completedSessions = completedSessions + slot)
}

/** Progress of one prayer's routine today: completed targets vs. total targets. */
data class SessionProgress(
    val slot: PrayerSlot,
    val entries: List<EntryProgress>,
    val isComplete: Boolean,
) {
    val target: Int get() = entries.sumOf { it.target }
    val done: Int get() = entries.sumOf { it.done }
    val fraction: Float get() = if (target == 0) 0f else (done.toFloat() / target).coerceIn(0f, 1f)
}

data class EntryProgress(val dhikr: Dhikr, val target: Int, val done: Int) {
    val fraction: Float get() = if (target == 0) 0f else (done.toFloat() / target).coerceIn(0f, 1f)
    val isComplete: Boolean get() = done >= target
}

fun DailyRecord.progressFor(slot: PrayerSlot, routine: List<RoutineEntry>, library: List<Dhikr>): SessionProgress {
    val session = sessionCounts[slot] ?: emptyMap()
    val entries = routine.mapNotNull { entry ->
        val dhikr = library.firstOrNull { it.id == entry.dhikrId } ?: return@mapNotNull null
        EntryProgress(dhikr, entry.count, (session[entry.dhikrId] ?: 0).coerceAtMost(entry.count))
    }
    val complete = slot in completedSessions || (entries.isNotEmpty() && entries.all { it.isComplete })
    return SessionProgress(slot, entries, complete)
}

@Serializable
data class UserSettings(
    val calculation: CalculationSettings = CalculationSettings(),
    val notificationsEnabled: Boolean = false,
    /** [AdhanSounds] id played when a prayer comes in; persisted settings rely on these ids staying stable. */
    val adhanSound: String = AdhanSounds.DEFAULT_ID,
    /** Which prayers get the adhan, a tone, a beep or nothing — optionally different on weekends. */
    val reminders: ReminderSchedule = ReminderSchedule(),
    /** Recitations per day the tracker's headline bar is measured against. */
    val dailyGoal: Int = 300,
    val hapticsEnabled: Boolean = true,
)
