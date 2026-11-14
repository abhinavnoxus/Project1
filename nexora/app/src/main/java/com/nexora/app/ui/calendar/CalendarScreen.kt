package com.nexora.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.domain.model.EventConflict
import com.nexora.app.domain.model.ScheduleEvent
import com.nexora.app.domain.model.Task
import com.nexora.app.domain.model.TimeOfDay
import com.nexora.app.platform.CalendarBridge
import com.nexora.app.ui.common.EmptyState
import com.nexora.app.ui.common.GlassButton
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.PillTag
import com.nexora.app.ui.common.SectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * AGENDA + PLANNER.
 *
 * The agenda reads the calendar (read-only) and combines it with NEXORA's own tasks so a
 * day is seen in one place. Conflicts are *reported*, never auto-resolved.
 */
@Composable
fun CalendarScreen(
    date: LocalDate,
    now: ZonedDateTime,
    events: List<ScheduleEvent>,
    tasks: List<Task>,
    granted: Boolean,
    spec: GlassSpec,
    onGrant: () -> Unit,
    onRequestRefresh: () -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onOpenPlanner: () -> Unit,
) {
    val conflicts = remember(events) { CalendarBridge.conflicts(events) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Agenda", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEEE d MMMM")),
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                    )
                }
                GlassButton("Plan", onOpenPlanner, tonal = true, icon = Icons.Rounded.Event)
            }
        }

        if (!granted) {
            item {
                GlassPanel(title = "Calendar not connected") {
                    Text(
                        "NEXORA reads today's events locally, on device. It never writes to your calendar " +
                            "and never uploads event data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                    )
                    Spacer(Modifier.height(10.dp))
                    GlassButton("Connect calendar", onGrant)
                }
            }
        }

        if (conflicts.isNotEmpty()) {
            item {
                GlassSurface(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Warning, null, tint = androidx.compose.ui.graphics.Color(0xFFFFB55A), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("${conflicts.size} overlapping event${if (conflicts.size == 1) "" else "s"}", style = MaterialTheme.typography.titleSmall, color = spec.textColor)
                            conflicts.take(2).forEach { c ->
                                Text(
                                    "\"${c.first.title}\" overlaps \"${c.second.title}\" by ${c.overlapMinutes} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = spec.secondaryTextColor,
                                )
                            }
                            Text(
                                "NEXORA only reports this. Nothing was changed.",
                                style = MaterialTheme.typography.labelSmall,
                                color = spec.accentColor,
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader("Timeline") }

        if (events.isEmpty() && tasks.isEmpty()) {
            item {
                GlassPanel(title = "Open day") {
                    EmptyState(
                        "Nothing on the calendar",
                        if (granted) "Add a task or start a focus block." else "Connect your calendar to see events.",
                        icon = Icons.Rounded.Event,
                    )
                }
            }
        }

        items(events, key = { "event-${it.id}" }) { event ->
            TimelineEvent(event, now, spec)
        }

        if (tasks.isNotEmpty()) {
            item { SectionHeader("Tasks today") }
            items(tasks, key = { "task-${it.id}" }) { task ->
                GlassSurface(Modifier.fillMaxWidth().clickable { onToggleTask(task.id, !task.done) }, contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(20.dp).clip(CircleShape)
                                .background(if (task.done) spec.accentColor else spec.textColor.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (task.done) Text("✓", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF04121C))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (task.done) spec.secondaryTextColor else spec.textColor,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            task.dueAt?.let { due ->
                                Text(
                                    Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = spec.secondaryTextColor,
                                )
                            }
                        }
                        PillTag(task.kind.name)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("Add task", onAddTask, icon = Icons.Rounded.Add)
                GlassButton("Refresh", onRequestRefresh, tonal = true)
            }
        }
    }
}

@Composable
private fun TimelineEvent(event: ScheduleEvent, now: ZonedDateTime, spec: GlassSpec) {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(event.startMillis).atZone(zone)
    val end = Instant.ofEpochMilli(event.endMillis).atZone(zone)
    val live = !event.allDay && now.isAfter(start) && now.isBefore(end)
    val past = now.isAfter(end)

    GlassSurface(
        Modifier.fillMaxWidth(),
        contentPadding = 14.dp,
        elevated = !past,
    ) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.width(64.dp)) {
                Text(
                    if (event.allDay) "All day" else start.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (live) spec.accentColor else spec.textColor,
                )
                if (!event.allDay) {
                    Text(
                        end.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                    )
                }
            }
            Box(
                Modifier
                    .width(3.dp)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(if (live) spec.accentColor else spec.accentColor.copy(alpha = 0.30f)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (past) spec.secondaryTextColor else spec.textColor,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                val meta = listOfNotNull(event.location, event.calendarName).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor, maxLines = 1)
                }
            }
            if (live) PillTag("Now", highlighted = true)
        }
    }
}

/**
 * WORLD CLOCKS + TIME-ZONE CONVERTER.
 *
 * Conversion uses the real IANA database through java.time, so DST is handled correctly
 * on both sides of the conversion.
 */
