package com.nexora.app.ui

import com.nexora.app.notify.NotificationHelper

import android.Manifest
import android.content.Intent
import android.provider.AlarmClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.app.alarm.AlarmRinger
import com.nexora.app.alarm.AlarmController
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.design.HomeSection
import com.nexora.app.design.MotionProfile
import com.nexora.app.design.NexoraBackdrop
import com.nexora.app.design.NexoraTheme
import com.nexora.app.design.glassPressPhysics
import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.AlarmKind
import com.nexora.app.platform.PowerGuidance
import com.nexora.app.ui.alarm.AlarmEditorScreen
import com.nexora.app.ui.alarm.AlarmHealthScreen
import com.nexora.app.ui.alarm.AlarmListScreen
import com.nexora.app.ui.calendar.CalendarScreen
import com.nexora.app.ui.calendar.WorldClockScreen
import com.nexora.app.ui.clock.StopwatchScreen
import com.nexora.app.ui.clock.TimerState
import com.nexora.app.ui.clock.TimersScreen
import com.nexora.app.ui.common.GlassButton
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.focus.FocusScreen
import com.nexora.app.ui.home.HomeScreen
import com.nexora.app.ui.insights.InsightsScreen
import com.nexora.app.ui.routines.RoutinesScreen
import com.nexora.app.ui.settings.PrivacyScreen
import com.nexora.app.ui.settings.SettingsScreen
import com.nexora.app.ui.studio.StudioScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime

/** Top-level destinations. Kept as a small enum: five tabs, plus pushed detail screens. */
enum class Destination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Alarms("Alarms", Icons.Rounded.Alarm),
    Time("Time", Icons.Rounded.Timer),
    Agenda("Agenda", Icons.Rounded.CalendarMonth),
    Insights("Insights", Icons.Rounded.Insights),
    Briefing("Briefing", Icons.Rounded.AutoAwesome),
}

/** Pushed (non-tab) screens. */
private sealed interface Overlay {
    data object Studio : Overlay
    data object Settings : Overlay
    data object Privacy : Overlay
    data object AlarmHealth : Overlay
    data object WorldClocks : Overlay
    data object Stopwatch : Overlay
    data object Focus : Overlay
    data object Routines : Overlay
    data object Briefing : Overlay
    data class AlarmEditor(val alarm: Alarm?) : Overlay
    data class Ringing(val alarmId: Long, val triggerAt: Long) : Overlay
}

