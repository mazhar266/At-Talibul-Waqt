package fi.mazhar.prayer.adhan.data

import fi.mazhar.prayer.adhan.model.DailyRecord
import fi.mazhar.prayer.adhan.model.DefaultDhikrLibrary
import fi.mazhar.prayer.adhan.model.Dhikr
import fi.mazhar.prayer.adhan.model.GeoLocation
import fi.mazhar.prayer.adhan.model.Playlist
import fi.mazhar.prayer.adhan.model.PrayerSlot
import fi.mazhar.prayer.adhan.model.PresetLocations
import fi.mazhar.prayer.adhan.model.RoutineEntry
import fi.mazhar.prayer.adhan.model.UserSettings
import fi.mazhar.prayer.adhan.model.routineFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Single source of truth for everything the user owns: location, calculation preferences,
 * the dhikr library (built-in + custom), per-prayer routines and daily recitation records.
 * Every mutation is persisted immediately as JSON.
 */
class AppRepository(
    private val store: KeyValueStore,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _settings = MutableStateFlow(load(KEY_SETTINGS, UserSettings.serializer()) ?: UserSettings())
    val settings: StateFlow<UserSettings> = _settings

    private val _location = MutableStateFlow(load(KEY_LOCATION, GeoLocation.serializer()) ?: PresetLocations.helsinki)
    val location: StateFlow<GeoLocation> = _location

    private val _library = MutableStateFlow(loadLibrary())
    val library: StateFlow<List<Dhikr>> = _library

    private val _playlist = MutableStateFlow(
        load(KEY_PLAYLIST, playlistSerializer) ?: DefaultDhikrLibrary.defaultPlaylist,
    )
    val playlist: StateFlow<Playlist> = _playlist

    private val _records = MutableStateFlow(load(KEY_RECORDS, recordsSerializer) ?: emptyMap())
    /** Keyed by ISO date; only the last [RECORD_HISTORY_DAYS] days are kept. */
    val records: StateFlow<Map<String, DailyRecord>> = _records

    // ---- settings & location -------------------------------------------------------------

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        _settings.update(transform)
        save(KEY_SETTINGS, UserSettings.serializer(), _settings.value)
    }

    fun setLocation(location: GeoLocation) {
        _location.value = location
        save(KEY_LOCATION, GeoLocation.serializer(), location)
    }

    // ---- library -------------------------------------------------------------------------

    fun addCustomDhikr(dhikr: Dhikr) {
        _library.update { it + dhikr.copy(isCustom = true) }
        saveLibrary()
    }

    fun updateDhikr(dhikr: Dhikr) {
        _library.update { list -> list.map { if (it.id == dhikr.id) dhikr else it } }
        saveLibrary()
    }

    fun deleteCustomDhikr(id: String) {
        _library.update { list -> list.filterNot { it.id == id && it.isCustom } }
        _playlist.update { playlist -> playlist.mapValues { (_, entries) -> entries.filterNot { it.dhikrId == id } } }
        saveLibrary()
        savePlaylist()
    }

    fun dhikr(id: String): Dhikr? = _library.value.firstOrNull { it.id == id }

    // ---- routines --------------------------------------------------------------------------

    fun addToRoutine(slot: PrayerSlot, dhikrId: String, count: Int? = null) {
        val dhikr = dhikr(dhikrId) ?: return
        updateRoutine(slot) { entries ->
            if (entries.any { it.dhikrId == dhikrId }) entries
            else entries + RoutineEntry(dhikrId, count ?: dhikr.targetCount)
        }
    }

    fun removeFromRoutine(slot: PrayerSlot, dhikrId: String) =
        updateRoutine(slot) { entries -> entries.filterNot { it.dhikrId == dhikrId } }

    fun setRoutineCount(slot: PrayerSlot, dhikrId: String, count: Int) =
        updateRoutine(slot) { entries ->
            entries.map { if (it.dhikrId == dhikrId) it.copy(count = count.coerceIn(1, 9999)) else it }
        }

    fun moveInRoutine(slot: PrayerSlot, fromIndex: Int, toIndex: Int) = updateRoutine(slot) { entries ->
        if (fromIndex !in entries.indices || toIndex !in entries.indices) entries
        else entries.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    /** Give [slot] its own list, seeded from whatever it currently inherits. */
    fun forkRoutine(slot: PrayerSlot) {
        if (slot == PrayerSlot.ALL || _playlist.value.containsKey(slot)) return
        _playlist.update { it + (slot to it.routineFor(slot)) }
        savePlaylist()
    }

    /** Drop [slot]'s own list so it falls back to the shared one again. */
    fun resetRoutineToShared(slot: PrayerSlot) {
        if (slot == PrayerSlot.ALL) return
        _playlist.update { it - slot }
        savePlaylist()
    }

    fun resetAllRoutines() {
        _playlist.value = DefaultDhikrLibrary.defaultPlaylist
        savePlaylist()
    }

    /** Edits always target the slot's own list; a prayer still on the shared list gets forked first. */
    private fun updateRoutine(slot: PrayerSlot, transform: (List<RoutineEntry>) -> List<RoutineEntry>) {
        _playlist.update { playlist ->
            val current = playlist.routineFor(slot)
            playlist + (slot to transform(current))
        }
        savePlaylist()
    }

    // ---- daily records -------------------------------------------------------------------

    fun todayKey(): String = clock.now().toLocalDateTime(timeZone()).date.toString()

    fun todayRecord(): DailyRecord = _records.value[todayKey()] ?: DailyRecord(todayKey())

    fun recordTap(dhikrId: String, slot: PrayerSlot?, amount: Int = 1) =
        updateToday { it.recordTap(dhikrId, slot, amount) }

    fun markSessionComplete(slot: PrayerSlot) = updateToday { it.markSessionComplete(slot) }

    fun resetToday() {
        _records.update { it - todayKey() }
        save(KEY_RECORDS, recordsSerializer, _records.value)
    }

    /** Consecutive days (ending today or yesterday) with at least one recitation. */
    fun streakDays(): Int {
        val today = clock.now().toLocalDateTime(timeZone()).date
        var day = today
        var streak = 0
        if ((_records.value[day.toString()]?.totalRecitations ?: 0) == 0) day = day.minus(1, DateTimeUnit.DAY)
        while ((_records.value[day.toString()]?.totalRecitations ?: 0) > 0) {
            streak++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    private fun updateToday(transform: (DailyRecord) -> DailyRecord) {
        val key = todayKey()
        _records.update { records ->
            val updated = records + (key to transform(records[key] ?: DailyRecord(key)))
            pruneHistory(updated, LocalDate.parse(key))
        }
        save(KEY_RECORDS, recordsSerializer, _records.value)
    }

    private fun pruneHistory(records: Map<String, DailyRecord>, today: LocalDate): Map<String, DailyRecord> {
        val oldest = today.minus(RECORD_HISTORY_DAYS, DateTimeUnit.DAY).toString()
        return records.filterKeys { it >= oldest }
    }

    // ---- persistence ---------------------------------------------------------------------

    private fun loadLibrary(): List<Dhikr> {
        val custom = load(KEY_CUSTOM_LIBRARY, ListSerializer(Dhikr.serializer())) ?: emptyList()
        val overrides = load(KEY_LIBRARY_OVERRIDES, ListSerializer(Dhikr.serializer())) ?: emptyList()
        val builtIn = DefaultDhikrLibrary.items.map { item -> overrides.firstOrNull { it.id == item.id } ?: item }
        return builtIn + custom
    }

    private fun saveLibrary() {
        val (custom, builtIn) = _library.value.partition { it.isCustom }
        save(KEY_CUSTOM_LIBRARY, ListSerializer(Dhikr.serializer()), custom)
        val overrides = builtIn.filter { edited -> DefaultDhikrLibrary.items.firstOrNull { it.id == edited.id } != edited }
        save(KEY_LIBRARY_OVERRIDES, ListSerializer(Dhikr.serializer()), overrides)
    }

    private fun savePlaylist() = save(KEY_PLAYLIST, playlistSerializer, _playlist.value)

    private fun <T> load(key: String, serializer: KSerializer<T>): T? =
        store.getString(key)?.let { raw -> runCatching { json.decodeFromString(serializer, raw) }.getOrNull() }

    private fun <T> save(key: String, serializer: KSerializer<T>, value: T) =
        store.putString(key, json.encodeToString(serializer, value))

    private companion object {
        const val KEY_SETTINGS = "adhan_settings_v1"
        const val KEY_LOCATION = "adhan_location_v1"
        const val KEY_CUSTOM_LIBRARY = "adhan_custom_dhikr_v1"
        const val KEY_LIBRARY_OVERRIDES = "adhan_dhikr_overrides_v1"
        const val KEY_PLAYLIST = "adhan_post_prayer_playlist_v1"
        const val KEY_RECORDS = "adhan_daily_records_v1"
        const val RECORD_HISTORY_DAYS = 90

        val playlistSerializer = MapSerializer(PrayerSlot.serializer(), ListSerializer(RoutineEntry.serializer()))
        val recordsSerializer = MapSerializer(String.serializer(), DailyRecord.serializer())
    }
}
