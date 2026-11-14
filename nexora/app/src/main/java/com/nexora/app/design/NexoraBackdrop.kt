package com.nexora.app.design

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Ambient backdrop behind the glass stack.
 *
 * Motion policy: the drifting orbs animate **only** when the interface is visible and
 * motion is enabled. When motion is off (or the user picks Efficient Glass) the backdrop
 * is a single static gradient — zero frame cost. We never loop animations when nothing
 * is on screen, and never animate a full-screen blur.
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
    } else 0f

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawAmbient(backdrop, spec, phase, drift)
        }
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
    val sway = (phase - 0.5f) * 0.16f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(w * (0.28f + sway), h * 0.22f),
            radius = w * 0.72f,
        ),
        radius = w * 0.72f,
        center = Offset(w * (0.28f + sway), h * 0.22f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(secondary.copy(alpha = 0.24f), Color.Transparent),
            center = Offset(w * (0.78f - sway), h * 0.62f),
            radius = w * 0.62f,
        ),
        radius = w * 0.62f,
        center = Offset(w * (0.78f - sway), h * 0.62f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(spec.accent.tertiary.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(w * (0.5f + sway * 0.6f), h * 0.92f),
            radius = w * 0.5f,
        ),
        radius = w * 0.5f,
        center = Offset(w * (0.5f + sway * 0.6f), h * 0.92f),
    )
}

/**
 * Backdrop blur for sheets and dialogs only.
 *
 * A full-screen continuously-redrawn blur is exactly the thing that makes mid-range
 * phones drop frames. So: one static scrim layer, blurred once per composition when the
 * device supports it (API 31+), and a plain translucent scrim otherwise.
 */
@Composable
fun blurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun Modifier.staticBlur(radiusDp: Float, spec: GlassSpec = NexoraTheme.spec): Modifier {
    if (!blurSupported() || radiusDp <= 0.5f || spec.reducedOrOff) return this
    val radius = androidx.compose.ui.platform.LocalDensity.current.run { radiusDp.dp.toPx() }
    val effect = remember(radius) {
        androidx.compose.ui.graphics.BlurEffect(radius, radius, androidx.compose.ui.graphics.TileMode.Clamp)
            .asComposeRenderEffect()
    }
    return this.graphicsLayer { renderEffect = effect }
}
