package fi.mazhar.salat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.mazhar.salat.data.AppRepository
import fi.mazhar.salat.data.createKeyValueStore
import fi.mazhar.salat.model.AdhanSounds
import fi.mazhar.salat.model.CalculationSettings
import fi.mazhar.salat.model.DailyRecord
import fi.mazhar.salat.model.Dhikr
import fi.mazhar.salat.model.DhikrCategory
import fi.mazhar.salat.model.GeoLocation
import fi.mazhar.salat.model.PrayerSlot
import fi.mazhar.salat.model.RoutineEntry
import fi.mazhar.salat.model.routineFor
import fi.mazhar.salat.platform.AudioPlayer
import fi.mazhar.salat.platform.CompassService
import fi.mazhar.salat.platform.LocationResult
import fi.mazhar.salat.platform.LocationService
import fi.mazhar.salat.platform.NotificationPlanner
import fi.mazhar.salat.platform.NotificationService
import fi.mazhar.salat.platform.createAudioPlayer
import fi.mazhar.salat.platform.createCompassService
import fi.mazhar.salat.platform.createLocationService
import fi.mazhar.salat.platform.createNotificationService
import fi.mazhar.salat.prayer.DailySchedule
import fi.mazhar.salat.prayer.PrayerPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import talibulwaqt.shared.generated.resources.Res
import kotlin.time.Clock
import kotlin.time.Instant

