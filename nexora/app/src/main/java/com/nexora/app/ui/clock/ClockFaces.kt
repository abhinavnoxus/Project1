package com.nexora.app.ui.clock

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.app.design.ClockFace
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.NexoraTheme
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val TIME_12 = DateTimeFormatter.ofPattern("h:mm")
private val SECONDS = DateTimeFormatter.ofPattern("ss")
private val AM_PM = DateTimeFormatter.ofPattern("a")
private val TIME_24 = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The animated live clock.
 *
 * One `1 Hz`-free design: the seconds hand advances by animating to the *current* second
 * on every recomposition triggered by the state holder, rather than running a permanent
 * frame loop. When the user picks "Still", the smooth sweep is dropped entirely and the
 * clock simply redraws once per second.
 */
@Composable
fun LiveClock(
    face: ClockFace,
    now: ZonedDateTime,
    spec: GlassSpec = NexoraTheme.spec,
    use24Hour: Boolean = false,
    modifier: Modifier = Modifier,
) {
    when (face) {
        ClockFace.Halo -> HaloClock(now, spec, use24Hour, modifier)
        ClockFace.Meridian -> MeridianClock(now, spec, use24Hour, modifier)
        ClockFace.Sundial -> SundialClock(now, spec, modifier)
        ClockFace.Minimal -> MinimalClock(now, spec, use24Hour, modifier)
        ClockFace.Epoch -> EpochClock(now, spec, modifier)
    }
}

@Composable
private fun HaloClock(now: ZonedDateTime, spec: GlassSpec, use24: Boolean, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = minOf(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val secondsFraction = now.second / 60f
            val minuteFraction = (now.minute + secondsFraction) / 60f
            val hourFraction = ((now.hour % 12) + minuteFraction) / 12f

            // Ambient halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(spec.accentColor.copy(alpha = 0.20f), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = spec.material.rimLight.copy(alpha = 0.18f),
                radius = radius * 0.995f,
                center = center,
                style = Stroke(width = 1.4f),
            )

            // 60 ticks, with hour marks longer
            for (i in 0 until 60) {
                val angle = i * 6f - 90f
                val isHour = i % 5 == 0
                val inner = radius * if (isHour) 0.80f else 0.86f
                val outer = radius * 0.93f
                val rad = Math.toRadians(angle.toDouble())
                drawLine(
                    color = if (isHour) spec.textColor.copy(alpha = 0.55f) else spec.textColor.copy(alpha = 0.20f),
                    start = Offset(center.x + cos(rad).toFloat() * inner, center.y + sin(rad).toFloat() * inner),
                    end = Offset(center.x + cos(rad).toFloat() * outer, center.y + sin(rad).toFloat() * outer),
                    strokeWidth = if (isHour) 2.4f else 1.2f,
                    cap = StrokeCap.Round,
                )
            }

            // Arc progress for the current hour
            rotate(degrees = -90f, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(spec.accentColor, spec.accent.secondary)),
                    startAngle = 0f,
                    sweepAngle = hourFraction * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.885f, center.y - radius * 0.885f),
                    size = Size(radius * 1.77f, radius * 1.77f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round),
                )
            }

            // No clock hands on this face. A digital readout sits at the centre, and hands
            // crossing it was the single worst visual defect in the first render. The
            // sweep ring carries the "where are we in the hour" information instead, which
            // is also what makes this face readable at a glance.
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                now.format(if (use24) TIME_24 else TIME_12),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Light,
                ),
                color = spec.textColor,
            )
            if (!use24) {
                Text(
                    now.format(AM_PM),
                    style = MaterialTheme.typography.labelLarge,
                    color = spec.accentColor,
                )
            }
        }
    }
}

private fun DrawScope.drawClockHand(center: Offset, degrees: Float, length: Float, width: Float, color: Color) {
    val rad = Math.toRadians((degrees - 90f).toDouble())
    val end = Offset(center.x + cos(rad).toFloat() * length, center.y + sin(rad).toFloat() * length)
    drawLine(color = color, start = center, end = end, strokeWidth = width, cap = StrokeCap.Round)
}

