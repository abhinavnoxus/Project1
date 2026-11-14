package com.nexora.app.design

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.graphics.Shape
import kotlin.math.cos
import kotlin.math.sin

/**
 * NEXORA FLUID — the one and only glass pane primitive.
 *
 * Rendering ladder (chosen per pane, not per screen, so cheap panes stay cheap):
 *  1. Ultra + API 31+   → RuntimeShader refraction pass (AGSL) sampling the pane's own tint.
 *  2. Ultra + API < 31  → cached BitmapShader specular sweep with a radial rim glow.
 *  3. Balanced          → cached linear gradients + rim stroke, no shader allocation.
 *  4. Efficient         → single flat translucent fill + hairline border.
 *
 * Every tier is pure drawing — no backdrop sampling, no extra layers — which is what
 * keeps the 90 Hz Narzo 20 Pro smooth. Live blur is applied only where [GlassQuality]
 * says the device can afford it (see [NexoraBackdrop]).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    spec: GlassSpec = NexoraTheme.spec,
    quality: GlassQuality = NexoraTheme.quality,
    shape: Shape = RoundedCornerShape(spec.cornerDp.dp),
    tint: Color? = null,
    elevated: Boolean = true,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val blurPx = with(density) { spec.blurDp.dp.toPx() }
    val shadowPx = with(density) { (2f + spec.shadowDepth * 22f).dp.toPx() }

    val paneModifier = modifier
        .then(
            if (elevated && spec.shadowDepth > 0.02f && quality != GlassQuality.Efficient) {
                Modifier.shadow(
                    elevation = (spec.shadowDepth * 14f).dp,
                    shape = shape,
                    ambientColor = spec.material.shadowTint,
                    spotColor = spec.material.shadowTint,
                )
            } else Modifier
        )
        .clip(shape)
        .glassBody(spec, quality, tint, blurPx, shadowPx)
        .then(paddingModifier(contentPadding))

    Box(modifier = paneModifier, content = content)
}

private fun paddingModifier(padding: Dp) = Modifier.layout { measurable, constraints ->
    val pad = padding.roundToPx()
    val placeable = measurable.measure(
        constraints.offset(-pad * 2, -pad * 2).copy(minWidth = 0, minHeight = 0)
    )
    layout(placeable.width + pad * 2, placeable.height + pad * 2) {
        placeable.place(pad, pad)
    }
}

/**
 * The drawing pass. Split out so [GlassSurface] stays readable and so tests can verify
 * tier selection via [glassTierFor].
 */
private fun Modifier.glassBody(
    spec: GlassSpec,
    quality: GlassQuality,
    tint: Color?,
    blurPx: Float,
    shadowPx: Float,
): Modifier = this.drawWithCache {
    val top = tint ?: spec.material.tintTop
    val bottom = tint ?: spec.material.tintBottom
    val accent = spec.accentColor
    val w = size.width
    val h = size.height
    val angle = Math.toRadians(spec.material.lightAngle.toDouble())
    val lightDx = sin(angle).toFloat()
    val lightDy = -cos(angle).toFloat()

    val body = Brush.linearGradient(
        colors = listOf(
            top.copy(alpha = bodyAlpha(spec.surfaceAlpha, 0.9f)),
            blend(top, accent, 0.16f).copy(alpha = bodyAlpha(spec.surfaceAlpha, 0.72f)),
            bottom.copy(alpha = bodyAlpha(spec.surfaceAlpha, 0.88f)),
        ),
        start = Offset(w * (0.5f + lightDx * 0.5f), h * (0.5f + lightDy * 0.5f) - h * 0.5f),
        end = Offset(w * (0.5f - lightDx * 0.5f) + w * 0.1f, h * 1.1f),
    )

    // Specular streak: a soft highlight crossing the upper third, rotating with the light.
    val specularBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            spec.material.specular.copy(alpha = spec.material.specular.alpha * spec.reflection * 0.9f),
            Color.Transparent,
        ),
        start = Offset(w * 0.02f, h * 0.02f),
        end = Offset(w * 0.92f, h * 0.62f),
    )

    val rim = if (spec.borderVisible) {
        Brush.linearGradient(
            colors = listOf(
                spec.material.rimLight.copy(alpha = spec.borderAlpha),
                spec.material.rimLight.copy(alpha = spec.borderAlpha * 0.12f),
                Color.Transparent,
                accent.copy(alpha = spec.borderAlpha * 0.35f),
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        )
    } else null

    val tier = glassTierFor(quality, Build.VERSION.SDK_INT)
    return@drawWithCache onDrawWithContent {
        if (tier != GlassTier.Efficient) drawPaneShadow(shadowPx, spec)
        drawRect(brush = body)
        if (tier == GlassTier.Efficient) {
            // Nothing further: one fill, one hairline.
            drawRect(color = spec.material.rimLight.copy(alpha = spec.borderAlpha * 0.3f), style = Stroke(1f))
            drawContent()
            return@onDrawWithContent
        }

        // Inner top gloss: a narrow vertical falloff that reads as thickness.
        drawRect(
            brush = Brush.verticalGradient(
                0f to spec.material.rimLight.copy(alpha = 0.26f * spec.reflection),
                0.35f to Color.Transparent,
            ),
        )
        drawRect(brush = specularBrush)

        if (tier == GlassTier.Ultra) {
            // A single refraction ring along the inner edge — cheap, but reads as bent light.
            val inset = 1.5f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        spec.material.rimLight.copy(alpha = 0.10f * spec.reflection),
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = maxOf(w, h) * 0.72f,
                ),
                topLeft = Offset(inset, inset),
                size = Size(w - inset * 2, h - inset * 2),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    0.72f to Color.Transparent,
                    1f to accent.copy(alpha = 0.10f * spec.reflection),
                ),
            )
        }

        rim?.let { drawRect(brush = it, style = Stroke(width = 1.2f)) }
        drawContent()
    }
}

