package com.nexora.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.design.NexoraSpacing
import com.nexora.app.design.NexoraTheme
import com.nexora.app.design.glassPressPhysics
import com.nexora.app.design.rememberNexoraHaptics

/** A titled glass panel — the workhorse container across every screen. */
@Composable
fun GlassPanel(
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spec = NexoraTheme.spec
    GlassSurface(modifier = modifier.fillMaxWidth(), contentPadding = contentPadding) {
        Column(Modifier.fillMaxWidth()) {
            if (title != null || trailing != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        if (title != null) {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleMedium,
                                color = spec.textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.secondaryTextColor,
                            )
                        }
                    }
                    trailing?.invoke()
                }
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

/** Primary action: gradient glass pill with press physics and haptics. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    tonal: Boolean = false,
) {
    val spec = NexoraTheme.spec
    val haptics = rememberNexoraHaptics(spec)
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(spec.cornerDp.dp * 0.7f)

    val brush = if (tonal) {
        Brush.linearGradient(listOf(spec.accentColor.copy(alpha = 0.24f), spec.accent.secondary.copy(alpha = 0.16f)))
    } else {
        Brush.linearGradient(listOf(spec.accentColor, spec.accent.secondary))
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .glassPressPhysics(spec, interaction)
            .background(brush)
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                haptics.tap()
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 13.dp)
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = if (tonal) spec.textColor else Color(0xFF04121C), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (tonal) spec.textColor.copy(alpha = if (enabled) 1f else 0.4f)
                else Color(0xFF04121C).copy(alpha = if (enabled) 1f else 0.5f),
                maxLines = 1,
            )
        }
    }
}

/** Row with a label, optional supporting text and a trailing control. */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val spec = NexoraTheme.spec
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button) { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = spec.accentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = spec.textColor)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun NexoraSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") modifier: Modifier = Modifier,
) {
    val spec = NexoraTheme.spec
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color(0xFF04121C),
            checkedTrackColor = spec.accentColor,
            uncheckedThumbColor = spec.secondaryTextColor,
            uncheckedTrackColor = spec.textColor.copy(alpha = 0.12f),
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Slim animated slider with a value read-out. */
@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    display: (Float) -> String = { "%.0f%%".format(it * 100) },
) {
    val spec = NexoraTheme.spec
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = spec.textColor)
            Text(display(value), style = MaterialTheme.typography.bodySmall, color = spec.accentColor)
        }
        Spacer(Modifier.height(2.dp))
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = spec.accentColor,
                activeTrackColor = spec.accentColor,
                inactiveTrackColor = spec.textColor.copy(alpha = 0.14f),
            ),
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val spec = NexoraTheme.spec
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = spec.secondaryTextColor,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val spec = NexoraTheme.spec
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon?.let {
            Box(
                Modifier.size(56.dp).clip(CircleShape)
                    .background(spec.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(it, null, tint = spec.accentColor, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = spec.textColor)
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = spec.secondaryTextColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun PillTag(text: String, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    val spec = NexoraTheme.spec
    Box(
        modifier
            .clip(CircleShape)
            .background(
                if (highlighted) spec.accentColor.copy(alpha = 0.22f)
                else spec.textColor.copy(alpha = 0.08f)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) spec.accentColor else spec.secondaryTextColor,
        )
    }
}

/** Small key/value stat used on Insights and the Alarm Health Centre. */
@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    val spec = NexoraTheme.spec
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            color = if (accent) spec.accentColor else spec.textColor,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
    }
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier) {
    val spec = NexoraTheme.spec
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(spec.textColor.copy(alpha = 0.08f)),
    )
}
