package com.nexora.app.ui.planner

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.domain.model.Task
import com.nexora.app.domain.model.TaskKind
import com.nexora.app.ui.common.EmptyState
import com.nexora.app.ui.common.GlassButton
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.PillTag
import com.nexora.app.ui.common.SectionHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * PLANNER — every task, reminder and priority in one list.
 *
 * Reminders are real: anything with a due time gets an OS-level notification through
 * [com.nexora.app.alarm.ReminderScheduler], so "reminds me later" is not a label on a row.
 */
@Composable
fun PlannerScreen(
    tasks: List<Task>,
    spec: GlassSpec,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    var filter by remember { mutableStateOf(PlannerFilter.All) }

    val visible = remember(tasks, filter) {
        tasks.filter { task ->
            when (filter) {
                PlannerFilter.All -> true
                PlannerFilter.Open -> !task.done
                PlannerFilter.Done -> task.done
                PlannerFilter.Reminders -> task.kind == TaskKind.Reminder
            }
        }
    }
    val open = tasks.count { !it.done }
    val done = tasks.count { it.done }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Planner", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                    Text(
                        "$open open · $done done",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                    )
                }
                GlassButton("New", onAdd, icon = Icons.Rounded.Add)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlannerFilter.entries.forEach { candidate ->
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(
                                if (filter == candidate) spec.accentColor.copy(alpha = 0.22f)
                                else spec.textColor.copy(alpha = 0.06f)
                            )
                            .clickable { filter = candidate }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            candidate.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (filter == candidate) spec.accentColor else spec.secondaryTextColor,
                        )
                    }
                }
            }
        }

        if (visible.isEmpty()) {
            item {
                GlassPanel(title = "Nothing here") {
                    EmptyState(
                        title = when (filter) {
                            PlannerFilter.Done -> "No completed items yet"
                            PlannerFilter.Reminders -> "No reminders set"
                            else -> "No tasks yet"
                        },
                        body = "Tasks and reminders appear here the moment you add them.",
                        icon = Icons.Rounded.Schedule,
                    )
                    GlassButton("Add something", onAdd, icon = Icons.Rounded.Add)
                }
            }
        }

        items(visible, key = { it.id }) { task ->
            PlannerRow(task, spec, onToggle, onDelete)
        }
    }
}

private enum class PlannerFilter(val label: String) {
    All("All"),
    Open("Open"),
    Done("Done"),
    Reminders("Reminders"),
}

@Composable
private fun PlannerRow(
    task: Task,
    spec: GlassSpec,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    GlassSurface(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (task.done) spec.accentColor else spec.textColor.copy(alpha = 0.08f))
                    .clickable { onToggle(task.id, !task.done) },
                contentAlignment = Alignment.Center,
            ) {
                if (task.done) {
                    Icon(
                        Icons.Rounded.Check, "Completed",
                        tint = androidx.compose.ui.graphics.Color(0xFF04121C),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.done) spec.secondaryTextColor else spec.textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                )
                if (task.note.isNotBlank()) {
                    Text(
                        task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (task.kind == TaskKind.Priority) PillTag("Priority ${task.priority}", highlighted = true)
                    if (task.kind == TaskKind.Reminder) {
                        PillTag("Reminder")
                    }
                    task.dueAt?.let { due ->
                        val at = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault())
                        PillTag(at.format(DateTimeFormatter.ofPattern("d MMM HH:mm")))
                    }
                }
            }
            IconButton(onClick = { onDelete(task.id) }) {
                Icon(
                    Icons.Rounded.Delete, "Delete",
                    tint = spec.secondaryTextColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Alarm health shortcut surfaced from the timer screen's empty state. */
@Composable
fun ReminderInfoPanel(spec: GlassSpec) {
    GlassSurface(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NotificationsActive, null, tint = spec.accentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Reminders with a due time are delivered as system notifications, scheduled " +
                    "inexactly so they never consume the exact-alarm budget your alarms need.",
                style = MaterialTheme.typography.bodySmall,
                color = spec.secondaryTextColor,
            )
        }
    }
}
