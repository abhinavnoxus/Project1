package com.nexora.app.design

import kotlinx.serialization.Serializable
import java.util.UUID

/** Visual layout knobs owned by NEXORA STUDIO. */
@Serializable
data class LayoutConfig(
    /** Ordered home sections, identified by [HomeSection.id]. */
    val homeSections: List<String> = HomeSection.defaultOrder.map { it.id },
    val clockFace: String = ClockFace.Halo.id,
    val widgetSpan: Int = 2,
    val showNavigationLabels: Boolean = true,
    val navigationStyle: String = NavStyle.Floating.id,
    val orbSize: Float = 1f,
    val animationProfile: String = MotionProfile.Fluid.id,
)

@Serializable
data class InterfaceProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "NEXORA Default",
    val spec: GlassSpecData = GlassSpecData.preset(GlassMaterial.Crystal),
    val layout: LayoutConfig = LayoutConfig(),
    val createdAt: Long = System.currentTimeMillis(),
    val builtIn: Boolean = false,
)

/** Flat serialisable mirror of [GlassSpec] (enums stored by stable id). */
@Serializable
data class GlassSpecData(
    val materialId: String = "crystal",
    val transparency: Float = 0.42f,
    val blurDp: Float = 18f,
    val reflection: Float = 0.55f,
    val borderVisible: Boolean = true,
    val borderAlpha: Float = 0.5f,
    val cornerDp: Float = 26f,
    val shadowDepth: Float = 0.5f,
    val accentId: String = "azure",
    val darkTheme: Boolean = true,
    val ambientId: String = "aurora_veil",
    val typographyScale: Float = 1f,
    val densityId: String = "comfortable",
    val motionIntensity: Float = 0.8f,
    val motionSpeed: Float = 1f,
    val haptics: Boolean = true,
    val sound: Boolean = false,
    val ambientDrift: Boolean = false,
) {
    fun toSpec(reducedMotion: Boolean = false): GlassSpec = GlassSpec(
        material = GlassMaterial.fromId(materialId),
        transparency = transparency.coerceIn(0f, 0.92f),
        blurDp = blurDp.coerceIn(0f, 40f),
        reflection = reflection.coerceIn(0f, 1f),
        borderVisible = borderVisible,
        borderAlpha = borderAlpha.coerceIn(0f, 1f),
        cornerDp = cornerDp.coerceIn(0f, 48f),
        shadowDepth = shadowDepth.coerceIn(0f, 1f),
        accent = AccentRamp.fromId(accentId),
        darkTheme = darkTheme,
        ambient = AmbientBackdrop.fromId(ambientId),
        typographyScale = typographyScale.coerceIn(0.85f, 1.3f),
        density = Density.fromId(densityId),
        motionIntensity = motionIntensity.coerceIn(0f, 1f),
        motionSpeed = motionSpeed.coerceIn(0.5f, 2f),
        haptics = haptics,
        sound = sound,
        reducedMotion = reducedMotion,
        ambientDrift = ambientDrift,
    )

    companion object {
        fun from(spec: GlassSpec): GlassSpecData = GlassSpecData(
            materialId = spec.material.id,
            transparency = spec.transparency,
            blurDp = spec.blurDp,
            reflection = spec.reflection,
            borderVisible = spec.borderVisible,
            borderAlpha = spec.borderAlpha,
            cornerDp = spec.cornerDp,
            shadowDepth = spec.shadowDepth,
            accentId = spec.accent.id,
            darkTheme = spec.darkTheme,
            ambientId = spec.ambient.id,
            typographyScale = spec.typographyScale,
            densityId = spec.density.id,
            motionIntensity = spec.motionIntensity,
            motionSpeed = spec.motionSpeed,
            haptics = spec.haptics,
            sound = spec.sound,
            ambientDrift = spec.ambientDrift,
        )

        fun preset(material: GlassMaterial): GlassSpecData = from(GlassSpec.presetFor(material))
    }
}

/** Home sections that STUDIO can reorder, hide or restore. */
enum class HomeSection(val id: String, val title: String, val locked: Boolean = false) {
    TimeOrb("time_orb", "Time Orb", locked = true),
    DateStrip("date_strip", "Date & Almanac"),
    NextAlarm("next_alarm", "Next Alarm"),
    Agenda("agenda", "Today's Agenda"),
    Priorities("priorities", "Daily Priorities"),
    Focus("focus", "Focus Status"),
    Routines("routines", "Routine Status"),
    Reminders("reminders", "Upcoming Reminders"),
    QuickActions("quick_actions", "Quick Actions"),
    InsightStrip("insight_strip", "Insight Strip");

    companion object {
        val defaultOrder = listOf(
            TimeOrb, NextAlarm, Agenda, Priorities, Focus, Routines, Reminders, QuickActions, InsightStrip,
        )
        fun fromId(id: String): HomeSection? = entries.firstOrNull { it.id == id }
    }
}

enum class ClockFace(val id: String, val displayName: String) {
    Halo("halo", "Halo"),
    Meridian("meridian", "Meridian"),
    Sundial("sundial", "Sundial"),
    Minimal("minimal", "Minimal"),
    Epoch("epoch", "Epoch");

    companion object {
        fun fromId(id: String?): ClockFace = entries.firstOrNull { it.id == id } ?: Halo
    }
}

enum class NavStyle(val id: String, val displayName: String) {
    Floating("floating", "Floating capsule"),
    Docked("docked", "Docked bar"),
    Rail("rail", "Side rail");

    companion object {
        fun fromId(id: String?): NavStyle = entries.firstOrNull { it.id == id } ?: Floating
    }
}

enum class MotionProfile(val id: String, val displayName: String, val intensity: Float, val speed: Float) {
    Still("still", "Still", 0f, 1f),
    Calm("calm", "Calm", 0.45f, 0.85f),
    Fluid("fluid", "Fluid", 0.8f, 1f),
    Kinetic("kinetic", "Kinetic", 1f, 1.25f);

    companion object {
        fun fromId(id: String?): MotionProfile = entries.firstOrNull { it.id == id } ?: Fluid
    }
}
