package com.nexora.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.app.design.AccentRamp
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.design.HomeSection
import com.nexora.app.design.NexoraSpacing
import com.nexora.app.design.NexoraTheme
import com.nexora.app.design.glassPressPhysics
import com.nexora.app.design.staggeredReveal
import com.nexora.app.design.rememberEntrance
import com.nexora.app.domain.alarm.AlarmMath
import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.Routine
import com.nexora.app.domain.model.ScheduleEvent
import com.nexora.app.domain.model.Task
import com.nexora.app.ui.clock.LiveClock
import com.nexora.app.ui.common.EmptyState
import com.nexora.app.ui.common.GlassButton
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.PillTag
import com.nexora.app.ui.common.SectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * NEXORA HOME — the living command centre.
 *
 * Section order comes from the user's active profile, so STUDIO's reordering is the same
 * data this screen reads. Every panel is backed by real state; where there is no data we
 * show an honest empty state with the action that would create data.
 */
@Composable
fun HomeScreen(
    now: ZonedDateTime,
    spec: GlassSpec,
    sectionOrder: List<HomeSection>,
    nextAlarm: Alarm?,
    todayEvents: List<ScheduleEvent>,
    priorities: List<Task>,
    reminders: List<Task>,
    routines: List<Routine>,
    completedRoutineIds: Set<Long>,
    focusActive: Boolean,
    focusLabel: String,
    focusElapsedMinutes: Long,
    todayFocusMinutes: Long,
    calendarGranted: Boolean,
    clockFace: com.nexora.app.design.ClockFace,
    onOpenAlarms: () -> Unit,
    onOpenAlarmDetail: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenTimer: () -> Unit,
    onStartFocus: () -> Unit,
    onOpenFocus: () -> Unit,
    onCompleteRoutine: (Long) -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onGrantCalendar: () -> Unit,
    onOpenInsights: () -> Unit,
    onAddTask: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenBriefing: () -> Unit = {},
) {
    val entrance = rememberEntrance(spec, 500)
    val progress = entrance.value

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HomeHeader(now, spec, Modifier.staggeredReveal(progress, 0, spec))
        }

        itemsIndexed(sectionOrder, key = { _, section -> section.id }) { index, section ->
            Box(Modifier.staggeredReveal(progress, index + 1, spec)) {
                when (section) {
                    HomeSection.TimeOrb -> TimeOrbPanel(
                        now = now,
                        spec = spec,
                        clockFace = clockFace,
                        nextAlarm = nextAlarm,
                        focusActive = focusActive,
                        onOpenAlarms = onOpenAlarms,
                    )
                    HomeSection.DateStrip -> DateStrip(now, spec)
                    HomeSection.NextAlarm -> NextAlarmPanel(nextAlarm, now, spec, onOpenAlarms, onOpenAlarmDetail)
                    HomeSection.Agenda -> AgendaPanel(todayEvents, calendarGranted, spec, onOpenCalendar, onGrantCalendar)
                    HomeSection.Priorities -> PrioritiesPanel(priorities, spec, onToggleTask, onAddTask)
                    HomeSection.Focus -> FocusPanel(focusActive, focusLabel, focusElapsedMinutes, todayFocusMinutes, spec, onStartFocus, onOpenFocus)
                    HomeSection.Routines -> RoutinesPanel(routines, completedRoutineIds, spec, onCompleteRoutine, onOpenRoutines)
                    HomeSection.Reminders -> RemindersPanel(reminders, spec, onToggleTask, onOpenReminders)
                    HomeSection.QuickActions -> QuickActions(spec, onOpenAlarms, onOpenTimer, onStartFocus, onOpenCalendar, onOpenInsights, onOpenBriefing)
                    HomeSection.InsightStrip -> InsightStrip(todayFocusMinutes, priorities, todayEvents, spec, onOpenInsights)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(now: ZonedDateTime, spec: GlassSpec, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                greeting(now),
                style = MaterialTheme.typography.headlineSmall,
                color = spec.textColor,
            )
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = MaterialTheme.typography.bodyMedium,
                color = spec.secondaryTextColor,
            )
        }
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(spec.accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "N",
                style = MaterialTheme.typography.titleMedium,
                color = spec.accentColor,
            )
        }
    }
}

