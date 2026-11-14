package com.nexora.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.app.NexoraApplication
import com.nexora.app.alarm.AlarmScheduler
import com.nexora.app.data.NexoraDatabase
import com.nexora.app.notify.NotificationHelper
import com.nexora.app.data.repo.AlarmRepository
import com.nexora.app.data.repo.FocusRepository
import com.nexora.app.data.repo.RoutineRepository
import com.nexora.app.data.repo.TaskRepository
import com.nexora.app.design.GlassQuality
import com.nexora.app.design.GlassSpecData
import com.nexora.app.design.InterfaceProfile
import com.nexora.app.design.InterfaceRepository
import com.nexora.app.design.LayoutConfig
import com.nexora.app.design.ProfileSet
import com.nexora.app.domain.alarm.AlarmMath
import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.FocusSession
import com.nexora.app.domain.model.Routine
import com.nexora.app.domain.model.ScheduleEvent
import com.nexora.app.domain.model.Task
import com.nexora.app.domain.model.TaskKind
import com.nexora.app.platform.CalendarBridge
import com.nexora.app.platform.DeviceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Single app-scoped state holder.
 *
 * One ViewModel rather than one per screen: NEXORA's screens share a great deal of state
 * (an alarm feeds the home, the alarm list and the health centre), and a single owner
 * avoids observing the database several times. Screens read narrow derived flows, so
 * recomposition still stays local.
 */