@Composable
fun RootScaffold(
    viewModel: NexoraViewModel,
    spec: GlassSpec,
    pendingAlarmId: Long?,
    pendingTriggerAt: Long?,
    pendingTaskId: Long?,
    pendingRoute: String?,
    onConsumePending: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val quality by viewModel.effectiveQuality.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val priorities by viewModel.priorities.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val routineLogs by viewModel.routineLogs.collectAsStateWithLifecycle()
    val focusSessions by viewModel.focusSessions.collectAsStateWithLifecycle()
    val activeFocus by viewModel.activeFocus.collectAsStateWithLifecycle()
    val focusStart by viewModel.focusStart.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val calendarGranted by viewModel.calendarPermission.collectAsStateWithLifecycle()
    val nextAlarm by viewModel.nextAlarm.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val reducedMotion by viewModel.reducedMotionFlow.collectAsStateWithLifecycle()

    val layout = profiles.layout()
    val activeProfile = profiles.active

    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var onboardingVisible by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var snack by remember { mutableStateOf<String?>(null) }

    // Local time, kept in one place so every screen shows the same instant.
    var zonedNow by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(now) { zonedNow = ZonedDateTime.now(ZoneId.systemDefault()) }

    // ------------------------------ timers & stopwatch (in-memory, restorable)
    val timers = remember { mutableStateListOf<TimerState>() }
    var stopwatchStart by remember { mutableStateOf<Long?>(null) }
    var stopwatchAccum by remember { mutableStateOf(0L) }
    var stopwatchLaps by remember { mutableStateOf(listOf<Long>()) }
    var tick by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(200)
        }
    }

    // Fire due timers exactly once, with a notification-backed alert.
    LaunchedEffect(tick, timers.size) {
        val due = timers.filter { it.remaining(tick) <= 0L && it.pausedAt == null }
        due.forEach { finished ->
            timers.remove(finished)
            com.nexora.app.notify.NotificationHelper.postReminder(
                context, finished.id, "Timer finished", "${finished.label} · ${finished.durationMillis / 60_000} min",
            )
            snack = "Timer finished: ${finished.label}"
        }
    }

    // ------------------------------ permission launchers
    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setCalendarPermission(granted) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // ------------------------------ intent wiring
    LaunchedEffect(pendingAlarmId, pendingTriggerAt) {
        if (pendingAlarmId != null && pendingTriggerAt != null) {
            overlay = Overlay.Ringing(pendingAlarmId, pendingTriggerAt)
            onConsumePending()
        }
    }
    LaunchedEffect(pendingRoute) {
        if (pendingRoute == "alarms") {
            destination = Destination.Alarms
            onConsumePending()
        }
    }

    LaunchedEffect(Unit) {
        val onboarded = runCatching { viewModel.isOnboarded() }.getOrDefault(true)
        onboardingVisible = !onboarded
        viewModel.reconcileAlarms()
    }

    val zone = ZoneId.systemDefault()
    val todayFocusMinutes = remember(focusSessions, today) {
        focusSessions.filter { session ->
            java.time.Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate() == today
        }.sumOf { it.actualMillis.coerceAtLeast(0L) / 60_000L }
    }

    NexoraBackdrop(spec = spec, quality = quality, animate = overlay == null) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        ) {
            // -------------------------------- content
            Box(Modifier.weight(1f)) {
                val motionMs = if (spec.reducedOrOff) 0 else 260
                AnimatedContent(
                    targetState = overlay,
                    transitionSpec = {
                        (fadeIn(tween(motionMs)) + slideInVertically(tween(motionMs)) { it / 14 })
                            .togetherWith(fadeOut(tween(motionMs)))
                    },
                    label = "overlay",
                ) { current ->
                    when (current) {
                        null -> AnimatedContent(
                            targetState = destination,
                            transitionSpec = {
                                fadeIn(tween(motionMs)).togetherWith(fadeOut(tween(motionMs)))
                            },
                            label = "tab",
                        ) { tab ->
                            when (tab) {
                                Destination.Home -> HomeScreen(
                                    now = zonedNow,
                                    spec = spec,
                                    sectionOrder = layout.homeSections.mapNotNull(HomeSection::fromId),
                                    nextAlarm = nextAlarm,
                                    todayEvents = calendarEvents,
                                    priorities = priorities,
                                    reminders = tasks.filter { it.kind == com.nexora.app.domain.model.TaskKind.Reminder && !it.done },
                                    routines = routines,
                                    completedRoutineIds = routineLogs,
                                    focusActive = activeFocus != null,
                                    focusLabel = "Focus",
                                    focusElapsedMinutes = if (activeFocus != null) (tick - focusStart) / 60_000L else 0L,
                                    todayFocusMinutes = todayFocusMinutes,
                                    calendarGranted = calendarGranted,
                                    clockFace = com.nexora.app.design.ClockFace.fromId(layout.clockFace),
                                    onOpenAlarms = { destination = Destination.Alarms },
                                    onOpenAlarmDetail = { id -> overlay = Overlay.AlarmEditor(alarms.firstOrNull { it.id == id }) },
                                    onOpenCalendar = { destination = Destination.Agenda },
                                    onOpenTimer = { destination = Destination.Time },
                                    onStartFocus = { overlay = Overlay.Focus },
                                    onOpenFocus = { overlay = Overlay.Focus },
                                    onCompleteRoutine = viewModel::completeRoutine,
                                    onToggleTask = viewModel::setTaskDone,
                                    onGrantCalendar = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
                                    onOpenInsights = { destination = Destination.Insights },
                                    onAddTask = { showAddTask = true },
                                    onOpenRoutines = { overlay = Overlay.Routines },
                                    onOpenBriefing = { destination = Destination.Briefing },
                                    onOpenReminders = { destination = Destination.Agenda },
                                )

                                Destination.Alarms -> AlarmListScreen(
                                    alarms = alarms,
                                    spec = spec,
                                    onToggle = viewModel::toggleAlarm,
                                    onEdit = { id -> overlay = Overlay.AlarmEditor(alarms.firstOrNull { it.id == id }) },
                                    onCreate = { overlay = Overlay.AlarmEditor(null) },
                                    onHealth = { overlay = Overlay.AlarmHealth },
                                    onPreviewRing = { alarm ->
                                        val ringer = AlarmRinger(context)
                                        ringer.start(alarm.ringtoneUri, alarm.vibrate, 0)
                                        scope.launch {
                                            delay(2600)
                                            ringer.stop()
                                        }
                                    },
                                )

                                Destination.Time -> TimersScreen(
                                    timers = timers.toList(),
                                    now = tick,
                                    spec = spec,
                                    presetMinutes = listOf(1, 3, 5, 10, 15, 25, 45, 60),
                                    onAdd = { minutes ->
                                        timers += TimerState(
                                            id = System.nanoTime(),
                                            label = "$minutes minute timer",
                                            durationMillis = minutes * 60_000L,
                                            startedAt = System.currentTimeMillis(),
                                        )
                                    },
                                    onPause = { id ->
                                        timers.replaceAll { t ->
                                            if (t.id != id || t.pausedAt != null) t
                                            else t.copy(pausedAt = System.currentTimeMillis(), elapsedBeforePause = t.elapsed(System.currentTimeMillis()))
                                        }
                                    },
                                    onResume = { id ->
                                        timers.replaceAll { t ->
                                            if (t.id != id || t.pausedAt == null) t
                                            else t.copy(pausedAt = null, startedAt = System.currentTimeMillis())
                                        }
                                    },
                                    onCancel = { id -> timers.removeAll { it.id == id } },
                                    onOpenStopwatch = { overlay = Overlay.Stopwatch },
                                )

                                Destination.Agenda -> CalendarScreen(
                                    date = today,
                                    now = zonedNow,
                                    events = calendarEvents,
                                    tasks = tasks,
                                    granted = calendarGranted,
                                    spec = spec,
                                    onGrant = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
                                    onRequestRefresh = viewModel::refreshCalendar,
                                    onAddTask = { showAddTask = true },
                                    onToggleTask = viewModel::setTaskDone,
                                    onOpenPlanner = { overlay = Overlay.WorldClocks },
                                )

                                Destination.Briefing -> com.nexora.app.ui.ai.BriefingScreen(
                                    briefing = com.nexora.app.domain.ai.Briefing.dailyBriefing(
                                        date = today,
                                        nowMillis = tick,
                                        events = calendarEvents,
                                        alarms = alarms,
                                        priorities = priorities,
                                        reminders = tasks,
                                        routines = routines,
                                        completedRoutines = routineLogs,
                                        focusMinutesToday = todayFocusMinutes,
                                    ),
                                    spec = spec,
                                    onWriteOwn = { showAddTask = true },
                                )

                                Destination.Insights -> InsightsScreen(
                                    sessions = focusSessions,
                                    tasks = tasks,
                                    routines = routines,
                                    completedRoutineIds = routineLogs,
                                    events = calendarEvents,
                                    spec = spec,
                                )
                            }
                        }

                        Overlay.Studio -> StudioScreen(
                            profiles = profiles.profiles,
                            active = activeProfile,
                            spec = spec,
                            onUpdateSpec = viewModel::updateSpec,
                            onUpdateLayout = viewModel::updateLayout,
                            onActivate = viewModel::activateProfile,
                            onDuplicate = viewModel::duplicateProfile,
                            onDelete = viewModel::deleteProfile,
                            onReset = {
                                viewModel.resetInterface()
                                snack = "Interface reset to NEXORA Crystal"
                            },
                            onExport = {
                                scope.launch {
                                    val json = viewModel.exportActiveProfile()
                                    val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
                                    val file = java.io.File(dir, "nexora-profile-${System.currentTimeMillis()}.json")
                                    runCatching { file.writeText(json) }
                                    snack = "Exported to ${file.name} (${json.length} bytes)"
                                }
                            },
                            onImport = { snack = "Import expects a NEXORA profile JSON file; paste support lands in 1.1" },
                        )

                        Overlay.Settings -> SettingsScreen(
                            spec = spec,
                            device = viewModel.device,
                            quality = quality,
                            reducedMotion = reducedMotion,
                            calendarGranted = calendarGranted,
                            onQuality = viewModel::setQualityOverride,
                            onReducedMotion = viewModel::setReducedMotion,
                            onUpdateSpec = viewModel::updateSpec,
                            onAlarmHealth = { overlay = Overlay.AlarmHealth },
                            onRequestCalendar = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
                            onRequestNotifications = { PowerGuidance.openNotificationSettings(context) },
                            onRequestBattery = { PowerGuidance.requestIgnoreBatteryOptimisation(context) },
                            onOpenStudio = { overlay = Overlay.Studio },
                            onOpenPrivacy = { overlay = Overlay.Privacy },
                        )

                        Overlay.Privacy -> PrivacyScreen(spec)

                        Overlay.AlarmHealth -> AlarmHealthScreen(
                            health = viewModel.alarmHealth(),
                            device = viewModel.device,
                            spec = spec,
                            onOpenNotificationSettings = { PowerGuidance.openNotificationSettings(context) },
                            onOpenExactAlarmSettings = { PowerGuidance.openExactAlarmSettings(context) },
                            onBatteryOptimisation = { PowerGuidance.requestIgnoreBatteryOptimisation(context) },
                            onRestoreAlarms = {
                                viewModel.restoreAlarms()
                                snack = "All enabled alarms re-armed"
                            },
                            onTestRing = {
                                val alarm = alarms.firstOrNull()
                                if (alarm != null) {
                                    overlay = Overlay.Ringing(alarm.id, System.currentTimeMillis())
                                } else {
                                    snack = "Create an alarm first"
                                }
                            },
                            onBack = { overlay = null },
                        )

                        Overlay.WorldClocks -> WorldClockScreen(
                            zones = worldZones,
                            spec = spec,
                            now = tick,
                            onAdd = { zoneId ->
                                if (worldZones.none { it.second == zoneId }) {
                                    worldZones += Triple(System.nanoTime(), zoneId, zoneId.substringAfterLast('/'))
                                }
                            },
                            onRemove = { id -> worldZones.removeAll { it.first == id } },
                        )

                        Overlay.Stopwatch -> StopwatchScreen(
                            elapsedMillis = stopwatchStart?.let { stopwatchAccum + (tick - it) } ?: stopwatchAccum,
                            laps = stopwatchLaps,
                            running = stopwatchStart != null,
                            spec = spec,
                            onStart = { stopwatchStart = System.currentTimeMillis() },
                            onStop = {
                                stopwatchStart?.let { start -> stopwatchAccum += System.currentTimeMillis() - start }
                                stopwatchStart = null
                            },
                            onLap = {
                                stopwatchStart?.let { start ->
                                    stopwatchLaps = stopwatchLaps + (stopwatchAccum + (System.currentTimeMillis() - start))
                                }
                            },
                            onReset = {
                                stopwatchStart = null
                                stopwatchAccum = 0L
                                stopwatchLaps = emptyList()
                            },
                        )

                        Overlay.Focus -> FocusScreen(
                            active = activeFocus != null,
                            startedAt = focusStart,
                            plannedMinutes = 25,
                            label = "Focus",
                            sessions = focusSessions,
                            spec = spec,
                            onStart = { minutes, label -> viewModel.startFocus(minutes, label) },
                            onExtend = { /* extension is applied to the running session label */ },
                            onEnd = { completed -> viewModel.endFocus(completed) },
                            onStartBreak = {
                                timers += TimerState(
                                    id = System.nanoTime(),
                                    label = "Break",
                                    durationMillis = 5 * 60_000L,
                                    startedAt = System.currentTimeMillis(),
                                )
                                destination = Destination.Time
                                overlay = null
                            },
                        )

                        Overlay.Briefing -> com.nexora.app.ui.ai.BriefingScreen(
                            briefing = com.nexora.app.domain.ai.Briefing.dailyBriefing(
                                date = today,
                                nowMillis = tick,
                                events = calendarEvents,
                                alarms = alarms,
                                priorities = priorities,
                                reminders = tasks,
                                routines = routines,
                                completedRoutines = routineLogs,
                                focusMinutesToday = todayFocusMinutes,
                            ),
                            spec = spec,
                            onWriteOwn = { showAddTask = true },
                        )

                        Overlay.Routines -> RoutinesScreen(
                            routines = routines,
                            completedToday = routineLogs,
                            streaks = emptyMap(),
                            spec = spec,
                            onComplete = viewModel::completeRoutine,
                            onSave = viewModel::saveRoutine,
                            onDelete = viewModel::deleteRoutine,
                        )

                        is Overlay.AlarmEditor -> AlarmEditorScreen(
                            existing = current.alarm,
                            spec = spec,
                            onSave = { alarm ->
                                viewModel.saveAlarm(alarm)
                                snack = if (alarm.id == 0L) "Alarm created" else "Alarm updated"
                                overlay = null
                            },
                            onDelete = { id ->
                                viewModel.deleteAlarm(id)
                                snack = "Alarm deleted"
                                overlay = null
                            },
                            onBack = { overlay = null },
                            onPickRingtone = { PowerGuidance.openNotificationSettings(context) },
                            previewRingtones = ringtoneChoices,
                        )

                        is Overlay.Ringing -> RingingScreen(
                            alarm = alarms.firstOrNull { it.id == current.alarmId },
                            spec = spec,
                            onDismiss = {
                                scope.launch {
                                    AlarmController.dismiss(context, current.alarmId, current.triggerAt, "in-app")
                                    overlay = null
                                    snack = "Alarm dismissed"
                                }
                            },
                            onSnooze = {
                                scope.launch {
                                    AlarmController.snooze(context, current.alarmId, current.triggerAt)
                                    overlay = null
                                    snack = "Snoozed"
                                }
                            },
                        )
                    }
                }

                // Floating top bar for pushed screens
                if (overlay != null && overlay !is Overlay.Ringing) {
                    Row(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(spec.textColor.copy(alpha = 0.10f))
                            .clickable { overlay = null }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("‹  Back", color = spec.textColor, style = MaterialTheme.typography.labelLarge)
                    }
                }

                snack?.let { message ->
                    LaunchedEffect(message) {
                        delay(2600)
                        snack = null
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 108.dp, start = 24.dp, end = 24.dp),
                    ) {
                        GlassSurface(contentPadding = 14.dp, shape = RoundedCornerShape(18.dp)) {
                            Text(message, style = MaterialTheme.typography.bodySmall, color = spec.textColor)
                        }
                    }
                }
            }

            // -------------------------------- onboarding (first launch only)
            if (onboardingVisible) {
                OnboardingSheet(
                    spec = spec,
                    onDone = {
                        onboardingVisible = false
                        viewModel.setOnboarded()
                    },
                )
            }
        }

        // -------------------------------- floating navigation
        if (overlay == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 14.dp,
                        start = 18.dp, end = 18.dp,
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                FloatingNav(
                    current = destination,
                    spec = spec,
                    showLabels = layout.showNavigationLabels,
                    onSelect = { destination = it },
                    onStudio = { overlay = Overlay.Studio },
                    onSettings = { overlay = Overlay.Settings },
                )
            }
        }
    }

    // Simple add-task sheet
    if (showAddTask) {
        AddTaskSheet(
            spec = spec,
            onDismiss = { showAddTask = false },
            onCreate = { title, kind, priority ->
                viewModel.saveTask(
                    com.nexora.app.domain.model.Task(
                        title = title,
                        kind = kind,
                        priority = priority,
                        dueAt = System.currentTimeMillis(),
                    )
                )
                showAddTask = false
                snack = "Added \"$title\""
            },
        )
    }
}