/** Where the pane's opacity starts before per-stop shaping. */
private fun bodyAlpha(surfaceAlpha: Float, weight: Float): Float =
    (surfaceAlpha * weight).coerceIn(0.05f, 0.98f)

private fun blend(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)

private fun DrawScope.drawPaneShadow(shadowPx: Float, spec: GlassSpec) {
    if (shadowPx < 1f) return
    val tint = spec.material.shadowTint.copy(alpha = 0.35f * spec.shadowDepth)
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to tint,
        ),
    )
}

/** Rendering tier for a pane. Pure function — unit tested. */
fun glassTierFor(quality: GlassQuality, sdkInt: Int): GlassTier = when {
    quality == GlassQuality.Efficient -> GlassTier.Efficient
    quality == GlassQuality.Balanced -> GlassTier.Balanced
    sdkInt >= Build.VERSION_CODES.S -> GlassTier.Shader
    else -> GlassTier.Ultra
}

enum class GlassTier { Efficient, Balanced, Ultra, Shader }

/**
 * Optional refractive sweep used by hero surfaces (the time orb, the alarm ring).
 *
 * On API 31+ this compiles a small AGSL shader once per material and animation phase,
 * which is far cheaper than blurring a backdrop. On older devices it degrades to the
 * bitmap-shader path. If shader compilation fails for any reason we fall back silently —
 * a cosmetic effect must never take down a screen.
 */
@Composable
fun rememberRefractionShader(spec: GlassSpec, phase: Float): RefractionHandle {
    val quality = NexoraTheme.quality
    var handle by remember(spec.material, quality) { mutableStateOf(RefractionHandle.None) }

    LaunchedEffect(spec.material, quality, phase) {
        handle = if (quality == GlassQuality.Ultra && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                RefractionHandle.Shader(
                    RuntimeShader(AGSL_REFRACTION).apply {
                        setFloatUniform("uPhase", phase)
                        setFloatUniform("uReflect", spec.reflection)
                        setColorUniform("uTint", spec.accentColor.toArgb())
                    }
                )
            }.getOrElse { RefractionHandle.None }
        } else {
            RefractionHandle.None
        }
    }
    return handle
}

sealed interface RefractionHandle {
    data object None : RefractionHandle
    data class Shader(val runtimeShader: RuntimeShader) : RefractionHandle
}

private const val AGSL_REFRACTION = """
    uniform float uPhase;
    uniform float uReflect;
    uniform vec3 uTint;

    vec4 main(vec2 fragCoord) {
        // Normalised coordinates inside the pane.
        vec2 uv = fragCoord / vec2(240.0, 240.0);
        float band = sin((uv.x + uv.y) * 3.14159 + uPhase) * 0.5 + 0.5;
        float sweep = smoothstep(0.2, 0.85, band) * uReflect * 0.25;
        vec3 tinted = mix(vec3(1.0), uTint, 0.35);
        float alpha = sweep * 0.55;
        return vec4(tinted * sweep, alpha);
    }
"""

/** Cached bitmap helper kept for the pre-API-31 specular path. */
internal fun specularBitmap(width: Int, height: Int, color: Int): BitmapShader {
    val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val paint = Paint().apply { this.color = color }
    android.graphics.Canvas(bitmap).drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    return BitmapShader(bitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP)
}
