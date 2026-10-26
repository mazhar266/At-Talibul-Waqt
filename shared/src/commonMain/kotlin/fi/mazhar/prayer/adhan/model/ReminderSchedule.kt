package fi.mazhar.prayer.adhan.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.Serializable

/** How a single prayer is announced. */
@Serializable
enum class ReminderMode(val label: String, val description: String) {
    ADHAN("Adhan", "Full adhan with your chosen recording"),
    TONE("Tone", "Short synthesised tone"),
    BEEP("Beep", "Device notification sound only"),
    OFF("Off", "No reminder for this prayer");

    /** Sound id for the notification planner, or null when nothing should be scheduled. */
    fun soundId(defaultAdhan: String): String? = when (this) {
        ADHAN -> defaultAdhan
        TONE -> AdhanSounds.TONE_ID
        BEEP -> AdhanSounds.BEEP_ID
        OFF -> null
    }
}

/**
 * Per-prayer reminder modes, optionally different on weekend days — e.g. a quiet beep for Dhuhr
 * and Asr at the office on weekdays, the full adhan at home otherwise. Prayers missing from a map
 * default to [ReminderMode.ADHAN], so an empty schedule behaves like the original single sound.
 */
@Serializable
data class ReminderSchedule(
    val weekday: Map<Prayer, ReminderMode> = emptyMap(),
    /** null means the weekday modes apply every day. */
    val weekend: Map<Prayer, ReminderMode>? = null,
    /** ISO day numbers (1 = Monday … 7 = Sunday) counted as weekend when [weekend] is set. */
    val weekendDays: Set<Int> = DEFAULT_WEEKEND_DAYS,
) {
    val hasWeekendOverride: Boolean get() = weekend != null

    fun isWeekend(date: LocalDate): Boolean = weekend != null && date.dayOfWeek.isoDayNumber in weekendDays

    fun modeFor(prayer: Prayer, date: LocalDate): ReminderMode = modes(isWeekend(date))[prayer] ?: ReminderMode.ADHAN

    /** Modes for every obligatory prayer, filled with the default where nothing was chosen. */
    fun modes(weekend: Boolean): Map<Prayer, ReminderMode> {
        val source = if (weekend) this.weekend ?: weekday else weekday
        return Prayer.entries.filter { it.isObligatory }.associateWith { source[it] ?: ReminderMode.ADHAN }
    }

    fun withMode(prayer: Prayer, mode: ReminderMode, weekend: Boolean): ReminderSchedule =
        if (weekend) copy(weekend = (this.weekend ?: weekday) + (prayer to mode))
        else copy(weekday = weekday + (prayer to mode))

    /** Turning the override on starts the weekend from a copy of the weekday modes. */
    fun withWeekendOverride(enabled: Boolean): ReminderSchedule =
        copy(weekend = if (enabled) weekend ?: weekday else null)

    fun withWeekendDay(day: DayOfWeek, isWeekend: Boolean): ReminderSchedule =
        copy(weekendDays = if (isWeekend) weekendDays + day.isoDayNumber else weekendDays - day.isoDayNumber)

    companion object {
        val DEFAULT_WEEKEND_DAYS: Set<Int> = setOf(DayOfWeek.SATURDAY.isoDayNumber, DayOfWeek.SUNDAY.isoDayNumber)

        fun allAdhan(): ReminderSchedule = ReminderSchedule()

        fun allBeep(): ReminderSchedule =
            ReminderSchedule(weekday = Prayer.entries.filter { it.isObligatory }.associateWith { ReminderMode.BEEP })

        /** Quiet office hours: Dhuhr and Asr beep on weekdays, everything else is the full adhan. */
        fun officeWeekdays(weekendDays: Set<Int> = DEFAULT_WEEKEND_DAYS): ReminderSchedule = ReminderSchedule(
            weekday = mapOf(Prayer.DHUHR to ReminderMode.BEEP, Prayer.ASR to ReminderMode.BEEP),
            weekend = emptyMap(),
            weekendDays = weekendDays,
        )
    }
}