@Composable
fun WorldClockScreen(
    zones: List<Triple<Long, String, String>>, // id, zoneId, label
    spec: GlassSpec,
    now: Long,
    onAdd: (String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var converterFrom by remember { mutableStateOf(ZoneId.systemDefault().id) }
    var converterTo by remember { mutableStateOf("America/New_York") }
    var converterTime by remember { mutableStateOf(TimeOfDay.of(java.time.LocalTime.now().hour, 0)) }

    val available = remember {
        ZoneId.getAvailableZoneIds().sorted()
    }
    val filtered = remember(query) {
        if (query.isBlank()) available.take(12)
        else available.filter { it.contains(query, ignoreCase = true) }.take(12)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("World clocks", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                Text("Live in every zone you add", style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
            }
        }

        items(zones, key = { it.first }) { (id, zoneId, label) ->
            val zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
            val local = Instant.ofEpochMilli(now).atZone(zone)
            val home = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
            val offsetHours = (local.offset.totalSeconds - home.offset.totalSeconds) / 3600.0
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium, color = spec.textColor)
                        Text(
                            zoneId,
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.secondaryTextColor,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        PillTag(
                            when {
                                offsetHours == 0.0 -> "Same time as you"
                                offsetHours > 0 -> "+${"%.0f".format(offsetHours)}h"
                                else -> "${"%.0f".format(offsetHours)}h"
                            }
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            local.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.headlineSmall,
                            color = spec.textColor,
                        )
                        Text(
                            local.format(DateTimeFormatter.ofPattern("EEE")),
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.secondaryTextColor,
                        )
                        Text(
                            "Remove",
                            style = MaterialTheme.typography.labelSmall,
                            color = spec.accentColor,
                            modifier = Modifier.clickable { onRemove(id) },
                        )
                    }
                }
            }
        }

        item {
            GlassPanel(title = "Add a zone") {
                androidx.compose.material3.OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search, e.g. Tokyo", color = spec.secondaryTextColor) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = spec.accentColor,
                        unfocusedBorderColor = spec.textColor.copy(alpha = 0.15f),
                        focusedTextColor = spec.textColor,
                        unfocusedTextColor = spec.textColor,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filtered.forEach { zoneId ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onAdd(zoneId) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Public, null, tint = spec.accentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(zoneId, style = MaterialTheme.typography.bodyMedium, color = spec.textColor, modifier = Modifier.weight(1f))
                            Text("Add", style = MaterialTheme.typography.labelSmall, color = spec.accentColor)
                        }
                    }
                }
            }
        }

        item {
            GlassPanel(title = "Time-zone converter", subtitle = "Handles DST on both sides") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZonePill("From", converterFrom, spec) { converterFrom = it }
                    ZonePill("To", converterTo, spec) { converterTo = it }
                }
                Spacer(Modifier.height(12.dp))
                val fromZone = runCatching { ZoneId.of(converterFrom) }.getOrDefault(ZoneId.systemDefault())
                val toZone = runCatching { ZoneId.of(converterTo) }.getOrDefault(ZoneId.systemDefault())
                val today = LocalDate.now()
                val fromInstant = today.atTime(converterTime.hour, converterTime.minute).atZone(fromZone)
                val converted = fromInstant.withZoneSameInstant(toZone)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(converterTime.format12h(), style = MaterialTheme.typography.titleLarge, color = spec.textColor)
                        Text(converterFrom.substringAfterLast('/'), style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
                    }
                    Text("→", style = MaterialTheme.typography.titleLarge, color = spec.accentColor)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            converted.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.titleLarge,
                            color = spec.accentColor,
                        )
                        Text(
                            converted.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.secondaryTextColor,
                        )
                        if (converted.toLocalDate() != fromInstant.toLocalDate()) {
                            PillTag(if (converted.toLocalDate().isAfter(fromInstant.toLocalDate())) "Next day" else "Previous day", highlighted = true)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-60, -15, 15, 60).forEach { delta ->
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp)).background(spec.textColor.copy(alpha = 0.07f))
                                .clickable { converterTime = converterTime.plusMinutes(delta) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                if (delta > 0) "+${delta}m" else "${delta}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = spec.secondaryTextColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZonePill(label: String, zoneId: String, spec: GlassSpec, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = spec.secondaryTextColor)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(spec.textColor.copy(alpha = 0.07f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(zoneId.substringAfterLast('/'), style = MaterialTheme.typography.bodyMedium, color = spec.textColor, maxLines = 1)
        }
        if (expanded) {
            Column {
                listOf("UTC", "Asia/Kolkata", "Asia/Tokyo", "Europe/London", "Europe/Berlin", "America/New_York", "America/Los_Angeles", "Australia/Sydney").forEach { z ->
                    Text(
                        z.substringAfterLast('/'),
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                        modifier = Modifier.fillMaxWidth().clickable { onChange(z); expanded = false }.padding(vertical = 6.dp, horizontal = 4.dp),
                    )
                }
            }
        }
    }
}
