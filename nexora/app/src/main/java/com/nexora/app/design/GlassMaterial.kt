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
    /**
     * Tint used when the dark theme is active.
     *
     * A glass recipe cannot be theme-agnostic: a pale tint composited over a dark
     * backdrop produces milk, not glass. Dark-theme panes therefore carry their own much
     * darker tint and rely on the specular streak and rim for their glass read.
     */
    val darkTintTop: Color,
    val darkTintBottom: Color,
) {
    /** Body tint for the current theme. */
    fun top(dark: Boolean): Color = if (dark) darkTintTop else tintTop
    fun bottom(dark: Boolean): Color = if (dark) darkTintBottom else tintBottom

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
        darkTintTop = Color(0xFF2A3A52),
        darkTintBottom = Color(0xFF0D1622),
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
        darkTintTop = Color(0xFF173B44),
        darkTintBottom = Color(0xFF101A3A),
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
        darkTintTop = Color(0xFF16181E),
        darkTintBottom = Color(0xFF05060A),
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
        darkTintTop = Color(0xFF2C3644),
        darkTintBottom = Color(0xFF131922),
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
        darkTintTop = Color(0xFF3A2456),
        darkTintBottom = Color(0xFF161033),
    );

    /** Whether text drawn on this material should be light. */
    val prefersLightText: Boolean
        get() = this == Obsidian || this == Holographic

    companion object {
        fun fromId(id: String?): GlassMaterial =
            entries.firstOrNull { it.id == id } ?: Crystal
    }
}
