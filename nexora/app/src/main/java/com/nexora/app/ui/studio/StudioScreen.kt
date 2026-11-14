package com.nexora.app.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.app.design.AccentRamp
import com.nexora.app.design.AmbientBackdrop
import com.nexora.app.design.ClockFace
import com.nexora.app.design.Density
import com.nexora.app.design.GlassMaterial
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSpecData
import com.nexora.app.design.GlassSurface
import com.nexora.app.design.HomeSection
import com.nexora.app.design.InterfaceProfile
import com.nexora.app.design.LayoutConfig
import com.nexora.app.design.MotionProfile
import com.nexora.app.design.NavStyle
import com.nexora.app.ui.common.GlassButton
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.LabeledSlider
import com.nexora.app.ui.common.NexoraSwitch
import com.nexora.app.ui.common.PillTag
import com.nexora.app.ui.common.SectionHeader
import com.nexora.app.ui.common.SettingsRow
import java.time.ZonedDateTime

/**
 * NEXORA STUDIO.
 *
 * Everything here mutates the *active profile* through the ViewModel, so the live
 * preview above the controls is the real renderer with real state — not a mock. Changes
 * are persisted by the same repository the app boots from.
 */
@Composable
fun StudioScreen(
    profiles: List<InterfaceProfile>,
    active: InterfaceProfile?,
    spec: GlassSpec,
    onUpdateSpec: ((GlassSpecData) -> GlassSpecData) -> Unit,
    onUpdateLayout: ((LayoutConfig) -> LayoutConfig) -> Unit,
    onActivate: (String) -> Unit,
    onDuplicate: (InterfaceProfile, String) -> Unit,
    onDelete: (String) -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    var tab by remember { mutableStateOf(StudioTab.Material) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("NEXORA Studio", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                    Text(
                        active?.name ?: "No profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.accentColor,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                GlassButton("Reset", onReset, tonal = true, icon = Icons.Rounded.RestartAlt)
            }
        }

        item { LivePreview(spec) }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StudioTab.entries) { candidate ->
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(
                                if (tab == candidate) spec.accentColor.copy(alpha = 0.22f)
                                else spec.textColor.copy(alpha = 0.06f)
                            )
                            .clickable { tab = candidate }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            candidate.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (tab == candidate) spec.accentColor else spec.secondaryTextColor,
                        )
                    }
                }
            }
        }

        when (tab) {
            StudioTab.Material -> {
                item {
                    GlassPanel(title = "Glass material", subtitle = "Five original recipes") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassMaterial.entries.forEach { material ->
                                MaterialSwatch(material, spec.material == material, spec) {
                                    onUpdateSpec { it.copy(materialId = material.id) }
                                }
                            }
                        }
                    }
                }
                item {
                    GlassPanel(title = "Surface") {
                        LabeledSlider("Transparency", spec.transparency, 0f..0.9f, { value ->
                            onUpdateSpec { it.copy(transparency = value) }
                        }, display = { "%.0f%%".format(it * 100) })
                        LabeledSlider("Blur", spec.blurDp, 0f..40f, { value ->
                            onUpdateSpec { it.copy(blurDp = value) }
                        }, display = { "%.0f dp".format(it) })
                        LabeledSlider("Reflection", spec.reflection, 0f..1f, { value ->
                            onUpdateSpec { it.copy(reflection = value) }
                        })
                        LabeledSlider("Border", spec.borderAlpha, 0f..1f, { value ->
                            onUpdateSpec { it.copy(borderAlpha = value) }
                        })
                        LabeledSlider("Corner radius", spec.cornerDp, 0f..48f, { value ->
                            onUpdateSpec { it.copy(cornerDp = value) }
                        }, display = { "%.0f dp".format(it) })
                        LabeledSlider("Shadow depth", spec.shadowDepth, 0f..1f, { value ->
                            onUpdateSpec { it.copy(shadowDepth = value) }
                        })
                        SettingsRow(
                            "Show glass border",
                            subtitle = "Hairline rim light on every pane",
                            trailing = {
                                NexoraSwitch(spec.borderVisible) { on ->
                                    onUpdateSpec { it.copy(borderVisible = on) }
                                }
                            },
                        )
                    }
                }
                item {
                    GlassPanel(title = "Accent") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(AccentRamp.entries) { ramp ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    listOf(ramp.seed, ramp.secondary)
                                                )
                                            )
                                            .clickable { onUpdateSpec { it.copy(accentId = ramp.id) } },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (spec.accent == ramp) {
                                            Icon(Icons.Rounded.Check, null, tint = androidx.compose.ui.graphics.Color(0xFF04121C), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(ramp.displayName, style = MaterialTheme.typography.labelSmall, color = spec.secondaryTextColor)
                                }
                            }
                        }
                    }
                }
                item {
                    GlassPanel(title = "Backdrop", subtitle = "What the glass reveals") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AmbientBackdrop.entries.forEach { backdrop ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onUpdateSpec { it.copy(ambientId = backdrop.id) } }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(backdrop.colors)
                                            ),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(backdrop.displayName, style = MaterialTheme.typography.bodyMedium, color = spec.textColor, modifier = Modifier.weight(1f))
                                    if (spec.ambient == backdrop) Icon(Icons.Rounded.Check, null, tint = spec.accentColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SettingsRow(
                            "Dark theme",
                            subtitle = "Switch the whole interface between light and dark",
                            trailing = {
                                NexoraSwitch(spec.darkTheme) { on -> onUpdateSpec { it.copy(darkTheme = on) } }
                            },
                        )
                    }
                }
            }

            StudioTab.Layout -> {
                item {
                    GlassPanel(title = "Home sections", subtitle = "Reorder what the command centre shows") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val order = active?.layout?.homeSections ?: LayoutConfig().homeSections
                            order.forEachIndexed { index, id ->
                                val section = HomeSection.fromId(id) ?: return@forEachIndexed
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(spec.textColor.copy(alpha = 0.05f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = spec.secondaryTextColor,
                                        modifier = Modifier.width(22.dp),
                                    )
                                    Text(section.title, style = MaterialTheme.typography.bodyMedium, color = spec.textColor, modifier = Modifier.weight(1f))
                                    if (!section.locked) {
                                        Icon(
                                            Icons.Rounded.ArrowUpward, "Move up", tint = spec.secondaryTextColor,
                                            modifier = Modifier.size(18.dp).clickable {
                                                if (index > 0) onUpdateLayout { layout ->
                                                    val list = layout.homeSections.toMutableList()
                                                    list.add(index - 1, list.removeAt(index))
                                                    layout.copy(homeSections = list)
                                                }
                                            },
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Rounded.ArrowDownward, "Move down", tint = spec.secondaryTextColor,
                                            modifier = Modifier.size(18.dp).clickable {
                                                if (index < order.lastIndex) onUpdateLayout { layout ->
                                                    val list = layout.homeSections.toMutableList()
                                                    list.add(index + 1, list.removeAt(index))
                                                    layout.copy(homeSections = list)
                                                }
                                            },
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Rounded.Delete, "Hide section", tint = spec.secondaryTextColor,
                                            modifier = Modifier.size(18.dp).clickable {
                                                onUpdateLayout { it.copy(homeSections = it.homeSections - id) }
                                            },
                                        )
                                    } else {
                                        PillTag("Pinned")
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        val hidden = HomeSection.entries.filter { it.id !in order }
                        if (hidden.isNotEmpty()) {
                            SectionHeader("Hidden")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                hidden.forEach { section ->
                                    Box(
                                        Modifier.clip(CircleShape).background(spec.accentColor.copy(alpha = 0.14f))
                                            .clickable { onUpdateLayout { it.copy(homeSections = it.homeSections + section.id) } }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Text("+ ${section.title}", style = MaterialTheme.typography.labelSmall, color = spec.accentColor)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    GlassPanel(title = "Clock face") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ClockFace.entries) { face ->
                                Box(
                                    Modifier.clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (active?.layout?.clockFace == face.id) spec.accentColor.copy(alpha = 0.2f)
                                            else spec.textColor.copy(alpha = 0.06f)
                                        )
                                        .clickable { onUpdateLayout { it.copy(clockFace = face.id) } }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        face.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active?.layout?.clockFace == face.id) spec.accentColor else spec.secondaryTextColor,
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    GlassPanel(title = "Navigation") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NavStyle.entries.forEach { style ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onUpdateLayout { it.copy(navigationStyle = style.id) } }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(style.displayName, style = MaterialTheme.typography.bodyMedium, color = spec.textColor, modifier = Modifier.weight(1f))
                                    if (active?.layout?.navigationStyle == style.id) Icon(Icons.Rounded.Check, null, tint = spec.accentColor, modifier = Modifier.size(18.dp))
                                }
                            }
                            SettingsRow(
                                "Show navigation labels",
                                trailing = {
                                    NexoraSwitch(active?.layout?.showNavigationLabels ?: true) { on ->
                                        onUpdateLayout { it.copy(showNavigationLabels = on) }
                                    }
                                },
                            )
                        }
                    }
                }
                item {
                    GlassPanel(title = "Typography & density") {
                        LabeledSlider("Text scale", spec.typographyScale, 0.85f..1.3f, { value ->
                            onUpdateSpec { it.copy(typographyScale = value) }
                        }, display = { "%.2f×".format(it) })
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Density.entries.forEach { density ->
                                Box(
                                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (spec.density == density) spec.accentColor.copy(alpha = 0.2f)
                                            else spec.textColor.copy(alpha = 0.06f)
                                        )
                                        .clickable { onUpdateSpec { it.copy(densityId = density.id) } }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        density.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (spec.density == density) spec.accentColor else spec.secondaryTextColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            StudioTab.Motion -> {
                item {
                    GlassPanel(title = "Motion profile", subtitle = "Presets, then fine-tune") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MotionProfile.entries.forEach { profile ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (active?.layout?.animationProfile == profile.id) spec.accentColor.copy(alpha = 0.12f)
                                            else spec.textColor.copy(alpha = 0.04f)
                                        )
                                        .clickable {
                                            onUpdateLayout { it.copy(animationProfile = profile.id) }
                                            onUpdateSpec {
                                                it.copy(
                                                    motionIntensity = profile.intensity,
                                                    motionSpeed = profile.speed,
                                                )
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(profile.displayName, style = MaterialTheme.typography.bodyLarge, color = spec.textColor)
                                        Text(
                                            "Intensity ${(profile.intensity * 100).toInt()}% · Speed ${profile.speed}×",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = spec.secondaryTextColor,
                                        )
                                    }
                                    if (active?.layout?.animationProfile == profile.id) {
                                        Icon(Icons.Rounded.Check, null, tint = spec.accentColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        LabeledSlider("Animation intensity", spec.motionIntensity, 0f..1f, { value ->
                            onUpdateSpec { it.copy(motionIntensity = value) }
                        })
                        LabeledSlider("Animation speed", spec.motionSpeed, 0.5f..2f, { value ->
                            onUpdateSpec { it.copy(motionSpeed = value) }
                        }, display = { "%.2f×".format(it) })
                        Text(
                            if (spec.reducedOrOff) "Motion is off. Every transition completes instantly."
                            else "Animations are interruptible and never gate an action.",
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.secondaryTextColor,
                        )
                    }
                }
                item {
                    GlassPanel(title = "Haptics & sound") {
                        SettingsRow(
                            "Haptic feedback",
                            subtitle = "Short taps on every control",
                            trailing = { NexoraSwitch(spec.haptics) { on -> onUpdateSpec { it.copy(haptics = on) } } },
                        )
                        SettingsRow(
                            "Interface sounds",
                            subtitle = "Subtle confirmation tones",
                            trailing = { NexoraSwitch(spec.sound) { on -> onUpdateSpec { it.copy(sound = on) } } },
                        )
                    }
                }
            }

            StudioTab.Profiles -> {
                item {
                    GlassPanel(title = "Interface profiles", subtitle = "${profiles.size} saved") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            profiles.forEach { profile ->
                                ProfileRow(
                                    profile = profile,
                                    active = profile.id == active?.id,
                                    spec = spec,
                                    onActivate = { onActivate(profile.id) },
                                    onDuplicate = { onDuplicate(profile, "${profile.name} copy") },
                                    onDelete = { onDelete(profile.id) },
                                )
                            }
                        }
                    }
                }
                item {
                    GlassPanel(title = "Backup") {
                        Text(
                            "Export writes the active profile as JSON into the app's cache folder; import reads a " +
                                "profile file you choose. Nothing leaves the device unless you share it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.secondaryTextColor,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassButton("Export active", onExport, tonal = true)
                            GlassButton("Import", onImport, tonal = true)
                        }
                    }
                }
            }
        }
    }
}

private enum class StudioTab(val label: String) {
    Material("Material"),
    Layout("Layout"),
    Motion("Motion"),
    Profiles("Profiles"),
}

@Composable
private fun MaterialSwatch(material: GlassMaterial, selected: Boolean, spec: GlassSpec, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) spec.accentColor.copy(alpha = 0.12f) else spec.textColor.copy(alpha = 0.04f))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(material.tintTop, material.tintBottom)))
                .then(
                    if (selected) Modifier.padding(0.dp) else Modifier
                ),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(material.displayName, style = MaterialTheme.typography.titleSmall, color = spec.textColor)
            Text(material.tagline, style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
        }
        if (selected) Icon(Icons.Rounded.Check, null, tint = spec.accentColor, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileRow(
    profile: InterfaceProfile,
    active: Boolean,
    spec: GlassSpec,
    onActivate: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) spec.accentColor.copy(alpha = 0.10f) else spec.textColor.copy(alpha = 0.04f))
            .clickable { onActivate() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(
                            GlassMaterial.fromId(profile.spec.materialId).tintTop,
                            GlassMaterial.fromId(profile.spec.materialId).tintBottom,
                        )
                    )
                ),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.bodyLarge, color = spec.textColor)
            Text(
                "${GlassMaterial.fromId(profile.spec.materialId).displayName} · ${AccentRamp.fromId(profile.spec.accentId).displayName}" +
                    if (profile.builtIn) " · built-in" else "",
                style = MaterialTheme.typography.bodySmall,
                color = spec.secondaryTextColor,
            )
        }
        if (active) PillTag("Active", highlighted = true)
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Rounded.ContentCopy, "Duplicate",
            tint = spec.secondaryTextColor,
            modifier = Modifier.size(18.dp).clickable { onDuplicate() },
        )
        if (!profile.builtIn) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.Delete, "Delete",
                tint = spec.secondaryTextColor,
                modifier = Modifier.size(18.dp).clickable { onDelete() },
            )
        }
    }
}