private fun greeting(now: ZonedDateTime): String = when (now.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Late night"
}

@Composable
private fun TimeOrbPanel(
    now: ZonedDateTime,
    spec: GlassSpec,
    clockFace: com.nexora.app.design.ClockFace,
    nextAlarm: Alarm?,
    focusActive: Boolean,
    onOpenAlarms: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 18.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                LiveClock(face = clockFace, now = now, spec = spec, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillTag(
                    text = if (focusActive) "Focus running" else "Ready to focus",
                    highlighted = focusActive,
                )
                PillTag(
                    text = nextAlarm?.let { "Next ${it.time.format12h()}" } ?: "No alarm",
                    highlighted = nextAlarm != null,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = "Open alarms",
                    tint = spec.secondaryTextColor,
                    modifier = Modifier.size(20.dp).clickable { onOpenAlarms() },
                )
            }
        }
    }
}

@Composable
private fun DateStrip(now: ZonedDateTime, spec: GlassSpec) {
    val dayOfYear = now.dayOfYear
    val progress = dayOfYear / (if (now.toLocalDate().isLeapYear) 366f else 365f)
    GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    now.format(DateTimeFormatter.ofPattern("yyyy")),
                    style = MaterialTheme.typography.labelSmall,
                    color = spec.secondaryTextColor,
                )
                Text(
                    "Day $dayOfYear",
                    style = MaterialTheme.typography.titleMedium,
                    color = spec.textColor,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Week ${now.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = spec.secondaryTextColor,
                )
                Text(
                    "%.0f%% of year".format(progress * 100),
                    style = MaterialTheme.typography.bodySmall,
                    color = spec.accentColor,
                )
            }
        }
    }
}