@OptIn(ExperimentalCoroutinesApi::class)
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

    // ---------------------------------------------------------------- interface

    val profiles: StateFlow<ProfileSet> = interfaceRepo.profileSet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileSet())

    private val qualityOverride = MutableStateFlow<GlassQuality?>(null)

    val effectiveQuality: StateFlow<GlassQuality> =
        combine(profiles, qualityOverride) { set, override ->
            override ?: set.qualityOverride ?: device.recommendedQuality()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, device.recommendedQuality())

    private val reducedMotion = MutableStateFlow(false)
    val reducedMotionFlow: StateFlow<Boolean> = reducedMotion.asStateFlow()

    fun setReducedMotion(value: Boolean) { reducedMotion.value = value }

    fun updateSpec(transform: (GlassSpecData) -> GlassSpecData) {
        viewModelScope.launch {
            val active = profiles.value.active ?: return@launch
            interfaceRepo.saveProfile(active.copy(spec = transform(active.spec)))
        }
    }

    fun updateLayout(transform: (LayoutConfig) -> LayoutConfig) {
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
        qualityOverride.value = quality
        viewModelScope.launch { interfaceRepo.setQualityOverride(quality) }
    }

    suspend fun exportActiveProfile(): String = interfaceRepo.exportActive()

    suspend fun importProfiles(raw: String): Int = interfaceRepo.importProfiles(raw)

    fun setOnboarded() = viewModelScope.launch { interfaceRepo.setOnboarded(true) }

    suspend fun isOnboarded(): Boolean = interfaceRepo.current().onboarded

    // ------------------------------------------------------------------ alarms

    val alarms: StateFlow<List<Alarm>> = alarmRepo.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Ticks once a minute; used for countdowns and the live clock. */
    private val minuteTicker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    val now: StateFlow<Long> = minuteTicker
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), System.currentTimeMillis())

    @OptIn(ExperimentalCoroutinesApi::class)
    val nextAlarm: StateFlow<Alarm?> = combine(alarms, now) { list, current ->
        list.filter { it.enabled }
            .mapNotNull { alarm -> AlarmMath.nextTrigger(alarm, current)?.let { alarm to it } }
            .minByOrNull { it.second }
            ?.first
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveAlarm(alarm: Alarm) = viewModelScope.launch(Dispatchers.IO) { alarmRepo.save(alarm) }

    fun deleteAlarm(id: Long) = viewModelScope.launch(Dispatchers.IO) { alarmRepo.delete(id) }

    fun toggleAlarm(id: Long, enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) { alarmRepo.setEnabled(id, enabled) }

    // ------------------------------------------------------------------- tasks

    val tasks: StateFlow<List<Task>> = taskRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val priorities: StateFlow<List<Task>> = tasks
        .map { list -> list.filter { it.kind == TaskKind.Priority && !it.done }.sortedByDescending { it.priority } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveTask(task: Task) = viewModelScope.launch(Dispatchers.IO) { taskRepo.save(task) }

    fun setTaskDone(id: Long, done: Boolean) =
        viewModelScope.launch(Dispatchers.IO) { taskRepo.setDone(id, done) }

    fun deleteTask(id: Long) = viewModelScope.launch(Dispatchers.IO) { taskRepo.delete(id) }

    // ---------------------------------------------------------------- routines

    val routines: StateFlow<List<Routine>> = routineRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val todayState = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = todayState.asStateFlow()

    val routineLogs: StateFlow<Set<Long>> = todayState
        .flatMapLatest { day -> routineRepo.observeLogs(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun completeRoutine(id: Long) =
        viewModelScope.launch(Dispatchers.IO) { routineRepo.markComplete(id, todayState.value) }

    fun saveRoutine(routine: Routine) = viewModelScope.launch(Dispatchers.IO) { routineRepo.save(routine) }

    fun deleteRoutine(id: Long) = viewModelScope.launch(Dispatchers.IO) { routineRepo.delete(id) }

    /** Consecutive completed days per routine, computed from stored logs. */
    suspend fun routineStreaks(): Map<Long, Int> = withContext(Dispatchers.IO) {
        val day = todayState.value
        routineRepo.streaks(day)
    }

    // ------------------------------------------------------------------- focus

    val focusSessions: StateFlow<List<FocusSession>> = focusRepo.observeRecent(40)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val activeFocusId = MutableStateFlow<Long?>(null)
    val activeFocus: StateFlow<Long?> = activeFocusId.asStateFlow()

    private val focusStartedAt = MutableStateFlow(0L)
    val focusStart: StateFlow<Long> = focusStartedAt.asStateFlow()

    fun startFocus(minutes: Int, label: String) = viewModelScope.launch(Dispatchers.IO) {
        val id = focusRepo.start(minutes, label)
        focusStartedAt.value = System.currentTimeMillis()
        focusPlanned.value = minutes
        activeFocusId.value = id
    }

    private val focusPlanned = MutableStateFlow(25)
    val focusPlannedMinutes: StateFlow<Int> = focusPlanned.asStateFlow()

    /** Extends a running session by [minutes] and persists the new plan. */
    fun extendFocus(minutes: Int) = viewModelScope.launch(Dispatchers.IO) {
        val id = activeFocusId.value ?: return@launch
        val next = focusPlanned.value + minutes
        focusPlanned.value = next
        db.focusDao().setPlanned(id, next)
    }

    fun endFocus(completed: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        activeFocusId.value?.let { focusRepo.finish(it, completed, !completed) }
        activeFocusId.value = null
        focusStartedAt.value = 0L
        focusPlanned.value = 25
    }

    // ------------------------------------------------------------------ timers

    private val timerState = MutableStateFlow<List<com.nexora.app.ui.clock.TimerState>>(emptyList())
    val timers: StateFlow<List<com.nexora.app.ui.clock.TimerState>> = timerState.asStateFlow()

    private val stopwatchState = MutableStateFlow(StopwatchState())
    val stopwatch: StateFlow<StopwatchState> = stopwatchState.asStateFlow()

    private val timerJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    init {
        // Restore timers stored as absolute instants so a process restart keeps them.
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val raw = interfaceRepo.loadTimers() ?: return@runCatching
                timerState.value = timerJson.decodeFromString<
                    List<com.nexora.app.ui.clock.TimerState>>(raw)
            }
        }
    }

    private fun persistTimers() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                interfaceRepo.saveTimers(timerJson.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(
                        com.nexora.app.ui.clock.TimerState.serializer()
                    ),
                    timerState.value,
                ))
            }
        }
    }

    fun addTimer(minutes: Int, label: String = "$minutes minute timer") {
        timerState.value = timerState.value + com.nexora.app.ui.clock.TimerState(
            id = System.nanoTime(),
            label = label,
            durationMillis = minutes * 60_000L,
            startedAt = System.currentTimeMillis(),
        )
        persistTimers()
    }

    fun pauseTimer(id: Long) {
        val now = System.currentTimeMillis()
        timerState.value = timerState.value.map { timer ->
            if (timer.id != id || timer.pausedAt != null) timer
            else timer.copy(pausedAt = now, elapsedBeforePause = timer.elapsed(now))
        }
    }

    fun resumeTimer(id: Long) {
        val now = System.currentTimeMillis()
        timerState.value = timerState.value.map { timer ->
            if (timer.id != id || timer.pausedAt == null) timer
            else timer.copy(pausedAt = null, startedAt = now)
        }
    }

    fun cancelTimer(id: Long) {
        timerState.value = timerState.value.filterNot { it.id == id }
        persistTimers()
    }

    /** Returns the timers that just finished, and removes them. Called by the UI ticker. */
    fun collectFinishedTimers(now: Long): List<com.nexora.app.ui.clock.TimerState> {
        val finished = timerState.value.filter { it.pausedAt == null && it.remaining(now) <= 0L }
        if (finished.isNotEmpty()) {
            timerState.value = timerState.value - finished.toSet()
            persistTimers()
        }
        return finished
    }

    fun stopwatchStart() {
        stopwatchState.value = stopwatchState.value.copy(runningSince = System.currentTimeMillis())
    }

    fun stopwatchStop() {
        val current = stopwatchState.value
        val started = current.runningSince ?: return
        stopwatchState.value = current.copy(
            accumulated = current.accumulated + (System.currentTimeMillis() - started),
            runningSince = null,
        )
    }

    fun stopwatchLap() {
        val current = stopwatchState.value
        val started = current.runningSince ?: return
        val total = current.accumulated + (System.currentTimeMillis() - started)
        stopwatchState.value = current.copy(laps = current.laps + total)
    }

    fun stopwatchReset() { stopwatchState.value = StopwatchState() }

    // ---------------------------------------------------------------- calendar

    private val calendarEventsState = MutableStateFlow<List<ScheduleEvent>>(emptyList())
    val calendarEvents: StateFlow<List<ScheduleEvent>> = calendarEventsState.asStateFlow()

    private val calendarPermissionState = MutableStateFlow(false)
    val calendarPermission: StateFlow<Boolean> = calendarPermissionState.asStateFlow()

    fun setCalendarPermission(granted: Boolean) {
        calendarPermissionState.value = granted
        if (granted) refreshCalendar()
    }

    fun refreshCalendar() = viewModelScope.launch(Dispatchers.IO) {
        if (!calendarPermissionState.value && !CalendarBridge.hasPermission(context)) return@launch
        calendarPermissionState.value = true
        calendarEventsState.value = CalendarBridge.todayEvents(context, todayState.value)
    }

    // --------------------------------------------------------------- lifecycle

    fun onResume() {
        viewModelScope.launch(Dispatchers.IO) { runCatching { alarmRepo.rescheduleAll() } }
        todayState.value = LocalDate.now()
        calendarPermissionState.value = CalendarBridge.hasPermission(context)
        refreshCalendar()
    }

    fun reconcileAlarms() = viewModelScope.launch(Dispatchers.IO) {
        com.nexora.app.alarm.AlarmController.reconcileOnStartup(context)
    }

    fun restoreAlarms() = viewModelScope.launch(Dispatchers.IO) {
        com.nexora.app.alarm.AlarmController.restoreAll(context)
    }

    fun alarmHealth(): AlarmHealth {
        val next = runCatching { kotlinx.coroutines.runBlocking { scheduler.nextAlarmMillis() } }.getOrNull()
        return alarmHealthWith(next)
    }

    private fun alarmHealthWith(next: Long?): AlarmHealth {
        val notifications = com.nexora.app.notify.NotificationHelper.canPost(context)
        val channel = com.nexora.app.notify.NotificationHelper.areChannelsEnabled(context)
        val exact = scheduler.canScheduleExact()
        val list = alarms.value
        val badRingtone = list.firstOrNull { !scheduler.ringtoneAvailable(it.ringtoneUri) }
        return AlarmHealth(
            notificationsEnabled = notifications,
            channelEnabled = channel,
            exactAlarmsAllowed = exact,
            nextAlarmMillis = next,
            enabledCount = list.count { it.enabled },
            totalCount = list.size,
            batteryOptimised = com.nexora.app.platform.PowerGuidance.isIgnoringBatteryOptimisations(context),
            ringtoneIssue = badRingtone?.label,
            realmeUiDetected = device.isRealmeUi,
            lastDismissLatencyMillis = com.nexora.app.alarm.AlarmHistory.lastDismissLatencyMillis,
            sdkInt = device.sdkInt,
        )
    }
}

data class StopwatchState(
    val accumulated: Long = 0L,
    val runningSince: Long? = null,
    val laps: List<Long> = emptyList(),
) {
    val isRunning: Boolean get() = runningSince != null
    fun elapsed(now: Long): Long = accumulated + (runningSince?.let { (now - it).coerceAtLeast(0L) } ?: 0L)
}

data class AlarmHealth(
    val notificationsEnabled: Boolean,
    val channelEnabled: Boolean,
    val exactAlarmsAllowed: Boolean,
    val nextAlarmMillis: Long?,
    val enabledCount: Int,
    val totalCount: Int,
    val batteryOptimised: Boolean,
    val ringtoneIssue: String?,
    val realmeUiDetected: Boolean,
    val lastDismissLatencyMillis: Long,
    val sdkInt: Int,
) {
    val allGood: Boolean
        get() = notificationsEnabled && channelEnabled && exactAlarmsAllowed && ringtoneIssue == null
}
