package com.nexora.app.ui.alarm

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VolumeUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.design.NexoraTheme
import com.nexora.app.design.glassPressPhysics
import com.nexora.app.domain.alarm.AlarmMath
import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.AlarmKind
import com.nexora.app.domain.model.TimeOfDay
import com.nexora.app.domain.model.WeekdayMask
import com.nexora.app.ui.common.EmptyState
import com.nexora.app.ui.common.GlassButton
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.NexoraSwitch
import com.nexora.app.ui.common.PillTag
import com.nexora.app.ui.common.SectionHeader
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** ALARMS — list, enabled state, next-fire countdown. */
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    spec: GlassSpec,
    onToggle: (Long, Boolean) -> Unit,
    onEdit: (Long) -> Unit,
    onCreate: () -> Unit,
    onHealth: () -> Unit,
    onPreviewRing: (Alarm) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Alarms", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                    Text(
                        "${alarms.count { it.enabled }} active of ${alarms.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                    )
                }
                GlassButton("New", onCreate, icon = Icons.Rounded.Add)
            }
        }

        item {
            GlassSurface(
                Modifier.fillMaxWidth().clickable { onHealth() },
                contentPadding = 14.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(spec.accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.HealthAndSafety, null, tint = spec.accentColor, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Alarm Health Centre", style = MaterialTheme.typography.titleSmall, color = spec.textColor)
                        Text(
                            "Permissions, exact-alarm access, reliability",
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.secondaryTextColor,
                        )
                    }
                }
            }
        }

        if (alarms.isEmpty()) {
            item {
                GlassPanel(title = "No alarms yet") {
                    EmptyState(
                        title = "Build your first alarm",
                        body = "Repeating, one-shot and label-your-own. Exact to the minute.",
                        icon = Icons.Rounded.Alarm,
                    )
                    GlassButton("Create alarm", onCreate, icon = Icons.Rounded.Add)
                }
            }
        } else {
            items(alarms, key = { it.id }) { alarm ->
                AlarmRow(alarm, spec, onToggle, onEdit, onPreviewRing)
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    spec: GlassSpec,
    onToggle: (Long, Boolean) -> Unit,
    onEdit: (Long) -> Unit,
    onPreview: (Alarm) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val next = remember(alarm) {
        AlarmMath.nextTrigger(alarm, System.currentTimeMillis(), zone)
    }
    GlassSurface(
        Modifier.fillMaxWidth().glassPressPhysics(spec).clickable { onEdit(alarm.id) },
        contentPadding = 16.dp,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        alarm.time.format12h(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (alarm.enabled) spec.textColor else spec.secondaryTextColor,
                    )
                }
                Text(
                    alarm.label.ifBlank { "Alarm" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = spec.secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillTag(alarm.repeat.label())
                    if (alarm.vibrate) PillTag("Vibrate")
                    if (next != null && alarm.enabled) {
                        PillTag(
                            "in " + com.nexora.app.ui.home.formatDuration(next - System.currentTimeMillis()),
                            highlighted = true,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                NexoraSwitch(alarm.enabled, { onToggle(alarm.id, it) })
                IconButton(onClick = { onPreview(alarm) }) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = "Preview ringtone",
                        tint = spec.secondaryTextColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** ALARM EDITOR — creation and editing with live next-fire preview. */
@Composable
fun AlarmEditorScreen(
    existing: Alarm?,
    spec: GlassSpec,
    onSave: (Alarm) -> Unit,
    onDelete: ((Long) -> Unit)?,
    onBack: () -> Unit,
    onPickRingtone: () -> Unit,
    previewRingtones: List<Pair<String?, String>>,
) {
    var alarm by remember(existing) {
        mutableStateOf(existing ?: Alarm(hour = 7, minute = 0, repeat = WeekdayMask.EveryDay, label = "Alarm"))
    }
    var ringtoneLabel by remember { mutableStateOf("System default") }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (existing == null) "New alarm" else "Edit alarm",
                        style = MaterialTheme.typography.headlineMedium,
                        color = spec.textColor,
                    )
                    IconButton(onClick = onBack) { Text("Close", color = spec.accentColor) }
                }
            }

            item {
                GlassSurface(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            alarm.time.format12h(),
                            style = MaterialTheme.typography.displayLarge,
                            color = spec.textColor,
                        )
                        Spacer(Modifier.height(14.dp))
                        TimePicker(
                            time = alarm.time,
                            spec = spec,
                            onChange = { alarm = alarm.copy(hour = it.hour, minute = it.minute) },
                        )
                    }
                }
            }

            item {
                GlassPanel(title = "Repeat") {
                    DayChips(alarm.repeat, spec) { mask -> alarm = alarm.copy(repeat = mask) }
                }
            }

            item {
                GlassPanel(title = "Label") {
                    androidx.compose.material3.OutlinedTextField(
                        value = alarm.label,
                        onValueChange = { alarm = alarm.copy(label = it.take(40)) },
                        placeholder = { Text("Alarm label", color = spec.secondaryTextColor) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = spec.accentColor,
                            unfocusedBorderColor = spec.textColor.copy(alpha = 0.15f),
                            focusedTextColor = spec.textColor,
                            unfocusedTextColor = spec.textColor,
                        ),
                    )
                }
            }

            item {
                GlassPanel(title = "Ringtone") {
                    Row(
                        Modifier.fillMaxWidth().clickable { onPickRingtone() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, tint = spec.accentColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            previewRingtones.firstOrNull { it.first == alarm.ringtoneUri }?.second
                                ?: ringtoneLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = spec.textColor,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Rounded.VolumeUp, null, tint = spec.secondaryTextColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(previewRingtones) { (uri, name) ->
                            val selected = alarm.ringtoneUri == uri && uri != null
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) spec.accentColor.copy(alpha = 0.20f)
                                        else spec.textColor.copy(alpha = 0.06f)
                                    )
                                    .clickable {
                                        alarm = alarm.copy(ringtoneUri = uri)
                                        ringtoneLabel = name
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) spec.accentColor else spec.secondaryTextColor,
                                )
                            }
                        }
                    }
                }
            }

            item {
                GlassPanel(title = "Behaviour") {
                    ToggleRow("Vibrate", alarm.vibrate, spec) { alarm = alarm.copy(vibrate = it) }
                    Stepper(
                        "Snooze", value = "${alarm.snoozeMinutes} min", spec,
                        onMinus = { alarm = alarm.copy(snoozeMinutes = (alarm.snoozeMinutes - 5).coerceAtLeast(1)) },
                        onPlus = { alarm = alarm.copy(snoozeMinutes = (alarm.snoozeMinutes + 5).coerceAtMost(60)) },
                    )
                    Stepper(
                        "Snooze limit", value = "${alarm.maxSnoozes}×", spec,
                        onMinus = { alarm = alarm.copy(maxSnoozes = (alarm.maxSnoozes - 1).coerceAtLeast(0)) },
                        onPlus = { alarm = alarm.copy(maxSnoozes = (alarm.maxSnoozes + 1).coerceAtMost(10)) },
                    )
                    Stepper(
                        "Fade in", value = "${alarm.fadeInSeconds}s", spec,
                        onMinus = { alarm = alarm.copy(fadeInSeconds = (alarm.fadeInSeconds - 2).coerceAtLeast(0)) },
                        onPlus = { alarm = alarm.copy(fadeInSeconds = (alarm.fadeInSeconds + 2).coerceAtMost(30)) },
                    )
                    ToggleRow(
                        "One-shot (fires once)",
                        alarm.kind == AlarmKind.OneShot,
                        spec,
                    ) { oneShot ->
                        alarm = alarm.copy(
                            kind = if (oneShot) AlarmKind.OneShot else AlarmKind.Repeating,
                            oneShotAt = if (oneShot) {
                                val now = java.time.ZonedDateTime.now()
                                now.toLocalDate().atTime(alarm.hour, alarm.minute)
                                    .atZone(now.zone)
                                    .toInstant().toEpochMilli()
                                    .let { ms -> if (ms <= System.currentTimeMillis()) ms + 86_400_000L else ms }
                            } else null,
                        )
                    }
                }
            }

            item {
                NextFiresPreview(alarm, spec)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (onDelete != null && existing != null) {
                GlassButton("Delete", onDelete.let { d -> { d(existing.id) } }, tonal = true, icon = Icons.Rounded.Delete)
            }
            GlassButton(
                "Save alarm",
                onClick = {
                    val toSave = if (alarm.kind == AlarmKind.OneShot && alarm.oneShotAt == null) {
                        alarm.copy(
                            oneShotAt = AlarmMath.nextTrigger(
                                alarm.copy(kind = AlarmKind.Repeating),
                                System.currentTimeMillis(),
                            )
                        )
                    } else alarm
                    onSave(toSave)
                },
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Check,
            )
        }
    }
}

