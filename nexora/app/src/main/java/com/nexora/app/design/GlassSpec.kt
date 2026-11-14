package com.nexora.app.design

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/** Device-independent rendering tier. Chosen from measured device capability, not guesswork. */
enum class GlassQuality(val id: String, val displayName: String, val blurb: String) {
    Ultra(
        id = "ultra",
        displayName = "Ultra Glass",
        blurb = "Full refraction, specular streaks and live scrim blur on every pane.",
    ),
    Balanced(
        id = "balanced",
        displayName = "Balanced Glass",
        blurb = "Real blur on the hero panes, cached gradients elsewhere.",
    ),
    Efficient(
        id = "efficient",
        displayName = "Efficient Glass",
        blurb = "Cached single-layer gradients, no live blur — for low battery or heavy load.",
    );

    companion object {
        fun fromId(id: String?): GlassQuality? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Everything the renderer needs to draw one glass pane.
 *
 * This is a pure data class with no Android dependencies so the whole customisation
 * model is unit-testable, serialisable and previewable without an emulator.
 */
data class GlassSpec(
    val material: GlassMaterial = GlassMaterial.Crystal,
    /** 0f = fully opaque pane, 1f = fully see-through. */
    val transparency: Float = 0.42f,
    /** Requested blur in dp; the renderer clamps it to what the device can afford. */
    val blurDp: Float = 18f,
    val reflection: Float = 0.55f,
    val borderVisible: Boolean = true,
    val borderAlpha: Float = 0.5f,
    val cornerDp: Float = 26f,
    val shadowDepth: Float = 0.5f,
    val accent: AccentRamp = AccentRamp.Azure,
    val darkTheme: Boolean = true,
    val ambient: AmbientBackdrop = AmbientBackdrop.AuroraVeil,
    val typographyScale: Float = 1f,
    val density: Density = Density.Comfortable,
    val motionIntensity: Float = 0.8f,
    val motionSpeed: Float = 1f,
    val haptics: Boolean = true,
    val sound: Boolean = false,
    val reducedMotion: Boolean = false,
) {
    /** Effective surface alpha after the transparency slider. */
    val surfaceAlpha: Float get() = (1f - transparency).coerceIn(0.12f, 0.97f)

    /** Alpha used for the inner content scrim so text stays legible at every material. */
    val contentScrimAlpha: Float
        get() = (surfaceAlpha * 0.85f + if (darkTheme) 0.25f else 0.12f).coerceIn(0f, 0.96f)

    /** Motion is disabled when the user or the OS asks for reduced motion. */
    val reducedOrOff: Boolean get() = reducedMotion || motionIntensity <= 0.01f

    /** Duration scaling for all animations. Never returns 0 so actions stay visible. */
    fun durationMillis(baseMs: Int): Int {
        if (reducedOrOff) return 0
        val scaled = baseMs * (2f - motionSpeed.coerceIn(0.5f, 2f))
        return scaled.roundToInt().coerceIn(40, 4000)
    }

    /** Normalised 0..1 motion weight used by the animation helpers. */
    fun motionWeight(): Float = if (reducedOrOff) 0f else motionIntensity.coerceIn(0f, 1f)

    val textColor: Color
        get() = if (darkTheme) Color(0xFFF2F6FF) else Color(0xFF10141C)

    val secondaryTextColor: Color
        get() = if (darkTheme) Color(0xFFA9B4C8) else Color(0xFF4C5668)

    val accentColor: Color get() = accent.seed

    fun withMaterial(material: GlassMaterial) = copy(material = material)

    companion object {
        /** Hand-tuned starting points for each material so the sliders begin somewhere good. */
        fun presetFor(material: GlassMaterial, dark: Boolean = true): GlassSpec = when (material) {
            GlassMaterial.Crystal -> GlassSpec(
                material = material, darkTheme = dark, transparency = 0.38f, blurDp = 22f,
                reflection = 0.6f, accent = AccentRamp.Azure,
            )
            GlassMaterial.Aurora -> GlassSpec(
                material = material, darkTheme = true, transparency = 0.46f, blurDp = 26f,
                reflection = 0.7f, accent = AccentRamp.Verde, shadowDepth = 0.6f,
            )
            GlassMaterial.Obsidian -> GlassSpec(
                material = material, darkTheme = true, transparency = 0.22f, blurDp = 14f,
                reflection = 0.4f, accent = AccentRamp.Amber, shadowDepth = 0.8f,
                ambient = AmbientBackdrop.DeepSpace,
            )
            GlassMaterial.Frost -> GlassSpec(
                material = material, darkTheme = false, transparency = 0.3f, blurDp = 28f,
                reflection = 0.45f, accent = AccentRamp.Iris, shadowDepth = 0.35f,
                ambient = AmbientBackdrop.PaleGradient,
            )
            GlassMaterial.Holographic -> GlassSpec(
                material = material, darkTheme = true, transparency = 0.34f, blurDp = 24f,
                reflection = 0.85f, accent = AccentRamp.Magenta, shadowDepth = 0.55f,
                ambient = AmbientBackdrop.PrismFog,
            )
        }
    }
}