/** The live preview: the real glass renderer at thumbnail scale. */
@Composable
private fun LivePreview(spec: GlassSpec) {
    GlassSurface(
        Modifier.fillMaxWidth().aspectRatio(1.55f),
        spec = spec,
        contentPadding = 14.dp,
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("12:30", style = MaterialTheme.typography.headlineMedium, color = spec.textColor)
                    Text("Live preview", style = MaterialTheme.typography.labelSmall, color = spec.secondaryTextColor)
                }
                PillTag(spec.material.displayName, highlighted = true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniPane(spec, Modifier.weight(1f)) {
                    Text("Next alarm", style = MaterialTheme.typography.labelSmall, color = spec.secondaryTextColor)
                    Text("06:30", style = MaterialTheme.typography.titleMedium, color = spec.textColor)
                }
                MiniPane(spec, Modifier.weight(1f)) {
                    Text("Focus", style = MaterialTheme.typography.labelSmall, color = spec.secondaryTextColor)
                    Text("25m", style = MaterialTheme.typography.titleMedium, color = spec.accentColor)
                }
                MiniPane(spec, Modifier.weight(0.8f)) {
                    Text("Radius", style = MaterialTheme.typography.labelSmall, color = spec.secondaryTextColor)
                    Text("${spec.cornerDp.toInt()}dp", style = MaterialTheme.typography.titleMedium, color = spec.textColor)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(spec.cornerDp.dp * 0.6f))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(spec.accentColor, spec.accent.secondary)
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Primary action", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF04121C))
                }
            }
        }
    }
}

@Composable
private fun MiniPane(spec: GlassSpec, modifier: Modifier, content: @Composable () -> Unit) {
    GlassSurface(
        modifier,
        spec = spec,
        shape = RoundedCornerShape(spec.cornerDp.dp * 0.7f),
        contentPadding = 8.dp,
        elevated = false,
    ) {
        Column { content() }
    }
}