@Composable
private fun NextFiresPreview(alarm: Alarm, spec: GlassSpec) {
    val fires = remember(alarm) {
        AlarmMath.upcomingTriggers(
            alarm.copy(enabled = true),
            System.currentTimeMillis(),
            count = 3,
        )
    }
    GlassPanel(title = "Next 3 firings", subtitle = "Computed locally from your repeat rule") {
        if (fires.isEmpty()) {
            Text(
                "This rule produces no future firing. Pick at least one day.",
                style = MaterialTheme.typography.bodySmall,
                color = spec.secondaryTextColor,
            )
        } else {
            val fmt = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                fires.forEach { millis ->
                    val at = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(spec.accentColor))
                        Spacer(Modifier.width(10.dp))
                        Text(at.format(fmt), style = MaterialTheme.typography.bodyMedium, color = spec.textColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayChips(mask: WeekdayMask, spec: GlassSpec, onChange: (WeekdayMask) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { day ->
                val on = mask.contains(day)
                Box(
                    Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(if (on) spec.accentColor else spec.textColor.copy(alpha = 0.07f))
                        .clickable { onChange(mask.with(day, !on)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.name.take(1),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) androidx.compose.ui.graphics.Color(0xFF04121C) else spec.secondaryTextColor,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Once" to WeekdayMask.None,
                "Every day" to WeekdayMask.EveryDay,
                "Weekdays" to WeekdayMask.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                "Weekends" to WeekdayMask.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            ).forEach { (label, preset) ->
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (mask == preset) spec.accentColor.copy(alpha = 0.2f) else spec.textColor.copy(alpha = 0.06f))
                        .clickable { onChange(preset) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) { Text(label, style = MaterialTheme.typography.labelSmall, color = if (mask == preset) spec.accentColor else spec.secondaryTextColor) }
            }
        }
    }
}

/** Wheel-style time picker built from two scroll-free steppers; precise and accessible. */
@Composable
fun TimePicker(time: TimeOfDay, spec: GlassSpec, onChange: (TimeOfDay) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeColumn(
            value = if (time.hour % 12 == 0) 12 else time.hour % 12,
            spec = spec,
            onUp = { onChange(TimeOfDay.of((time.hour + 1) % 24, time.minute)) },
            onDown = { onChange(TimeOfDay.of((time.hour + 23) % 24, time.minute)) },
        )
        Text(":", style = MaterialTheme.typography.displaySmall, color = spec.secondaryTextColor, modifier = Modifier.padding(horizontal = 12.dp))
        TimeColumn(
            value = time.minute,
            spec = spec,
            onUp = { onChange(TimeOfDay.of(time.hour, (time.minute + 1) % 60)) },
            onDown = { onChange(TimeOfDay.of(time.hour, (time.minute + 59) % 60)) },
        )
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("AM" to (time.hour % 12), "PM" to (time.hour % 12 + 12)).forEach { (label, hourValue) ->
                val selected = if (label == "AM") time.hour < 12 else time.hour >= 12
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) spec.accentColor.copy(alpha = 0.22f) else spec.textColor.copy(alpha = 0.06f))
                        .clickable { onChange(TimeOfDay.of(hourValue, time.minute)) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) spec.accentColor else spec.secondaryTextColor)
                }
            }
        }
    }
}

