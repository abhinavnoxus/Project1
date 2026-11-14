package com.nexora.app.ui.focus

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.domain.insight.InsightMath
import com.nexora.app.domain.model.FocusSession
import com.nexora.app.ui.common.EmptyState
import com.nexora.app.ui.common.GlassButton
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.PillTag
import java.time.Instant
import java.time.ZoneId

/**
 * FOCUS — sessions with break cycles.
 *
 * The ring is the only thing that animates, and it is driven by the remaining time
 * (state), not a frame loop, so a 25-minute session costs one recomposition per second.
 */
@Composable
fun FocusScreen(
    active: Boolean,
    startedAt: Long,
    plannedMinutes: Int,
    label: String,
    sessions: List<FocusSession>,
    spec: GlassSpec,
    onStart: (Int, String) -> Unit,
    onExtend: (Int) -> Unit,
    onEnd: (Boolean) -> Unit,
    onStartBreak: () -> Unit,
) {
    var selectedMinutes by remember { mutableStateOf(plannedMinutes.coerceIn(10, 60)) }
    val now = System.currentTimeMillis()
    val elapsedMillis = if (active) now - startedAt else 0L
    val totalMillis = plannedMinutes * 60_000L
    val progress = if (active && totalMillis > 0) (elapsedMillis.toFloat() / totalMillis).coerceIn(0f, 1f) else 0f

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Focus", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                Text(
                    if (active) "Session running · $label" else "Deep work, measured honestly",
                    style = MaterialTheme.typography.bodySmall,
                    color = spec.secondaryTextColor,
                )
            }
        }

        item {
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = 22.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val stroke = 10f
                            val inset = stroke / 2
                            val arcSize = Size(size.width - stroke, size.height - stroke)
                            drawArc(
                                color = spec.textColor.copy(alpha = 0.08f),
                                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                                topLeft = Offset(inset, inset), size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round),
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(spec.accentColor, spec.accent.secondary, spec.accent.tertiary, spec.accentColor)
                                ),
                                startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                                topLeft = Offset(inset, inset), size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (active) "${elapsedMillis / 60_000}m" else "${selectedMinutes}m",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light),
                                color = spec.textColor,
                            )
                            Text(
                                if (active) "remaining ${((totalMillis - elapsedMillis).coerceAtLeast(0L) / 60_000L)}m" else "planned",
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.secondaryTextColor,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (active) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassButton("Complete", { onEnd(true) }, icon = Icons.Rounded.Stop)
                            GlassButton("Abandon", { onEnd(false) }, tonal = true)
                            GlassButton("+5 min", { onExtend(5) }, tonal = true)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 45, 60).forEach { minutes ->
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedMinutes == minutes) spec.accentColor.copy(alpha = 0.22f)
                                            else spec.textColor.copy(alpha = 0.07f)
                                        )
                                        .clickable { selectedMinutes = minutes }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        "${minutes}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selectedMinutes == minutes) spec.accentColor else spec.secondaryTextColor,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        GlassButton(
                            "Start focus session",
                            { onStart(selectedMinutes, "Focus") },
                            icon = Icons.Rounded.PlayArrow,
                        )
                    }
                }
            }
        }

        item {
            GlassPanel(title = "Break cycle", subtitle = "Pomodoro-style rhythm") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CycleStep("Focus 25m", true, spec)
                    CycleStep("Break 5m", false, spec)
                    CycleStep("Focus 25m", false, spec)
                    CycleStep("Long 15m", false, spec)
                }
                Spacer(Modifier.height(12.dp))
                GlassButton("Start 5-minute break", onStartBreak, tonal = true)
            }
        }

        item {
            val stats = remember(sessions) { InsightMath.summarise(sessions, ZoneId.systemDefault()) }
            GlassPanel(title = "Your focus record", subtitle = "From stored sessions only") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("${stats.totalMinutes / 60}h", "Total")
                    Stat("${stats.sessionCount}", "Sessions")
                    Stat("${stats.averageMinutes}m", "Average")
                    Stat("${stats.streakDays}d", "Streak")
                }
            }
        }

        if (sessions.isNotEmpty()) {
            item { Text("Recent sessions", style = MaterialTheme.typography.titleMedium, color = spec.textColor) }
            items(sessions.take(12), key = { it.id }) { session ->
                GlassSurface(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(session.label, style = MaterialTheme.typography.bodyLarge, color = spec.textColor)
                            Text(
                                Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault())
                                    .format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")),
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.secondaryTextColor,
                            )
                        }
                        PillTag(
                            if (session.completed) "${session.actualMillis / 60_000}m" else "interrupted",
                            highlighted = session.completed,
                        )
                    }
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                GlassPanel(title = "No sessions yet") {
                    EmptyState("Your first session starts the record", "Nothing is fabricated — the charts stay empty until you focus.", icon = Icons.Rounded.PlayArrow)
                }
            }
        }
    }
}

@Composable
private fun CycleStep(label: String, active: Boolean, spec: GlassSpec) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(10.dp).clip(CircleShape)
                .background(if (active) spec.accentColor else spec.textColor.copy(alpha = 0.16f)),
        )
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) spec.textColor else spec.secondaryTextColor)
    }
}

@Composable
private fun Stat(value: String, label: String) {
    val spec = com.nexora.app.design.NexoraTheme.spec
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = spec.accentColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = spec.secondaryTextColor)
    }
}