/** Live tasbeeh session: a list of steps recited in order, one counter at a time. */
data class SessionState(
    val slot: PrayerSlot?,
    val title: String,
    val steps: List<RoutineEntry>,
    val index: Int = 0,
    val count: Int = 0,
    val finished: Boolean = false,
) {
    val current: RoutineEntry get() = steps[index]
    val target: Int get() = current.count
    val isStepComplete: Boolean get() = count >= target
    val isLastStep: Boolean get() = index == steps.lastIndex
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AppViewModel(
    val repository: AppRepository = AppRepository(createKeyValueStore()),
    private val locationService: LocationService = createLocationService(),
    private val compassService: CompassService = createCompassService(),
    private val notificationService: NotificationService = createNotificationService(),
    private val audioPlayer: AudioPlayer = createAudioPlayer(),
) : ViewModel() {

    val settings = repository.settings
    val location = repository.location
    val library = repository.library
    val playlist = repository.playlist
    val records = repository.records

    private val _now = MutableStateFlow(Clock.System.now())
    val now: StateFlow<Instant> = _now

    val timeZone: TimeZone get() = TimeZone.currentSystemDefault()

    private val dayKey = _now.map { it.toLocalDateTime(timeZone).date to timeZone.id }.distinctUntilChanged()

    val schedule: StateFlow<DailySchedule> = combine(location, settings, dayKey) { loc, prefs, (date, _) ->
        DailySchedule.forDate(date, loc, prefs.calculation, timeZone)
    }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        DailySchedule.forInstant(_now.value, location.value, settings.value.calculation, timeZone),
    )

    val period: StateFlow<PrayerPeriod> = combine(schedule, _now) { sched, now -> sched.periodAt(now) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, schedule.value.periodAt(_now.value))

    val todayRecord = combine(records, dayKey) { all, (date, _) ->
        all[date.toString()] ?: DailyRecord(date.toString())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.todayRecord())

    private val _session = MutableStateFlow<SessionState?>(null)
    val session: StateFlow<SessionState?> = _session

    private val _locationStatus = MutableStateFlow<String?>(null)
    val locationStatus: StateFlow<String?> = _locationStatus
    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating

    private val _notificationStatus = MutableStateFlow<String?>(null)
    val notificationStatus: StateFlow<String?> = _notificationStatus

    private val _previewingSound = MutableStateFlow<String?>(null)
    /** Id of the adhan sound currently playing from the picker or the test button, or null. */
    val previewingSound: StateFlow<String?> = _previewingSound
    private var cachedClip: Pair<String, ByteArray>? = null

    val locationSupported: Boolean get() = locationService.isSupported
    val compassSupported: Boolean get() = compassService.isSupported
    val notificationsSupported: Boolean get() = notificationService.isSupported
    val adhanTestSupported: Boolean get() = audioPlayer.isSupported

    /** True headings for the current location; empty where there is no sensor. */
    val headings: Flow<Float> = location.flatMapLatest { compassService.headings(it.latitude, it.longitude) }

    init {
        viewModelScope.launch {
            while (isActive) {
                _now.value = Clock.System.now()
                delay(1_000)
            }
        }
        // Any change that moves the prayer times must move the alarms too.
        viewModelScope.launch {
            combine(location, settings) { loc, prefs -> loc to prefs }
                .drop(1)
                .debounce(500)
                .collect { rescheduleNotifications() }
        }
        rescheduleNotifications()
    }

    // ---- location --------------------------------------------------------------------------

    fun setLocation(location: GeoLocation) {
        repository.setLocation(location)
        _locationStatus.value = null
    }

    fun useDeviceLocation() {
        if (_locating.value) return
        viewModelScope.launch {
            _locating.value = true
            _locationStatus.value = null
            when (val result = locationService.requestCurrentLocation()) {
                is LocationResult.Success -> {
                    repository.setLocation(
                        GeoLocation(
                            name = "My location",
                            country = "",
                            latitude = result.latitude,
                            longitude = result.longitude,
                        ),
                    )
                    _locationStatus.value = "Location updated from GPS."
                }
                LocationResult.PermissionDenied -> _locationStatus.value = "Location permission denied. Pick a city instead."
                LocationResult.Unavailable -> _locationStatus.value = "No location fix available. Pick a city instead."
                is LocationResult.Error -> _locationStatus.value = "Location failed: ${result.message}"
            }
            _locating.value = false
        }
    }

    // ---- settings --------------------------------------------------------------------------

    fun updateCalculation(transform: (CalculationSettings) -> CalculationSettings) =
        repository.updateSettings { it.copy(calculation = transform(it.calculation)) }

    fun setDailyGoal(goal: Int) = repository.updateSettings { it.copy(dailyGoal = goal.coerceIn(1, 100_000)) }

    fun setHaptics(enabled: Boolean) = repository.updateSettings { it.copy(hapticsEnabled = enabled) }

    fun setNotificationsEnabled(enabled: Boolean) {
        if (!enabled) {
            repository.updateSettings { it.copy(notificationsEnabled = false) }
            notificationService.cancelAll()
            _notificationStatus.value = null
            return
        }
        viewModelScope.launch {
            val granted = notificationService.requestPermission()
            if (granted) {
                repository.updateSettings { it.copy(notificationsEnabled = true) }
                rescheduleNotifications()
                _notificationStatus.value = "Adhan reminders scheduled for the next ${notificationService.planningHorizonDays} days."
            } else {
                repository.updateSettings { it.copy(notificationsEnabled = false) }
                _notificationStatus.value = if (notificationService.isSupported) {
                    "Notification permission was not granted."
                } else {
                    "Notifications are not available on this platform."
                }
            }
        }
    }

    fun rescheduleNotifications() {
        if (!notificationService.isSupported) return
        if (!settings.value.notificationsEnabled) return
        notificationService.schedule(NotificationPlanner.plan(repository, notificationService.planningHorizonDays))
    }

    // ---- adhan sound -----------------------------------------------------------------------

    fun setAdhanSound(id: String) = repository.updateSettings { it.copy(adhanSound = AdhanSounds.byId(id).id) }

    /** Plays the selected adhan sound once, or stops it if it is already playing. */
    fun toggleAdhanTest() = togglePreview(settings.value.adhanSound)

    /** Plays [soundId] from the start, or stops it if it is the one playing. "Beep" has nothing to play. */
    fun togglePreview(soundId: String) {
        if (_previewingSound.value == soundId) {
            stopPreview()
            return
        }
        val sound = AdhanSounds.byId(soundId)
        val path = sound.resourcePath ?: return
        if (!audioPlayer.isSupported) return
        audioPlayer.stop()
        _previewingSound.value = sound.id
        viewModelScope.launch {
            val bytes = try {
                cachedClip?.takeIf { it.first == sound.id }?.second
                    ?: Res.readBytes(path).also { cachedClip = sound.id to it }
            } catch (e: Exception) {
                if (_previewingSound.value == sound.id) _previewingSound.value = null
                return@launch
            }
            // Stopped, or switched to another sound, while the clip was being read.
            if (_previewingSound.value != sound.id) return@launch
            audioPlayer.play(bytes) {
                if (_previewingSound.value == sound.id) _previewingSound.value = null
            }
        }
    }

    fun stopPreview() {
        audioPlayer.stop()
        _previewingSound.value = null
    }

    override fun onCleared() {
        audioPlayer.release()
    }

    // ---- library & routines ----------------------------------------------------------------

    fun addCustomDhikr(
        title: String,
        arabic: String,
        transliteration: String,
        translation: String,
        targetCount: Int,
        category: DhikrCategory,
    ) {
        val dhikr = Dhikr(
            id = "custom-${Clock.System.now().toEpochMilliseconds()}",
            title = title.trim(),
            arabic = arabic.trim(),
            transliteration = transliteration.trim(),
            translation = translation.trim(),
            targetCount = targetCount.coerceIn(1, 9999),
            category = category,
            isCustom = true,
        )
        repository.addCustomDhikr(dhikr)
    }

    fun deleteCustomDhikr(id: String) = repository.deleteCustomDhikr(id)
    fun addToRoutine(slot: PrayerSlot, dhikrId: String) = repository.addToRoutine(slot, dhikrId)
    fun removeFromRoutine(slot: PrayerSlot, dhikrId: String) = repository.removeFromRoutine(slot, dhikrId)
    fun setRoutineCount(slot: PrayerSlot, dhikrId: String, count: Int) = repository.setRoutineCount(slot, dhikrId, count)
    fun moveInRoutine(slot: PrayerSlot, from: Int, to: Int) = repository.moveInRoutine(slot, from, to)
    fun resetRoutineToShared(slot: PrayerSlot) = repository.resetRoutineToShared(slot)
    fun resetAllRoutines() = repository.resetAllRoutines()
    fun resetToday() = repository.resetToday()
    fun streakDays(): Int = repository.streakDays()

    // ---- tasbeeh sessions ------------------------------------------------------------------

    fun startRoutine(slot: PrayerSlot) {
        val steps = playlist.value.routineFor(slot).filter { repository.dhikr(it.dhikrId) != null }
        if (steps.isEmpty()) return
        val title = if (slot == PrayerSlot.ALL) "Post-prayer adhkar" else "Post-${slot.label} adhkar"
        _session.value = SessionState(slot, title, steps)
    }

    fun startSingle(dhikr: Dhikr) {
        _session.value = SessionState(null, dhikr.title, listOf(RoutineEntry(dhikr.id, dhikr.targetCount)))
    }

    /** A tap counts one recitation; once the target is met the next tap moves on. */
    fun tap() {
        val state = _session.value ?: return
        if (state.finished) return
        if (state.isStepComplete) {
            nextStep()
            return
        }
        repository.recordTap(state.current.dhikrId, state.slot)
        _session.update { it?.copy(count = it.count + 1) }
    }

    fun nextStep() {
        val state = _session.value ?: return
        if (state.isLastStep) {
            state.slot?.let { repository.markSessionComplete(it) }
            _session.value = state.copy(finished = true)
        } else {
            _session.value = state.copy(index = state.index + 1, count = 0)
        }
    }

    fun previousStep() {
        _session.update { s -> if (s == null || s.index == 0) s else s.copy(index = s.index - 1, count = 0) }
    }

    fun resetStep() = _session.update { it?.copy(count = 0) }

    fun closeSession() {
        _session.value = null
    }
}
