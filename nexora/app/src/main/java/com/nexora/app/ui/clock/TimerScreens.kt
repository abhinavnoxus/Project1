package com.nexora.app.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.ui.common.EmptyState
import com.nexora.app.ui.common.GlassButton
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.PillTag

/** A running countdown, described by absolute instants so it survives process death. */
data class TimerState(
    val id: Long,
    val label: String,
    val durationMillis: Long,
    val startedAt: Long,
    val pausedAt: Long? = null,
    val elapsedBeforePause: Long = 0L,
) {
    fun elapsed(now: Long): Long =
        if (pausedAt != null) elapsedBeforePause else elapsedBeforePause + (now - startedAt)

    fun remaining(now: Long): Long = (durationMillis - elapsed(now)).coerceAtLeast(0L)

    val isRunning: Boolean get() = pausedAt == null
}

@Composable
fun TimersScreen(
    timers: List<TimerState>,
    now: Long,
    spec: GlassSpec,
    presetMinutes: List<Int>,
    onAdd: (Int) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onOpenStopwatch: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Timers", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                    Text("Run several at once", style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
                }
                GlassButton("Stopwatch", onOpenStopwatch, tonal = true, icon = Icons.Rounded.Timer)
            }
        }

        if (timers.isEmpty()) {
            item {
                GlassPanel(title = "No timers running") {
                    EmptyState("Start a countdown", "Pick a duration below. Timers keep running with the app closed.", icon = Icons.Rounded.Timer)
                }
            }
        }

        items(timers, key = { it.id }) { timer ->
            TimerCard(timer, now, spec, onPause, onResume, onCancel)
        }

        item {
            GlassPanel(title = "Quick start") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetMinutes) { minutes ->
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(spec.textColor.copy(alpha = 0.07f))
                                .clickable { onAdd(minutes) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(
                                "$minutes min",
                                style = MaterialTheme.typography.labelLarge,
                                color = spec.accentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerCard(
    timer: TimerState,
    now: Long,
    spec: GlassSpec,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
) {
    val remaining = timer.remaining(now)
    val progress = if (timer.durationMillis <= 0) 0f
    else (1f - remaining.toFloat() / timer.durationMillis).coerceIn(0f, 1f)
    val finished = remaining <= 0L

    GlassSurface(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 6f
                    drawArc(
                        color = spec.textColor.copy(alpha = 0.10f),
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(spec.accentColor, spec.accent.secondary, spec.accentColor)),
                        startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Text(
                    formatClock(remaining),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = spec.textColor,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(timer.label, style = MaterialTheme.typography.titleMedium, color = spec.textColor)
                Text(
                    when {
                        finished -> "Finished"
                        timer.isRunning -> "Running"
                        else -> "Paused"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (finished) spec.accentColor else spec.secondaryTextColor,
                )
                Spacer(Modifier.height(6.dp))
                PillTag("${timer.durationMillis / 60_000}m total")
            }
            Row {
                if (timer.isRunning) {
                    RoundIcon(Icons.Rounded.Pause, "Pause", spec) { onPause(timer.id) }
                } else if (!finished) {
                    RoundIcon(Icons.Rounded.PlayArrow, "Resume", spec) { onResume(timer.id) }
                }
                RoundIcon(Icons.Rounded.Stop, "Cancel", spec) { onCancel(timer.id) }
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, spec: GlassSpec, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(start = 6.dp)
            .size(38.dp)
            .clip(CircleShape)
            .background(spec.accentColor.copy(alpha = 0.14f))
            .clickable(onClickLabel = label) { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = spec.accentColor, modifier = Modifier.size(18.dp)) }
}

/**
 * Stopwatch.
 *
 * Elapsed time is derived from a monotonic instant captured on start, never from a
 * counter incremented per frame — so it stays accurate regardless of frame rate and
 * survives recomposition. Laps keep their absolute split and the delta from the previous lap.
 */
@Composable
fun StopwatchScreen(
    elapsedMillis: Long,
    laps: List<Long>,
    running: Boolean,
    spec: GlassSpec,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Stopwatch", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
        }
        item {
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatClockPrecise(elapsedMillis),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light),
                        color = spec.textColor,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!running && elapsedMillis == 0L) {
                            GlassButton("Start", onStart, icon = Icons.Rounded.PlayArrow)
                        } else {
                            GlassButton(if (running) "Lap" else "Resume", if (running) onLap else onStart, tonal = true)
                            GlassButton("Stop", onStop, icon = Icons.Rounded.Stop)
                            if (!running) GlassButton("Reset", onReset, tonal = true, icon = Icons.Rounded.Refresh)
                        }
                    }
                }
            }
        }
        if (laps.isNotEmpty()) {
            item {
                GlassPanel(title = "Laps", subtitle = "${laps.size} recorded") {
                    laps.forEachIndexed { index, split ->
                        val previous = if (index == 0) 0L else laps[index - 1]
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Lap ${index + 1}", style = MaterialTheme.typography.bodyMedium, color = spec.secondaryTextColor)
                            Text(
                                formatClockPrecise(split - previous),
                                style = MaterialTheme.typography.bodyMedium,
                                color = spec.accentColor,
                            )
                            Text(
                                formatClockPrecise(split),
                                style = MaterialTheme.typography.bodyMedium,
                                color = spec.textColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun formatClock(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

internal fun formatClockPrecise(millis: Long): String {
    val totalCentis = (millis / 10L).coerceAtLeast(0L)
    val h = totalCentis / 360_000
    val m = (totalCentis % 360_000) / 6_000
    val s = (totalCentis % 6_000) / 100
    val cs = totalCentis % 100
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs) else "%02d:%02d.%02d".format(m, s, cs)
}