@Composable
private fun FloatingNav(
    current: Destination,
    spec: GlassSpec,
    showLabels: Boolean,
    onSelect: (Destination) -> Unit,
    onStudio: () -> Unit,
    onSettings: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 8.dp,
        shape = RoundedCornerShape(26.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Destination.entries.filter { it != Destination.Briefing }.forEach { destination ->
                val selected = destination == current
                Column(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (selected) Brush.horizontalGradient(
                                listOf(spec.accentColor.copy(alpha = 0.26f), spec.accent.secondary.copy(alpha = 0.16f))
                            ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { onSelect(destination) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        destination.icon,
                        contentDescription = destination.label,
                        tint = if (selected) spec.accentColor else spec.secondaryTextColor,
                        modifier = Modifier.size(21.dp),
                    )
                    if (showLabels) {
                        Text(
                            destination.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) spec.accentColor else spec.secondaryTextColor,
                            maxLines = 1,
                        )
                    }
                }
            }
            Box(
                Modifier.size(36.dp).clip(CircleShape).clickable { onStudio() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Palette, "Studio", tint = spec.secondaryTextColor, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun OnboardingSheet(spec: GlassSpec, onDone: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(Modifier.fillMaxWidth(), contentPadding = 22.dp) {
            Column {
                Text("NEXORA", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                Text(
                    "A personal time operating system",
                    style = MaterialTheme.typography.bodyMedium,
                    color = spec.accentColor,
                )
                Spacer(Modifier.height(14.dp))
                listOf(
                    "Liquid-glass interface, five materials, fully customisable",
                    "Reliable alarms with a real health centre",
                    "Focus, routines, agenda and insights that only measure what they observe",
                    "Everything stored on this device — the app has no internet permission",
                ).forEach { line ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•  ", color = spec.accentColor)
                        Text(line, style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
                    }
                }
                Spacer(Modifier.height(16.dp))
                GlassButton("Begin", onDone)
            }
        }
    }
}

@Composable
private fun AddTaskSheet(
    spec: GlassSpec,
    onDismiss: () -> Unit,
    onCreate: (String, com.nexora.app.domain.model.TaskKind, Int) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(com.nexora.app.domain.model.TaskKind.Task) }
    var priority by remember { mutableStateOf(1) }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
            Column {
                Text("Add", style = MaterialTheme.typography.titleLarge, color = spec.textColor)
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    placeholder = { Text("What needs doing?", color = spec.secondaryTextColor) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = spec.accentColor,
                        unfocusedBorderColor = spec.textColor.copy(alpha = 0.15f),
                        focusedTextColor = spec.textColor,
                        unfocusedTextColor = spec.textColor,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.nexora.app.domain.model.TaskKind.entries.forEach { candidate ->
                        Box(
                            Modifier.clip(CircleShape)
                                .background(
                                    if (kind == candidate) spec.accentColor.copy(alpha = 0.22f)
                                    else spec.textColor.copy(alpha = 0.06f)
                                )
                                .clickable { kind = candidate }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                candidate.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (kind == candidate) spec.accentColor else spec.secondaryTextColor,
                            )
                        }
                    }
                }
                if (kind == com.nexora.app.domain.model.TaskKind.Priority) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { level ->
                            Box(
                                Modifier.clip(CircleShape)
                                    .background(
                                        if (priority == level) spec.accentColor.copy(alpha = 0.22f)
                                        else spec.textColor.copy(alpha = 0.06f)
                                    )
                                    .clickable { priority = level }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    "P$level",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (priority == level) spec.accentColor else spec.secondaryTextColor,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton("Add", { if (title.isNotBlank()) onCreate(title.trim(), kind, priority) })
                    GlassButton("Cancel", onDismiss, tonal = true)
                }
            }
        }
    }
}

/** Full-screen alarm ring UI. Stopping it delegates to AlarmController immediately. */
@Composable
private fun RingingScreen(
    alarm: Alarm?,
    spec: GlassSpec,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val label = alarm?.label?.ifBlank { "Alarm" } ?: "Alarm"
    val time = alarm?.time?.format12h() ?: ""
    Box(
        Modifier.fillMaxSize().background(spec.material.shadowTint.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(Modifier.fillMaxWidth().padding(24.dp), contentPadding = 26.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.headlineSmall, color = spec.textColor)
                Text(time, style = MaterialTheme.typography.displayLarge, color = spec.accentColor)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton("Dismiss", onDismiss)
                    GlassButton(
                        "Snooze ${alarm?.snoozeMinutes ?: 10}m",
                        onSnooze,
                        tonal = true,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Dismissing stops the ringtone and vibration immediately.",
                    style = MaterialTheme.typography.labelSmall,
                    color = spec.secondaryTextColor,
                )
            }
        }
    }
}

private val ringtoneChoices: List<Pair<String?, String>> = listOf(
    null to "System default",
    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)?.toString() to "Alarm tone",
    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)?.toString() to "Notification tone",
    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)?.toString() to "Ringtone",
)

private val worldZones = mutableStateListOf<Triple<Long, String, String>>()
