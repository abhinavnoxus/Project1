package com.nexora.app.design

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrast tests.
 *
 * The first version of this app shipped secondary text at #A9B4C8 over a translucent
 * pane and the schedule times were effectively invisible in the rendered screenshot.
 * "Looks fine to me" is not a test, so these compute the actual WCAG ratio against the
 * composited pane colour.
 */
class ContrastTest {

    private fun luminance(r: Float, g: Float, b: Float): Float = luminanceOf(r, g, b)

    private fun luminanceOf(r: Float, g: Float, b: Float): Float {
        fun ch(c: Float): Float = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        return 0.2126f * ch(r) + 0.7152f * ch(g) + 0.0722f * ch(b)
    }

    private fun ratio(a: Triple<Float, Float, Float>, b: Triple<Float, Float, Float>): Float {
        val la = luminance(a.first, a.second, a.third)
        val lb = luminance(b.first, b.second, b.third)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05f) / (lo + 0.05f)
    }

    private fun triple(c: androidx.compose.ui.graphics.Color) = Triple(c.red, c.green, c.blue)

    @Test
    fun darkThemeTextMeetsAa() {
        val spec = GlassSpec(darkTheme = true)
        val paneTop = spec.material.darkTintTop
        val paneBottom = spec.material.darkTintBottom

        val primary = ratio(triple(spec.textColor), triple(paneTop))
        val secondaryTop = ratio(triple(spec.secondaryTextColor), triple(paneTop))
        val secondaryBottom = ratio(triple(spec.secondaryTextColor), triple(paneBottom))

        // AA for normal text is 4.5:1.
        assertTrue("primary text on dark pane was $primary", primary >= 7f)
        assertTrue("secondary text on dark pane top was $secondaryTop", secondaryTop >= 4.5f)
        assertTrue("secondary text on dark pane bottom was $secondaryBottom", secondaryBottom >= 4.5f)
    }

    @Test
    fun lightThemeTextMeetsAa() {
        val spec = GlassSpec(darkTheme = false)
        val pane = spec.material.top(dark = false)
        val secondary = ratio(triple(spec.secondaryTextColor), triple(pane))
        assertTrue("secondary text on light pane was $secondary", secondary >= 4.5f)
    }

    @Test
    fun everyMaterialHasADistinctDarkPane() {
        val pans = GlassMaterial.entries.map { it.darkTintTop }
        assertTrue(
            "dark tints should not be duplicates of one another",
            pans.toSet().size >= 4,
        )
    }

    @Test
    fun darkPanesAreActuallyDark() {
        GlassMaterial.entries.forEach { material ->
            val lum = luminance(
                material.darkTintTop.red,
                material.darkTintTop.green,
                material.darkTintTop.blue,
            )
            assertTrue(
                "${material.displayName} dark tint is too bright (luminance $lum) - pale " +
                    "tints over a dark backdrop are what made the first render look like " +
                    "grey plastic",
                lum < 0.15f,
            )
        }
    }
}
