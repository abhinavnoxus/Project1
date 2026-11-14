package com.nexora.app.design

import androidx.compose.ui.graphics.Color

/**
 * One of the five NEXORA FLUID material styles.
 *
 * A material is a *recipe*: a light-direction, a set of gradient stops, a specular
 * highlight and a shadow tint. The user's numeric sliders (transparency, blur,
 * reflection, border, radius, shadow) are applied on top of the recipe by
 * [GlassSurface], so the same material reads correctly at any opacity the user picks.
 */
enum class GlassMaterial(
    val id: String,
    val displayName: String,
    val tagline: String,
    /** Top-left surface tint of the glass body. */
    val tintTop: Color,
    /** Bottom-right surface tint of the glass body. */
    val tintBottom: Color,
    /** Colour of the 1px rim light on the top/left edges. */
    val rimLight: Color,
    /** Colour used for the specular streak across the upper third. */
    val specular: Color,
    /** Ambient shadow colour beneath the pane. */
    val shadowTint: Color,
    /** Direction of the light source in degrees, measured clockwise from up. */
    val lightAngle: Float,
) {
    Crystal(
        id = "crystal",
        displayName = "Crystal",
        tagline = "Clear, cool, precise",
        tintTop = Color(0xFFEAF2FF),
        tintBottom = Color(0xFFC8D8EE),
        rimLight = Color(0xFFFFFFFF),
        specular = Color(0x99FFFFFF),
        shadowTint = Color(0xFF0B1524),
        lightAngle = 315f,
    ),
    Aurora(
        id = "aurora",
        displayName = "Aurora",
        tagline = "Northern light, flowing",
        tintTop = Color(0xFFB8F2E6),
        tintBottom = Color(0xFF7C8CFF),
        rimLight = Color(0xFFE9FFFA),
        specular = Color(0x77D8FFF4),
        shadowTint = Color(0xFF06122A),
        lightAngle = 340f,
    ),
    Obsidian(
        id = "obsidian",
        displayName = "Obsidian",
        tagline = "Volcanic black, deep gloss",
        tintTop = Color(0xFF23262E),
        tintBottom = Color(0xFF0B0C10),
        rimLight = Color(0xFF9AA4B8),
        specular = Color(0x40B9C4DA),
        shadowTint = Color(0xFF000000),
        lightAngle = 300f,
    ),
    Frost(
        id = "frost",
        displayName = "Frost",
        tagline = "Soft, diffuse, calm",
        tintTop = Color(0xFFF7FAFF),
        tintBottom = Color(0xFFDCE6F5),
        rimLight = Color(0xFFFFFFFF),
        specular = Color(0x66FFFFFF),
        shadowTint = Color(0xFF22304A),
        lightAngle = 320f,
    ),
    Holographic(
        id = "holographic",
        displayName = "Holographic",
        tagline = "Prismatic, shifting",
        tintTop = Color(0xFFCFA6FF),
        tintBottom = Color(0xFF6FE3FF),
        rimLight = Color(0xFFFFF2A8),
        specular = Color(0x88FFD1F5),
        shadowTint = Color(0xFF120A2A),
        lightAngle = 330f,
    );

    /** Whether text drawn on this material should be light. */
    val prefersLightText: Boolean
        get() = this == Obsidian || this == Holographic

    companion object {
        fun fromId(id: String?): GlassMaterial =
            entries.firstOrNull { it.id == id } ?: Crystal
    }
}