@Composable
private fun TimeColumn(value: Int, spec: GlassSpec, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(spec.textColor.copy(alpha = 0.06f)).clickable { onUp() },
            contentAlignment = Alignment.Center,
        ) { Text("+", color = spec.accentColor, style = MaterialTheme.typography.titleMedium) }
        Text(
            "%02d".format(value),
            style = MaterialTheme.typography.displaySmall,
            color = spec.textColor,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(spec.textColor.copy(alpha = 0.06f)).clickable { onDown() },
            contentAlignment = Alignment.Center,
        ) { Text("–", color = spec.accentColor, style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, spec: GlassSpec, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = spec.textColor)
        NexoraSwitch(checked, onChange)
    }
}

@Composable
private fun Stepper(label: String, value: String, spec: GlassSpec, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = spec.textColor)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepButton("–", spec, onMinus)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = spec.accentColor)
            StepButton("+", spec, onPlus)
        }
    }
}

@Composable
private fun StepButton(symbol: String, spec: GlassSpec, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(spec.textColor.copy(alpha = 0.08f)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, color = spec.textColor, style = MaterialTheme.typography.titleMedium) }
}

/** ALARM HEALTH CENTRE — every dependency the alarm engine has, with its real state. */
@Composable
fun AlarmHealthScreen(
    health: com.nexora.app.ui.AlarmHealth,
    device: com.nexora.app.platform.DeviceProfile,
    spec: GlassSpec,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onBatteryOptimisation: () -> Unit,
    onRestoreAlarms: () -> Unit,
    onTestRing: () -> Unit,
    onBack: () -> Unit,
) {
    val notes = remember(device) { com.nexora.app.platform.PowerGuidance.oemNotes(device) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Alarm Health", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                IconButton(onClick = onBack) { Text("Back", color = spec.accentColor) }
            }
        }

        item {
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = 18.dp) {
                Column(Modifier.fillMaxWidth()) {
                    StatusLine(
                        "Notification permission",
                        if (health.notificationsEnabled) "Granted" else "Missing",
                        health.notificationsEnabled,
                        spec,
                        if (!health.notificationsEnabled) onOpenNotificationSettings else null,
                    )
                    StatusLine(
                        "Alarms channel",
                        when {
                            !health.channelEnabled -> "Muted in system settings"
                            else -> "Enabled, high importance"
                        },
                        health.channelEnabled,
                        spec,
                        if (!health.channelEnabled) onOpenNotificationSettings else null,
                    )
                    StatusLine(
                        "Exact alarm access",
                        if (health.exactAlarmsAllowed) "Allowed — alarms fire on time"
                        else "Not granted — alarms may be delayed",
                        health.exactAlarmsAllowed,
                        spec,
                        if (!health.exactAlarmsAllowed && health.sdkInt >= 31) onOpenExactAlarmSettings else null,
                    )
                    StatusLine(
                        "Battery optimisation",
                        if (health.batteryOptimised) "NEXORA is exempt"
                        else "Not exempt — the OS may freeze alarms",
                        health.batteryOptimised,
                        spec,
                        if (!health.batteryOptimised) onBatteryOptimisation else null,
                    )
                    StatusLine(
                        "Ringtone files",
                        health.ringtoneIssue?.let { "Missing for \"$it\"" } ?: "All selected ringtones readable",
                        health.ringtoneIssue == null,
                        spec,
                        null,
                    )
                }
            }
        }

        item {
            GlassPanel(title = "Scheduling") {
                InfoRow("Enabled alarms", "${health.enabledCount} of ${health.totalCount}", spec)
                InfoRow(
                    "Next alarm",
                    health.nextAlarmMillis?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm"))
                    } ?: "None scheduled",
                    spec,
                )
                InfoRow(
                    "Last dismiss latency",
                    if (health.lastDismissLatencyMillis > 0) "${health.lastDismissLatencyMillis} ms after trigger" else "No dismiss recorded yet",
                    spec,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton("Re-arm all", onRestoreAlarms, tonal = true)
                    GlassButton("Test ring", onTestRing)
                }
            }
        }

        item {
            GlassPanel(title = "Device", subtitle = device.summary()) {
                InfoRow("Class", device.classLabel, spec)
                InfoRow("Blur support", if (device.supportsBlur) "RenderEffect (API 31+)" else "Gradient fallback", spec)
                if (health.realmeUiDetected) {
                    Spacer(Modifier.height(6.dp))
                    PillTag("Realme UI detected", highlighted = true)
                }
            }
        }

        if (notes.isNotEmpty()) {
            item {
                GlassPanel(title = "What this device needs", subtitle = "Only shown because it applies") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        notes.forEach { note ->
                            Row {
                                Text("•", color = spec.accentColor)
                                Spacer(Modifier.width(8.dp))
                                Text(note, style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassPanel(title = "How dismissal works", subtitle = "Why a tap always stops the sound") {
                Text(
                    "Each firing gets a unique instance id. The notification's Dismiss action is a broadcast, " +
                        "not an activity, so it silences the ringtone, cancels that exact notification and updates " +
                        "state immediately — even with the screen locked, the app closed, or the activity destroyed. " +
                        "Repeated taps are idempotent, and taps on stale notifications are ignored.",
                    style = MaterialTheme.typography.bodySmall,
                    color = spec.secondaryTextColor,
                )
            }
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
    ok: Boolean,
    spec: GlassSpec,
    action: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).then(
            if (action != null) Modifier.clickable { action() } else Modifier
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(9.dp).clip(CircleShape)
                .background(if (ok) spec.accentColor else androidx.compose.ui.graphics.Color(0xFFFFB55A)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = spec.textColor)
            Text(value, style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
        }
        if (action != null) Text("Fix", style = MaterialTheme.typography.labelMedium, color = spec.accentColor)
    }
}

@Composable
private fun InfoRow(label: String, value: String, spec: GlassSpec) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = spec.secondaryTextColor)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = spec.textColor)
    }
}
