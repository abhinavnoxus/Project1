package com.nexora.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.app.NexoraApplication
import com.nexora.app.alarm.AlarmScheduler
import com.nexora.app.data.NexoraDatabase
import com.nexora.app.data.repo.AlarmRepository
import com.nexora.app.data.repo.FocusRepository
import com.nexora.app.data.repo.RoutineRepository
import com.nexora.app.data.repo.TaskRepository
import com.nexora.app.data.repo.toDomain
import com.nexora.app.design.GlassQuality
import com.nexora.app.design.InterfaceProfile
import com.nexora.app.design.InterfaceRepository
import com.nexora.app.design.ProfileSet
import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.FocusSession
import com.nexora.app.domain.model.Routine
import com.nexora.app.domain.model.ScheduleEvent
import com.nexora.app.domain.model.Task
import com.nexora.app.platform.DeviceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Single app-scoped state holder.
 *
 * Kept intentionally as one ViewModel rather than one per screen: NEXORA's screens share
 * a great deal of state (alarms feed the home, the alarm screen and the health centre),
 * and a single owner avoids duplicate observation of the database. Screens read narrow
 * derived flows so recomposition still stays local.
 */
class NexoraViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context get() = getApplication<Application>()

    val device: DeviceProfile = (app as? NexoraApplication)?.deviceProfile ?: DeviceProfile.detect(app)

    private val db = NexoraDatabase.get(app)
    private val scheduler = AlarmScheduler(app)

    private val alarmRepo = AlarmRepository(db, scheduler)
    private val taskRepo = TaskRepository(db)
    private val routineRepo = RoutineRepository(db)
    private val focusRepo = FocusRepository(db)
    private val interfaceRepo = InterfaceRepository(app)

    // ---- interface --------------------------------------------------------------

    val profiles: StateFlow<ProfileSet> = interfaceRepo.profileSet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileSet())

    private val _qualityOverride = MutableStateFlow<GlassQuality?>(null)
    val effectiveQuality: StateFlow<GlassQuality> = combine(profiles, _qualityOverride) { set, override ->
        override ?: set.qualityOverride ?: device.recommendedQuality()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, device.recommendedQuality())

    private val _reducedMotion = MutableStateFlow(false)
    val reducedMotion: StateFlow<Boolean> = _reducedMotion.asStateFlow()

    fun setReducedMotion(value: Boolean) {
        _reducedMotion.value = value
    }

    fun updateSpec(transform: (com.nexora.app.design.GlassSpecData) -> com.nexora.app.design.GlassSpecData) {
        viewModelScope.launch {
            val set = profiles.value
            val active = set.active ?: return@launch
            interfaceRepo.saveProfile(active.copy(spec = transform(active.spec)))
        }
    }

    fun updateLayout(transform: (com.nexora.app.design.LayoutConfig) -> com.nexora.app.design.LayoutConfig) {
        viewModelScope.launch {
            val active = profiles.value.active ?: return@launch
            interfaceRepo.saveProfile(active.copy(layout = transform(active.layout)))
        }
    }

    fun activateProfile(profileId: String) = viewModelScope.launch { interfaceRepo.activate(profileId) }

    fun duplicateProfile(profile: InterfaceProfile, newName: String) =
        viewModelScope.launch { interfaceRepo.duplicate(profile.id, newName) }

    fun deleteProfile(profileId: String) = viewModelScope.launch { interfaceRepo.delete(profileId) }

    fun resetInterface() = viewModelScope.launch { interfaceRepo.resetToDefault() }

    fun setQualityOverride(quality: GlassQuality?) {
        _qualityOverride.value = quality
        viewModelScope.launch { interfaceRepo.setQualityOverride(quality) }
    }

    suspend fun exportActiveProfile(): String = interfaceRepo.exportActive()

    suspend fun importProfiles(raw: String): Int = interfaceRepo.importProfiles(raw)

    fun setOnboarded() = viewModelScope.launch { interfaceRepo.setOnboarded(true) }

    // ---- alarms -----------------------------------------------------------------

    val alarms: StateFlow<List<Alarm>> = alarmRepo.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nextAlarm: StateFlow<Alarm?> = alarms
        .combine(kotlinx.coroutines.flow.flow { while (true) { emit(System.currentTimeMillis()); kotlinx.coroutines.delay(30_000) } }) { list, now ->
            list.filter { it.enabled }
                .mapNotNull { a -> com.nexora.app.domain.alarm.AlarmMath.nextTrigger(a, now)?.let { a to it } }
                .minByOrNull { it.second }?.first
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveAlarm(alarm: Alarm) = viewModelScope.launch(Dispatchers.IO) { alarmRepo.save(alarm) }

    fun deleteAlarm(id: Long) = viewModelScope.launch(Dispatchers.IO) { alarmRepo.delete(id) }

    fun toggleAlarm(id: Long, enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) { alarmRepo.setEnabled(id, enabled) }

    // ---- tasks ------------------------------------------------------------------

    val tasks: StateFlow<List<Task>> = taskRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val priorities: StateFlow<List<Task>> = tasks
        .let { flow ->
            kotlinx.coroutines.flow.map(tasks) { list ->
                list.filter { it.kind == com.nexora.app.domain.model.TaskKind.Priority && !it.done }
                    .sortedByDescending { it.priority }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveTask(task: Task) = viewModelScope.launch(Dispatchers.IO) { taskRepo.save(task) }

    fun setTaskDone(id: Long, done: Boolean) =
        viewModelScope.launch(Dispatchers.IO) { taskRepo.setDone(id, done) }

    fun deleteTask(id: Long) = viewModelScope.launch(Dispatchers.IO) { taskRepo.delete(id) }

    // ---- routines ---------------------------------------------------------------

    val routines: StateFlow<List<Routine>> = routineRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    val routineLogs: StateFlow<Set<Long>> = combine(_today, routines) { day, _ -> day }
        .let { flow ->
            kotlinx.coroutines.flow.flatMapLatestCompat(flow) { day -> routineRepo.observeLogs(day) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun completeRoutine(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        routineRepo.markComplete(id, _today.value)
    }

    fun saveRoutine(routine: Routine) = viewModelScope.launch(Dispatchers.IO) { routineRepo.save(routine) }

    fun deleteRoutine(id: Long) = viewModelScope.launch(Dispatchers.IO) { routineRepo.delete(id) }

    // ---- focus ------------------------------------------------------------------

    val focusSessions: StateFlow<List<FocusSession>> = focusRepo.observeRecent(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _activeFocusId = MutableStateFlow<Long?>(null)
    val activeFocusId: StateFlow<Long?> = _activeFocusId.asStateFlow()

    fun startFocus(minutes: Int, label: String) = viewModelScope.launch(Dispatchers.IO) {
        val id = focusRepo.start(minutes, label)
        _activeFocusId.value = id
    }

    fun endFocus(completed: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        _activeFocusId.value?.let { focusRepo.finish(it, completed, !completed) }
        _activeFocusId.value = null
    }

    // ---- calendar (optional) ----------------------------------------------------

    private val _calendarEvents = MutableStateFlow<List<ScheduleEvent>>(emptyList())
    val calendarEvents: StateFlow<List<ScheduleEvent>> = _calendarEvents.asStateFlow()

    private val _calendarPermission = MutableStateFlow(false)
    val calendarPermission: StateFlow<Boolean> = _calendarPermission.asStateFlow()

    fun setCalendarPermission(granted: Boolean) {
        _calendarPermission.value = granted
        if (granted) refreshCalendar()
    }

    fun refreshCalendar() = viewModelScope.launch(Dispatchers.IO) {
        if (!_calendarPermission.value) return@launch
        val events = com.nexora.app.platform.CalendarBridge.todayEvents(context)
        _calendarEvents.value = events
    }

    // ---- lifecycle --------------------------------------------------------------

    fun onResume() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { alarmRepo.rescheduleAll() }
        }
        _today.value = LocalDate.now()
        refreshCalendar()
    }

    fun reconcileAlarms() = viewModelScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.IO) { com.nexora.app.alarm.AlarmController.reconcileOnStartup(context) }
    }

    fun restoreAlarms() = viewModelScope.launch(Dispatchers.IO) {
        com.nexora.app.alarm.AlarmController.restoreAll(context)
    }

    /** Alarm Health snapshot, recomputed on demand. */
    fun alarmHealth(): AlarmHealth = AlarmHealth(
        notificationsEnabled = com.nexora.app.alarm.NotificationHelper.canPost(context),
        channelEnabled = com.nexora.app.alarm.NotificationHelper.areChannelsEnabled(context),
        exactAlarmsAllowed = scheduler.canScheduleExact(),
        nextAlarmMillis = scheduler.nextAlarmMillis(),
        enabledCount = alarms.value.count { it.enabled },
        totalCount = alarms.value.size,
        ringingCount = db.alarmInstanceDao().let { 0 },
        batteryOptimised = com.nexora.app.platform.PowerGuidance.isIgnoringBatteryOptimisations(context),
        ringtoneIssue = alarms.value.firstOrNull { !scheduler.ringtoneAvailable(it.ringtoneUri) }?.label,
        realmeUiDetected = device.isRealmeUi,
        lastDismissLatencyMillis = com.nexora.app.alarm.AlarmHistory.lastDismissLatencyMillis,
    )
}

data class AlarmHealth(
    val notificationsEnabled: Boolean,
    val channelEnabled: Boolean,
    val exactAlarmsAllowed: Boolean,
    val nextAlarmMillis: Long?,
    val enabledCount: Int,
    val totalCount: Int,
    val ringingCount: Int,
    val batteryOptimised: Boolean,
    val ringtoneIssue: String?,
    val realmeUiDetected: Boolean,
    val lastDismissLatencyMillis: Long,
) {
    val allGood: Boolean
        get() = notificationsEnabled && channelEnabled && exactAlarmsAllowed && ringtoneIssue == null
}

/** `flatMapLatest` without importing ExperimentalCoroutinesApi everywhere. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private fun <T, R> kotlinx.coroutines.flow.flatMapLatestCompat(
    source: kotlinx.coroutines.flow.Flow<T>,
    transform: (T) -> kotlinx.coroutines.flow.Flow<R>,
): kotlinx.coroutines.flow.Flow<R> = kotlinx.coroutines.flow.flatMapLatest(source, transform)
