package com.nexora.app.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * NEXORA MOTION ENGINE.
 *
 * One place that decides how everything moves. Three rules it enforces:
 *  1. **Motion never gates a critical action.** Alarm stop/dismiss paths call the domain
 *     layer directly and never await an animation (see AlarmController).
 *  2. **Every duration comes from the user's spec** (intensity + speed + reduced-motion),
 *     so "Still" really is still — animations collapse to ~0 ms rather than slowing down.
 *  3. **Everything is interruptible** — we animate properties with `Animatable`/`animate*AsState`
 *     driven by state, never with `LaunchedEffect { delay; mutate }` queues.
 */
object NexoraMotion {

    /** The signature "liquid" easing: slow in, quick middle, long settle. */
    val Liquid: Easing = CubicBezierEasing(0.22f, 0.61f, 0.16f, 1f)

    /** Enter easing for panels morphing in. */
    val Morph: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    fun <T> liquidSpec(spec: GlassSpec, baseMs: Int = 380): androidx.compose.animation.core.TweenSpec<T> =
        tween(durationMillis = spec.durationMillis(baseMs), easing = Liquid)

    fun <T> panelSpring(spec: GlassSpec): androidx.compose.animation.core.FiniteAnimationSpec<T> =
        if (spec.reducedOrOff) {
            tween(0)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy * (1f + spec.motionIntensity * 0.3f),
                stiffness = Spring.StiffnessMediumLow,
            )
        }
}

/**
 * Press physics: a glass button depresses slightly and lifts back with the liquid curve.
 * Scales with motion intensity so "Calm" feels gentler and "Still" doesn't move at all.
 */
fun Modifier.glassPressPhysics(
    spec: GlassSpec,
    interactionSource: InteractionSource? = null,
    pressedScale: Float = 0.965f,
    liftOnPress: Boolean = true,
): Modifier = composed {
    val source = interactionSource ?: remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val weight = spec.motionWeight()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1f - (1f - pressedScale) * weight else 1f,
        animationSpec = if (spec.reducedOrOff) tween(0) else spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press-scale",
    )
    val lift by animateFloatAsState(
        targetValue = if (pressed && liftOnPress) -2f * weight else 0f,
        animationSpec = NexoraMotion.liquidSpec(spec, 220),
        label = "press-lift",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = lift
    }
}

/** Haptic helper that honours the user's preference. */
@Composable
fun rememberNexoraHaptics(spec: GlassSpec = NexoraTheme.spec): NexoraHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(spec.haptics) {
        NexoraHaptics { type ->
            if (spec.haptics) {
                runCatching { feedback.performHapticFeedback(type) }
            }
        }
    }
}

fun interface NexoraHaptics {
    fun perform(type: HapticFeedbackType)

    fun tap() = perform(HapticFeedbackType.TextHandleMove)
    fun confirm() = perform(HapticFeedbackType.LongPress)
}

/** Drives a value in from 0..1 once, for entrance choreography. */
@Composable
fun rememberEntrance(spec: GlassSpec, baseMs: Int = 420): State<Float> {
    val anim = remember { Animatable(if (spec.reducedOrOff) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(spec.material, spec.motionIntensity) {
        anim.animateTo(1f, NexoraMotion.liquidSpec(spec, baseMs))
    }
    return anim.asState()
}

/** Standard staggered reveal used by every list in NEXORA. */
fun Modifier.staggeredReveal(progress: Float, index: Int, spec: GlassSpec): Modifier {
    if (spec.reducedOrOff) return this
    val stagger = 0.07f * spec.motionIntensity
    val start = (index * stagger).coerceAtMost(0.5f)
    val local = ((progress - start) / (1f - start)).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(local)
    return this.graphicsLayer {
        alpha = eased
        translationY = (1f - eased) * 26f * spec.motionIntensity
        scaleX = 0.985f + 0.015f * eased
        scaleY = 0.985f + 0.015f * eased
    }
}