@Composable
private fun NextAlarmPanel(
    nextAlarm: Alarm?,
    now: ZonedDateTime,
    spec: GlassSpec,
    onOpenAlarms: () -> Unit,
    onOpenAlarmDetail: (Long) -> Unit,
) {
    GlassPanel(
        title = "Next alarm",
        subtitle = nextAlarm?.label,
        modifier = Modifier.clickable {
            nextAlarm?.let { onOpenAlarmDetail(it.id) } ?: onOpenAlarms()
        },
    ) {
        if (nextAlarm == null) {
            EmptyState(
                title = "No alarm scheduled",
                body = "An alarm you enable will count down here, to the minute.",
                icon = Icons.Rounded.Alarm,
            )
        } else {
            val zone = ZoneId.systemDefault()
            val next = AlarmMath.nextTrigger(nextAlarm, System.currentTimeMillis(), zone)
            val remaining = ((next ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    nextAlarm.time.format12h(),
                    style = MaterialTheme.typography.displaySmall,
                    color = spec.textColor,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "in ${formatDuration(remaining)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = spec.accentColor,
                    )
                    Text(
                        nextAlarm.repeat.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaPanel(
    events: List<ScheduleEvent>,
    granted: Boolean,
    spec: GlassSpec,
    onOpenCalendar: () -> Unit,
    onGrantCalendar: () -> Unit,
) {
    GlassPanel(
        title = "Today's agenda",
        subtitle = if (granted) "${events.size} event${if (events.size == 1) "" else "s"}" else "Calendar not connected",
        modifier = Modifier.clickable { onOpenCalendar() },
    ) {
        when {
            !granted -> {
                Column {
                    Text(
                        "NEXORA can read today's events to build your agenda. It never writes to your calendar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                    )
                    Spacer(Modifier.height(10.dp))
                    GlassButton("Connect calendar", onGrantCalendar, tonal = true, icon = Icons.Rounded.CalendarMonth)
                }
            }
            events.isEmpty() -> EmptyState(
                title = "Nothing scheduled",
                body = "A clear day. Protect it with a focus session.",
                icon = Icons.Rounded.Event,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                events.take(4).forEach { event -> AgendaRow(event, spec) }
                if (events.size > 4) {
                    Text(
                        "+${events.size - 4} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.accentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaRow(event: ScheduleEvent, spec: GlassSpec) {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(event.startMillis).atZone(zone)
    val end = Instant.ofEpochMilli(event.endMillis).atZone(zone)
    val now = ZonedDateTime.now(zone)
    val live = now.isAfter(start) && now.isBefore(end)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .width(3.dp)
                .height(30.dp)
                .clip(CircleShape)
                .background(if (live) spec.accentColor else spec.accentColor.copy(alpha = 0.35f)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                event.title,
                style = MaterialTheme.typography.bodyLarge,
                color = spec.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (event.allDay) "All day" else "${start.format(DateTimeFormatter.ofPattern("HH:mm"))} – ${end.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                style = MaterialTheme.typography.bodySmall,
                color = spec.secondaryTextColor,
            )
        }
        if (live) PillTag("Now", highlighted = true)
    }
}

@Composable
private fun PrioritiesPanel(
    priorities: List<Task>,
    spec: GlassSpec,
    onToggle: (Long, Boolean) -> Unit,
    onAdd: () -> Unit,
) {
    GlassPanel(
        title = "Daily priorities",
        subtitle = if (priorities.isEmpty()) "Nothing set for today" else "${priorities.count { it.done }}/${priorities.size} complete",
        trailing = {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Add priority",
                tint = spec.accentColor,
                modifier = Modifier.size(22.dp).clickable { onAdd() },
            )
        },
    ) {
        if (priorities.isEmpty()) {
            EmptyState(
                title = "Choose up to three",
                body = "Priorities are the things today is judged by — not a backlog.",
                icon = Icons.Rounded.Bolt,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                priorities.take(5).forEach { task ->
                    TaskCheckRow(task, spec) { onToggle(task.id, !task.done) }
                }
            }
        }
    }
}

@Composable
private fun TaskCheckRow(task: Task, spec: GlassSpec, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (task.done) spec.accentColor else spec.textColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            if (task.done) Icon(Icons.Rounded.Check, "Done", tint = androidx.compose.ui.graphics.Color(0xFF04121C), modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (task.done) spec.secondaryTextColor else spec.textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (task.priority > 0) PillTag("P${task.priority}")
    }
}

@Composable
private fun FocusPanel(
    active: Boolean,
    label: String,
    elapsedMinutes: Long,
    todayMinutes: Long,
    spec: GlassSpec,
    onStart: () -> Unit,
    onOpen: () -> Unit,
) {
    GlassPanel(
        title = "Focus",
        subtitle = if (active) "Session in progress" else "Nothing running",
        modifier = Modifier.clickable { if (active) onOpen() else onStart() },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (active) "${elapsedMinutes}m elapsed" else "${todayMinutes}m today",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (active) spec.accentColor else spec.textColor,
                )
                Text(
                    if (active) label else "Start a 25-minute session",
                    style = MaterialTheme.typography.bodySmall,
                    color = spec.secondaryTextColor,
                )
            }
            if (!active) {
                GlassButton("Focus", onStart, icon = Icons.Rounded.PlayArrow)
            } else {
                PillTag("Live", highlighted = true)
            }
        }
    }
}

@Composable
private fun RoutinesPanel(
    routines: List<Routine>,
    completed: Set<Long>,
    spec: GlassSpec,
    onComplete: (Long) -> Unit,
    onOpen: () -> Unit,
) {
    val dueNow = remember(routines) {
        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        routines.filter { it.enabled }
            .filter { nowMinutes >= it.start - 120 }
            .sortedBy { it.start }
    }
    GlassPanel(
        title = "Routines",
        subtitle = "${completed.size}/${routines.size} done today",
        modifier = Modifier.clickable { onOpen() },
    ) {
        if (routines.isEmpty()) {
            EmptyState(
                title = "No routines yet",
                body = "Morning, wind-down and workout anchors live here.",
                icon = Icons.Rounded.Bedtime,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                dueNow.take(4).forEach { routine ->
                    val done = completed.contains(routine.id)
                    Row(
                        Modifier.fillMaxWidth().clickable { onComplete(routine.id) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape)
                                .background(if (done) spec.accentColor else spec.accentColor.copy(alpha = 0.3f)),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                routine.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (done) spec.secondaryTextColor else spec.textColor,
                                maxLines = 1,
                            )
                            Text(
                                com.nexora.app.domain.model.TimeOfDay(routine.start).format12h() +
                                    " · ${routine.durationMinutes}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.secondaryTextColor,
                            )
                        }
                        if (done) {
                            Icon(Icons.Rounded.Check, "Completed", tint = spec.accentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindersPanel(
    reminders: List<Task>,
    spec: GlassSpec,
    onToggle: (Long, Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    GlassPanel(
        title = "Upcoming reminders",
        subtitle = "${reminders.size} pending",
        modifier = Modifier.clickable { onOpen() },
    ) {
        if (reminders.isEmpty()) {
            EmptyState(
                title = "Nothing pending",
                body = "Reminders you add will surface here and as notifications.",
                icon = Icons.Rounded.Timer,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                reminders.take(4).forEach { task ->
                    TaskCheckRow(task, spec) { onToggle(task.id, true) }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    spec: GlassSpec,
    onOpenAlarms: () -> Unit,
    onOpenTimer: () -> Unit,
    onStartFocus: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenBriefing: () -> Unit,
) {
    val actions = listOf(
        Triple("Alarm", Icons.Rounded.Alarm, onOpenAlarms),
        Triple("Timer", Icons.Rounded.Timer, onOpenTimer),
        Triple("Focus", Icons.Rounded.PlayArrow, onStartFocus),
        Triple("Agenda", Icons.Rounded.CalendarMonth, onOpenCalendar),
        Triple("Insights", Icons.Rounded.Bolt, onOpenInsights),
        Triple("Briefing", Icons.Rounded.AutoAwesome, onOpenBriefing),
    )
    Column {
        SectionHeader("Quick actions")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(actions) { (label, icon, action) ->
                QuickActionTile(label, icon, spec, action)
            }
        }
    }
}

@Composable
private fun QuickActionTile(label: String, icon: ImageVector, spec: GlassSpec, onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier.glassPressPhysics(spec).clickable { onClick() },
        contentPadding = 14.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(spec.cornerDp.dp),
        elevated = false,
    ) {
        Column(
            Modifier.width(78.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(spec.accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = spec.accentColor, modifier = Modifier.size(18.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = spec.textColor, maxLines = 1)
        }
    }
}

@Composable
private fun InsightStrip(
    focusMinutes: Long,
    priorities: List<Task>,
    events: List<ScheduleEvent>,
    spec: GlassSpec,
    onOpen: () -> Unit,
) {
    val done = priorities.count { it.done }
    GlassSurface(modifier = Modifier.fillMaxWidth().clickable { onOpen() }, contentPadding = 16.dp) {
        Column(Modifier.fillMaxWidth()) {
            SectionHeader("Today at a glance")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat("${focusMinutes}m", "Focus", spec)
                MiniStat("$done/${priorities.size}", "Priorities", spec)
                MiniStat("${events.size}", "Events", spec)
                MiniStat("${priorities.count { !it.done }}", "Open", spec)
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, spec: GlassSpec) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = spec.accentColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = spec.secondaryTextColor)
    }
}

internal fun formatDuration(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}
