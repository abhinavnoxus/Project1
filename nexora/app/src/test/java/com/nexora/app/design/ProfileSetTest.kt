package com.nexora.app.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProfileSetTest {

    @Test fun `active profile resolves from id`() {
        val set = ProfileSet(activeId = "builtin_obsidian")
        assertEquals(GlassMaterial.Obsidian, set.active?.spec?.materialId?.let(GlassMaterial::fromId))
    }

    @Test fun `unknown active id falls back to the first profile`() {
        val set = ProfileSet(activeId = "does_not_exist")
        assertNotNull(set.active)
        assertEquals("builtin_crystal", set.active?.id)
    }

    @Test fun `spec honours reduced motion flag`() {
        val set = ProfileSet(activeId = "builtin_aurora")
        assertEquals(true, set.spec(reducedMotion = true).reducedMotion)
        assertEquals(false, set.spec(reducedMotion = false).reducedMotion)
    }

    @Test fun `layout comes from the active profile`() {
        val set = ProfileSet(activeId = "builtin_frost")
        assertEquals(ClockFace.Minimal.id, set.layout().clockFace)
    }

    @Test fun `built-in recipes are all visually distinct`() {
        val tints = InterfaceRepository.builtInProfiles().map { it.spec.materialId }
        assertEquals(5, tints.toSet().size)
    }

    @Test fun `every material preset produces a usable spec`() {
        GlassMaterial.entries.forEach { material ->
            val spec = GlassSpec.presetFor(material)
            assertEquals(material, spec.material)
            assertEquals(true, spec.surfaceAlpha in 0.05f..0.98f)
            assertEquals(true, spec.blurDp in 0f..40f)
        }
    }
}
