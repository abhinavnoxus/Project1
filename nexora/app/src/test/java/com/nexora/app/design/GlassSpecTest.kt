package com.nexora.app.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassSpecTest {

    @Test fun `every material has a distinct tint and a stable id`() {
        val ids = GlassMaterial.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(5, GlassMaterial.entries.size)
        assertEquals(GlassMaterial.Aurora, GlassMaterial.fromId("aurora"))
        assertEquals(GlassMaterial.Crystal, GlassMaterial.fromId("nonsense"))
        assertEquals(GlassMaterial.Crystal, GlassMaterial.fromId(null))
    }

    @Test fun `surface alpha inverts transparency and is clamped`() {
        assertEquals(1f, GlassSpec(transparency = 0f).surfaceAlpha)
        assertTrue(GlassSpec(transparency = 1f).surfaceAlpha <= 0.97f)
        assertTrue(GlassSpec(transparency = 0f).surfaceAlpha >= GlassSpec(transparency = 0.5f).surfaceAlpha)
    }

    @Test fun `duration scales with speed and collapses when reduced`() {
        val normal = GlassSpec(motionSpeed = 1f, motionIntensity = 1f)
        val fast = GlassSpec(motionSpeed = 2f, motionIntensity = 1f)
        val slow = GlassSpec(motionSpeed = 0.5f, motionIntensity = 1f)
        assertTrue(fast.durationMillis(400) < normal.durationMillis(400))
        assertTrue(slow.durationMillis(400) > normal.durationMillis(400))
        assertTrue(normal.durationMillis(400) > 0)
    }

    @Test fun `reduced motion zeroes every duration`() {
        val spec = GlassSpec(reducedMotion = true, motionIntensity = 1f)
        assertEquals(0, spec.durationMillis(500))
        assertTrue(spec.reducedOrOff)
        assertEquals(0f, spec.motionWeight())
    }

    @Test fun `zero intensity also disables motion`() {
        assertTrue(GlassSpec(motionIntensity = 0f).reducedOrOff)
    }

    @Test fun `round trip through serialisable data preserves the spec`() {
        GlassMaterial.entries.forEach { material ->
            val original = GlassSpec.presetFor(material)
            val round = GlassSpecData.from(original).toSpec()
            assertEquals(original.material, round.material)
            assertEquals(original.transparency, round.transparency, 0.0001f)
            assertEquals(original.blurDp, round.blurDp, 0.0001f)
            assertEquals(original.accent, round.accent)
            assertEquals(original.ambient, round.ambient)
            assertEquals(original.darkTheme, round.darkTheme)
            assertEquals(original.cornerDp, round.cornerDp, 0.0001f)
        }
    }

    @Test fun `hostile persisted values are clamped not trusted`() {
        val hostile = GlassSpecData(
            transparency = 5f,
            blurDp = -20f,
            reflection = 99f,
            cornerDp = 500f,
            typographyScale = 9f,
            motionSpeed = 40f,
            shadowDepth = -3f,
            borderAlpha = 8f,
        )
        val spec = hostile.toSpec()
        assertTrue(spec.transparency in 0f..0.92f)
        assertTrue(spec.blurDp in 0f..40f)
        assertTrue(spec.reflection in 0f..1f)
        assertTrue(spec.cornerDp in 0f..48f)
        assertTrue(spec.typographyScale in 0.85f..1.3f)
        assertTrue(spec.motionSpeed in 0.5f..2f)
        assertTrue(spec.shadowDepth in 0f..1f)
        assertTrue(spec.borderAlpha in 0f..1f)
    }

    @Test fun `unknown ids fall back to defaults`() {
        assertEquals(AccentRamp.Azure, AccentRamp.fromId("banana"))
        assertEquals(AmbientBackdrop.AuroraVeil, AmbientBackdrop.fromId("banana"))
        assertEquals(Density.Comfortable, Density.fromId("banana"))
        assertEquals(ClockFace.Halo, ClockFace.fromId("banana"))
        assertEquals(NavStyle.Floating, NavStyle.fromId("banana"))
        assertEquals(MotionProfile.Fluid, MotionProfile.fromId("banana"))
    }

    @Test fun `quality tiers degrade correctly by sdk and override`() {
        assertEquals(GlassTier.Shader, glassTierFor(GlassQuality.Ultra, 31))
        assertEquals(GlassTier.Ultra, glassTierFor(GlassQuality.Ultra, 30))
        assertEquals(GlassTier.Ultra, glassTierFor(GlassQuality.Ultra, 26))
        assertEquals(GlassTier.Balanced, glassTierFor(GlassQuality.Balanced, 33))
        assertEquals(GlassTier.Efficient, glassTierFor(GlassQuality.Efficient, 33))
    }

    @Test fun `light themes get dark text`() {
        assertTrue(!GlassSpec(darkTheme = false).textColor.luminanceIsHigh())
        assertTrue(GlassSpec(darkTheme = true).textColor.luminanceIsHigh())
    }

    private fun androidx.compose.ui.graphics.Color.luminanceIsHigh(): Boolean =
        (red * 0.299f + green * 0.587f + blue * 0.114f) > 0.5f

    @Test fun `all five built-in profiles exist and are deletable-proof`() {
        val builtins = InterfaceRepository.builtInProfiles()
        assertEquals(5, builtins.size)
        assertTrue(builtins.all { it.builtIn })
        assertEquals(builtins.map { it.id }.toSet().size, builtins.size)
        assertTrue(builtins.none { it.spec.materialId.isBlank() })
    }

    @Test fun `default layout contains every home section exactly once`() {
        val layout = LayoutConfig()
        assertEquals(HomeSection.entries.size, layout.homeSections.size)
        assertEquals(layout.homeSections.toSet().size, layout.homeSections.size)
        assertTrue(layout.homeSections.contains(HomeSection.TimeOrb.id))
    }

    @Test fun `content scrim stays legible in both themes`() {
        val dark = GlassSpec(darkTheme = true, transparency = 0.9f)
        val light = GlassSpec(darkTheme = false, transparency = 0.9f)
        assertTrue(dark.contentScrimAlpha >= 0.18f)
        assertTrue(light.contentScrimAlpha >= 0.1f)
        assertTrue(dark.contentScrimAlpha <= 0.96f)
    }

    @Test fun `opaque pane has no see through`() {
        assertEquals(1f, GlassSpec(transparency = 0f).surfaceAlpha)
        assertFalse(GlassSpec(transparency = 0f).surfaceAlpha < 0.99f)
    }
}