@Composable
private fun MeridianClock(now: ZonedDateTime, spec: GlassSpec, use24: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            now.format(if (use24) TIME_24 else TIME_12),
            style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum", 
                fontWeight = FontWeight.Light,
                fontSize = 58.sp,
            ),
            color = spec.textColor,
        )
        if (!use24) {
            Text(now.format(AM_PM), style = MaterialTheme.typography.titleMedium, color = spec.accentColor)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth(0.8f).height(2.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    color = spec.textColor.copy(alpha = 0.12f),
                    start = Offset(0f, 1f),
                    end = Offset(size.width, 1f),
                    strokeWidth = 2f,
                )
                drawLine(
                    color = spec.accentColor,
                    start = Offset(0f, 1f),
                    end = Offset(size.width * (now.second / 60f), 1f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun SundialClock(now: ZonedDateTime, spec: GlassSpec, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.78f)
            val radius = minOf(size.width * 0.46f, size.height * 0.70f)

            // Horizon arc
            drawArc(
                color = spec.textColor.copy(alpha = 0.22f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 1.6f),
            )

            // Sun position: fraction of daylight, 6:00 -> 18:00
            val hours = now.hour + now.minute / 60f + now.second / 3600f
            val daylight = ((hours - 6f) / 12f).coerceIn(0f, 1f)
            val angle = (180f + daylight * 180f)
            val rad = Math.toRadians(angle.toDouble())
            val sun = Offset(center.x + cos(rad).toFloat() * radius, center.y + sin(rad).toFloat() * radius)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(spec.accentColor.copy(alpha = 0.55f), Color.Transparent),
                    center = sun,
                    radius = radius * 0.30f,
                ),
                radius = radius * 0.30f,
                center = sun,
            )
            drawCircle(color = spec.accent.tertiary, radius = radius * 0.055f, center = sun)

            // Hour rays
            for (h in 6..18 step 2) {
                val frac = (h - 6f) / 12f
                @Suppress("UNNECESSARY_SAFE_CALL")
                val a = Math.toRadians((180f + frac * 180f).toDouble())
                val inner = radius * 0.93f
                drawLine(
                    color = spec.textColor.copy(alpha = 0.25f),
                    start = Offset(center.x + cos(a).toFloat() * inner, center.y + sin(a).toFloat() * inner),
                    end = Offset(center.x + cos(a).toFloat() * radius, center.y + sin(a).toFloat() * radius),
                    strokeWidth = 1.2f,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                now.format(TIME_12),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
                color = spec.textColor,
            )
            Text(
                now.format(SECONDS) + "s",
                style = MaterialTheme.typography.bodySmall,
                color = spec.secondaryTextColor,
            )
        }
    }
}

@Composable
private fun MinimalClock(now: ZonedDateTime, spec: GlassSpec, use24: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            now.format(if (use24) TIME_24 else TIME_12),
            style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Thin),
            color = spec.textColor,
        )
        Text(
            now.format(DateTimeFormatter.ofPattern("EEEE d MMMM")),
            style = MaterialTheme.typography.bodyMedium,
            color = spec.secondaryTextColor,
        )
    }
}

@Composable
private fun EpochClock(now: ZonedDateTime, spec: GlassSpec, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            now.format(TIME_24),
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum", 
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp,
            ),
            color = spec.textColor,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "DAY ${now.dayOfYear} · WEEK ${now.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}",
            style = MaterialTheme.typography.labelSmall,
            color = spec.accentColor,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            style = MaterialTheme.typography.bodySmall,
            color = spec.secondaryTextColor,
        )
    }
}

/** Compact analog clock used in the widget preview and provider rows. */
@Composable
fun MiniAnalogClock(now: ZonedDateTime, spec: GlassSpec, modifier: Modifier = Modifier, size: Int = 44) {
    Canvas(modifier.size(size.dp)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = spec.textColor.copy(alpha = 0.10f), radius = radius, center = center)
        drawCircle(
            color = spec.material.rimLight.copy(alpha = 0.30f),
            radius = radius,
            center = center,
            style = Stroke(width = 1f),
        )
        val minuteFraction = (now.minute + now.second / 60f) / 60f
        val hourFraction = ((now.hour % 12) + minuteFraction) / 12f
        drawClockHand(center, hourFraction * 360f, radius * 0.5f, 2.6f, spec.textColor)
        drawClockHand(center, minuteFraction * 360f, radius * 0.76f, 1.8f, spec.accentColor)
        drawCircle(color = spec.accentColor, radius = radius * 0.07f, center = center)
    }
}
