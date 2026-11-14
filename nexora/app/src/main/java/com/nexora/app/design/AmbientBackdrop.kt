package com.nexora.app.design

import androidx.compose.ui.graphics.Color

/**
 * Ambient backdrops sit *behind* the glass stack. They are what the transparency
 * actually reveals, so they matter as much as the pane itself.
 */
enum class AmbientBackdrop(val id: String, val displayName: String, val colors: List<Color>) {
    AuroraVeil(
        "aurora_veil",
        "Aurora Veil",
        listOf(Color(0xFF07122B), Color(0xFF122A56), Color(0xFF1D5C6B), Color(0xFF0A1024)),
    ),
    DeepSpace(
        "deep_space",
        "Deep Space",
        listOf(Color(0xFF04060B), Color(0xFF0C1120), Color(0xFF161C2E), Color(0xFF000000)),
    ),
    PaleGradient(
        "pale_gradient",
        "Pale Gradient",
        listOf(Color(0xFFF6F9FF), Color(0xFFDDE7F7), Color(0xFFF0E9FF), Color(0xFFFFFFFF)),
    ),
    PrismFog(
        "prism_fog",
        "Prism Fog",
        listOf(Color(0xFF22103A), Color(0xFF3A1458), Color(0xFF10405E), Color(0xFF0B0A1C)),
    ),
    EmberNight(
        "ember_night",
        "Ember Night",
        listOf(Color(0xFF1A0B08), Color(0xFF3A140C), Color(0xFF1C1B2E), Color(0xFF06050A)),
    ),
    MintDawn(
        "mint_dawn",
        "Mint Dawn",
        listOf(Color(0xFFEFFBF6), Color(0xFFD3F2EE), Color(0xFFF4F0FF), Color(0xFFFFFFFF)),
    );

    val isLight: Boolean get() = this == PaleGradient || this == MintDawn

    companion object {
        fun fromId(id: String?): AmbientBackdrop = entries.firstOrNull { it.id == id } ?: AuroraVeil
    }
}

/** Interface density: drives paddings, list heights and how much is on screen at once. */
enum class Density(val id: String, val displayName: String, val scale: Float) {
    Compact("compact", "Compact", 0.86f),
    Comfortable("comfortable", "Comfortable", 1f),
    Airy("airy", "Airy", 1.16f);

    companion object {
        fun fromId(id: String?): Density = entries.firstOrNull { it.id == id } ?: Comfortable
    }
}
