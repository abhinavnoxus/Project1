package com.nexora.app.ui.routines

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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.unit.dp
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.domain.model.Routine
import com.nexora.app.domain.model.TimeOfDay
import com.nexora.app.domain.model.WeekdayMask
import com.nexora.app.ui.common.EmptyState
import com.nexora.app.ui.common.GlassButton
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.PillTag
import java.time.DayOfWeek

@Composable
fun RoutinesScreen(
    routines: List<Routine>,
    completedToday: Set<Long>,
    streaks: Map<Long, Int>,
    spec: GlassSpec,
    onComplete: (Long) -> Unit,
    onSave: (Routine) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf<Routine?>(null) }
    var creating by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Routines", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                    Text(
                        "${completedToday.size}/${routines.size} complete today",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                    )
                }
                GlassButton("New", { creating = true }, icon = Icons.Rounded.Add)
            }
        }

        if (routines.isEmpty()) {
            item {
                GlassPanel(title = "No routines") {
                    EmptyState(
                        "Anchor your day",
                        "Morning, wind-down, workout — routines show up on Home and in the planner.",
                        icon = Icons.Rounded.Bedtime,
                    )
                }
            }
        }

        items(routines, key = { it.id }) { routine ->
            val done = completedToday.contains(routine.id)
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(if (done) spec.accentColor else spec.accentColor.copy(alpha = 0.14f))
                            .clickable { onComplete(routine.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Rounded.Check, "Completed", tint = androidx.compose.ui.graphics.Color(0xFF04121C), modifier = Modifier.size(20.dp))
                        else Text(TimeOfDay(routine.start).format12h().take(5), style = MaterialTheme.typography.labelSmall, color = spec.accentColor)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f).clickable { editing = routine }) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium, color = spec.textColor)
                        Text(
                            "${TimeOfDay(routine.start).format12h()} · ${routine.durationMinutes} min · ${routine.repeat.label()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.secondaryTextColor,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val streak = streaks[routine.id] ?: 0
                            if (streak > 0) PillTag("$streak-day streak", highlighted = true)
                            if (routine.repeat.isEmpty) PillTag("One-off")
                        }
                    }
                    IconButton(onClick = { onDelete(routine.id) }) {
                        Icon(Icons.Rounded.Delete, "Delete routine", tint = spec.secondaryTextColor, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (creating || editing != null) {
            item {
                val target = editing ?: Routine()
                RoutineEditor(target, spec, onCancel = { creating = false; editing = null }, onSave = {
                    onSave(it); creating = false; editing = null
                })
            }
        }
    }
}

@Composable
private fun RoutineEditor(routine: Routine, spec: GlassSpec, onCancel: () -> Unit, onSave: (Routine) -> Unit) {
    var draft by remember(routine) { mutableStateOf(routine) }
    GlassPanel(
        title = if (routine.id == 0L) "New routine" else "Edit routine",
        trailing = { Text("Cancel", color = spec.accentColor, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable { onCancel() }) },
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = draft.name,
            onValueChange = { draft = draft.copy(name = it.take(40)) },
            placeholder = { Text("Routine name", color = spec.secondaryTextColor) },
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Start", style = MaterialTheme.typography.bodyMedium, color = spec.textColor)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-15, 15).forEach { delta ->
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(spec.textColor.copy(alpha = 0.08f))
                            .clickable { draft = draft.copy(start = Math.floorMod(draft.start + delta, 1440)) },
                        contentAlignment = Alignment.Center,
                    ) { Text(if (delta < 0) "–" else "+", color = spec.textColor) }
                }
                Text(TimeOfDay(draft.start).format12h(), style = MaterialTheme.typography.bodyLarge, color = spec.accentColor)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Duration", style = MaterialTheme.typography.bodyMedium, color = spec.textColor)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-5, 5).forEach { delta ->
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(spec.textColor.copy(alpha = 0.08f))
                            .clickable { draft = draft.copy(durationMinutes = (draft.durationMinutes + delta).coerceIn(5, 240)) },
                        contentAlignment = Alignment.Center,
                    ) { Text(if (delta < 0) "–" else "+", color = spec.textColor) }
                }
                Text("${draft.durationMinutes} min", style = MaterialTheme.typography.bodyLarge, color = spec.accentColor)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { day ->
                val on = draft.repeat.contains(day)
                Box(
                    Modifier.weight(1f).height(36.dp).clip(CircleShape)
                        .background(if (on) spec.accentColor else spec.textColor.copy(alpha = 0.07f))
                        .clickable { draft = draft.copy(repeat = draft.repeat.with(day, !on)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(day.name.take(1), style = MaterialTheme.typography.labelMedium, color = if (on) androidx.compose.ui.graphics.Color(0xFF04121C) else spec.secondaryTextColor)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        GlassButton(
            if (routine.id == 0L) "Create routine" else "Save changes",
            { if (draft.name.isNotBlank()) onSave(draft) },
            icon = Icons.Rounded.Check,
        )
    }
}
