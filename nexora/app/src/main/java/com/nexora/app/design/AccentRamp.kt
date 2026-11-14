package com.nexora.app.design

import androidx.compose.ui.graphics.Color

/**
 * Accent ramps. Each ramp supplies a seed plus a triadic pair used for gradients and
 * charts, so an accent change recolours the entire product coherently.
 */
enum class AccentRamp(
    val id: String,
    val displayName: String,
    val seed: Color,
    val secondary: Color,
    val tertiary: Color,
) {
    Azure("azure", "Azure", Color(0xFF5AC8FF), Color(0xFF4C6FFF), Color(0xFF9BE7FF)),
    Verde("verde", "Verde", Color(0xFF3CE8B0), Color(0xFF25B3FF), Color(0xFFB8FFDD)),
    Iris("iris", "Iris", Color(0xFF9A7BFF), Color(0xFF5AE0F0), Color(0xFFE0D2FF)),
    Amber("amber", "Amber", Color(0xFFFFB55A), Color(0xFFFF7A59), Color(0xFFFFE3B0)),
    Magenta("magenta", "Magenta", Color(0xFFFF62C8), Color(0xFF9A6BFF), Color(0xFFFFC7EC)),
    Jade("jade", "Jade", Color(0xFF22D3A5), Color(0xFF7BE495), Color(0xFFCBFFE9)),
    Crimson("crimson", "Crimson", Color(0xFFFF5C73), Color(0xFFFF9A6C), Color(0xFFFFD1D8)),
    Platinum("platinum", "Platinum", Color(0xFFBFC9D9), Color(0xFF8FA0B8), Color(0xFFF2F5FA));

    companion object {
        fun fromId(id: String?): AccentRamp = entries.firstOrNull { it.id == id } ?: Azure
    }
}
