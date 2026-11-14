package com.nexora.app.design

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Ambient backdrop behind the glass stack.
 *
 * Motion policy: the drifting orbs animate **only** when motion is enabled and the
 * quality tier can afford it. With motion off (or Efficient Glass) the backdrop is one
 * static gradient — zero per-frame cost. There is no full-screen continually-redrawn
 * blur anywhere in NEXORA: the glass panes draw their own refraction, which is orders of
 * magnitude cheaper than sampling the backdrop every frame.
 */
@Composable
fun NexoraBackdrop(
    spec: GlassSpec = NexoraTheme.spec,
    quality: GlassQuality = NexoraTheme.quality,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = spec.ambient
    val drift = !spec.reducedOrOff && animate && quality != GlassQuality.Efficient

    val phase = if (drift) {
        val transition = rememberInfiniteTransition(label = "backdrop")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (28_000 / spec.motionSpeed.coerceAtLeast(0.5f)).toInt(),
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "phase",
        ).value
    } else {
        0f
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { drawAmbient(backdrop, spec, phase, drift) }
        content()
    }
}

private fun DrawScope.drawAmbient(
    backdrop: AmbientBackdrop,
    spec: GlassSpec,
    phase: Float,
    drift: Boolean,
) {
    val w = size.width
    val h = size.height
    drawRect(
        brush = Brush.linearGradient(
            colors = backdrop.colors,
            start = Offset(0f, 0f),
            end = Offset(w * 0.6f, h),
        ),
    )
    if (!drift) return

    val accent = spec.accentColor
    val secondary = spec.accent.secondary
    val tertiary = spec.accent.tertiary
    val sway = (phase - 0.5f) * 0.16f

    val orbs = listOf(
        Triple(Offset(w * (0.28f + sway), h * 0.22f), w * 0.72f, accent.copy(alpha = 0.30f)),
        Triple(Offset(w * (0.78f - sway), h * 0.62f), w * 0.62f, secondary.copy(alpha = 0.24f)),
        Triple(Offset(w * (0.5f + sway * 0.6f), h * 0.92f), w * 0.5f, tertiary.copy(alpha = 0.14f)),
    )
    orbs.forEach { (center, radius, color) ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

/** True when this platform can blur a layer at all (API 31+). */
fun blurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * One-shot blur for sheets and dialogs.
 *
 * Applied to a sheet's own *already-rendered* content, never to a live backdrop, and
 * skipped entirely on pre-API-31 devices where it degrades to the translucent scrim the
 * caller draws underneath.
 */
@Composable
fun Modifier.staticBlur(radiusDp: Float, spec: GlassSpec = NexoraTheme.spec): Modifier {
    if (!blurSupported() || radiusDp <= 0.5f || spec.reducedOrOff) return this
    val radiusPx = with(LocalDensity.current) { radiusDp.dp.toPx() }
    val effect = androidx.compose.ui.graphics.BlurEffect(
        radiusX = radiusPx,
        radiusY = radiusPx,
        edgeTreatment = androidx.compose.ui.graphics.TileMode.Clamp,
    ).asComposeRenderEffect()
    return this.graphicsLayer { renderEffect = effect }
}
