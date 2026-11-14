package com.nexora.app.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.domain.insight.FocusStats
import com.nexora.app.domain.insight.InsightMath
import com.nexora.app.domain.model.FocusSession
import com.nexora.app.domain.model.Routine
import com.nexora.app.domain.model.ScheduleEvent
import com.nexora.app.domain.model.Task
import com.nexora.app.ui.common.EmptyState
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.PillTag
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * INSIGHTS.
 *
 * Every number here is derived from records the app stored. When there is no data the
 * chart is empty and the copy says so — no synthetic trend lines, no invented averages.
 */
@Composable
fun InsightsScreen(
    sessions: List<FocusSession>,
    tasks: List<Task>,
    routines: List<Routine>,
    completedRoutineIds: Set<Long>,
    events: List<ScheduleEvent>,
    spec: GlassSpec,
) {
    val zone = ZoneId.systemDefault()
    val stats = remember(sessions) { InsightMath.summarise(sessions, zone) }
    val today = LocalDate.now()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Insights", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                Text("Measured from your own records", style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
            }
        }

        item {
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = 18.dp) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Focus this week", style = MaterialTheme.typography.titleMedium, color = spec.textColor)
                    Spacer(Modifier.height(14.dp))
                    WeeklyFocusChart(stats, today, spec)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatCell(stats.totalLabel, "total", spec)
                        StatCell("${stats.sessionCount}", "sessions", spec)
                        StatCell("${stats.streakDays}d", "streak", spec)
                        StatCell("${stats.activeDays}", "active days", spec)
                    }
                }
            }
        }

        if (stats.sessionCount == 0) {
            item {
                GlassPanel(title = "No sessions recorded yet") {
                    EmptyState(
                        "Insights appear as you focus",
                        "NEXORA will not draw a trend it has not observed. Run a focus session and this fills in.",
                    )
                }
            }
        }

        item {
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = 18.dp) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Schedule density today", style = MaterialTheme.typography.titleMedium, color = spec.textColor)
                    Spacer(Modifier.height(6.dp))
                    val density = InsightMath.density(events, today, zone)
                    DensityBar(density, spec)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (events.isEmpty()) "No calendar events today — nothing to measure."
                        else "${"%.0f".format(density * 100)}% of a 07:00–22:00 day is booked (${events.size} events).",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                    )
                }
            }
        }

        item {
            val completedTasks = tasks.count { it.done }
            val totalTasks = tasks.size
            GlassPanel(title = "Task completion", subtitle = "All tasks in NEXORA") {
                Column {
                    DensityBar(InsightMath.completionRate(completedTasks, totalTasks), spec)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatCell("$completedTasks", "done", spec)
                        StatCell("${totalTasks - completedTasks}", "open", spec)
                        StatCell("${"%.0f".format(InsightMath.completionRate(completedTasks, totalTasks) * 100)}%", "rate", spec)
                    }
                }
            }
        }

        item {
            val consistency = InsightMath.consistency(completedRoutineIds.size, routines.size)
            GlassPanel(title = "Routine consistency", subtitle = "Today") {
                Column {
                    DensityBar(consistency, spec)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (routines.isEmpty()) "No routines to measure yet."
                        else "${completedRoutineIds.size} of ${routines.size} routines kept today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.secondaryTextColor,
                    )
                }
            }
        }

        item {
            GlassPanel(title = "Monthly focus", subtitle = YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))) {
                MonthlyStrip(stats, today, spec)
            }
        }

        item {
            GlassPanel(title = "What NEXORA can and cannot see") {
                Text(
                    "NEXORA measures focus sessions you start, tasks and routines you complete, and calendar " +
                        "events it is allowed to read. It does not infer app usage, does not run a background " +
                        "tracker, and never extrapolates missing days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = spec.secondaryTextColor,
                )
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, spec: GlassSpec) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = spec.accentColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = spec.secondaryTextColor)
    }
}

@Composable
private fun WeeklyFocusChart(stats: FocusStats, today: LocalDate, spec: GlassSpec) {
    val days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val values = days.map { stats.byDay[it] ?: 0L }
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(30L)

    Column {
        Row(
            Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEachIndexed { index, day ->
                val value = values[index]
                val fraction = (value.toFloat() / maxValue).coerceIn(0f, 1f)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (value > 0) {
                        Text("${value}m", style = MaterialTheme.typography.labelSmall, color = spec.secondaryTextColor)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((96 * fraction).dp.coerceAtLeast(3.dp))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                if (fraction > 0f) Brush.verticalGradient(
                                    listOf(spec.accentColor, spec.accentColor.copy(alpha = 0.45f))
                                ) else Brush.verticalGradient(
                                    listOf(spec.textColor.copy(alpha = 0.06f), spec.textColor.copy(alpha = 0.06f))
                                )
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                Text(
                    day.format(DateTimeFormatter.ofPattern("EE")).take(2),
                    style = MaterialTheme.typography.labelSmall,
                    color = spec.secondaryTextColor,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MonthlyStrip(stats: FocusStats, today: LocalDate, spec: GlassSpec) {
    val month = YearMonth.from(today)
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val values = days.map { stats.byDay[it] ?: 0L }
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Canvas(Modifier.fillMaxWidth().height(60.dp)) {
        val cell = size.width / days.size
        values.forEachIndexed { index, value ->
            val fraction = (value.toFloat() / maxValue).coerceIn(0f, 1f)
            val h = (size.height * fraction).coerceAtLeast(if (value > 0) 3f else 1.5f)
            drawRoundRect(
                color = if (value > 0) spec.accentColor.copy(alpha = 0.35f + 0.65f * fraction)
                else spec.textColor.copy(alpha = 0.08f),
                topLeft = Offset(index * cell, size.height - h),
                size = Size(cell * 0.62f, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    val monthTotal = values.sum()
    Text(
        if (monthTotal == 0L) "No sessions this month yet."
        else "$monthTotal minutes this month across ${values.count { it > 0 }} days.",
        style = MaterialTheme.typography.bodySmall,
        color = spec.secondaryTextColor,
    )
}

@Composable
private fun DensityBar(fraction: Float, spec: GlassSpec) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(spec.textColor.copy(alpha = 0.08f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(10.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(spec.accentColor, spec.accent.secondary))),
        )
    }
}
