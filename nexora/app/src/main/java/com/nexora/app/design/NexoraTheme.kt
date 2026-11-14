package com.nexora.app.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalGlassSpec = staticCompositionLocalOf { GlassSpec() }
val LocalGlassQuality = staticCompositionLocalOf { GlassQuality.Balanced }
val LocalMotionEnabled = provisionLocal()

private fun provisionLocal(): ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }

object NexoraTheme {
    val spec: GlassSpec
        @Composable @ReadOnlyComposable get() = LocalGlassSpec.current

    val quality: GlassQuality
        @Composable @ReadOnlyComposable get() = LocalGlassQuality.current
}

/**
 * Type ramp. Sizes are multiplied by the user's typography scale and never allowed
 * below the platform's minimum legible size.
 */
@Composable
fun nexoraTypography(scale: Float): Typography {
    val s = scale.coerceIn(0.85f, 1.3f)
    fun Style(size: Int, weight: FontWeight, tracking: Float = 0f, lineHeight: Int = (size * 1.28f).toInt()) =
        TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = weight,
            fontSize = (size * s).sp,
            lineHeight = (lineHeight * s).sp,
            letterSpacing = tracking.em,
        )

    return Typography(
        displayLarge = Style(64, FontWeight.Light, tracking = -0.02f),
        displayMedium = Style(48, FontWeight.Light, tracking = -0.015f),
        displaySmall = Style(38, FontWeight.Normal, tracking = -0.01f),
        headlineLarge = Style(32, FontWeight.Normal, tracking = -0.01f),
        headlineMedium = Style(26, FontWeight.Medium),
        headlineSmall = Style(22, FontWeight.Medium),
        titleLarge = Style(19, FontWeight.Medium, tracking = 0.005f),
        titleMedium = Style(16, FontWeight.Medium, tracking = 0.01f),
        titleSmall = Style(14, FontWeight.Medium, tracking = 0.01f),
        bodyLarge = Style(16, FontWeight.Normal, tracking = 0.006f),
        bodyMedium = Style(14, FontWeight.Normal, tracking = 0.01f),
        bodySmall = Style(12, FontWeight.Normal, tracking = 0.02f),
        labelLarge = Style(14, FontWeight.Medium, tracking = 0.06f),
        labelMedium = Style(12, FontWeight.Medium, tracking = 0.08f),
        labelSmall = Style(10.5f.let { 11 }, FontWeight.Medium, tracking = 0.1f),
    )
}

@Composable
fun NexoraTheme(
    spec: GlassSpec,
    quality: GlassQuality = GlassQuality.Balanced,
    content: @Composable () -> Unit,
) {
    val scheme = if (spec.darkTheme) {
        darkColorScheme(
            primary = spec.accentColor,
            secondary = spec.accent.secondary,
            tertiary = spec.accent.tertiary,
            background = androidx.compose.ui.graphics.Color(0xFF05070C),
            surface = androidx.compose.ui.graphics.Color(0xFF0B0E15),
            onPrimary = androidx.compose.ui.graphics.Color(0xFF04121C),
            onBackground = spec.textColor,
            onSurface = spec.textColor,
            error = androidx.compose.ui.graphics.Color(0xFFFF5C73),
        )
    } else {
        lightColorScheme(
            primary = spec.accentColor,
            secondary = spec.accent.secondary,
            tertiary = spec.accent.tertiary,
            background = androidx.compose.ui.graphics.Color(0xFFF4F7FC),
            surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            onPrimary = androidx.compose.ui.graphics.Color(0xFF04121C),
            onBackground = spec.textColor,
            onSurface = spec.textColor,
            error = androidx.compose.ui.graphics.Color(0xFFC0263B),
        )
    }

    CompositionLocalProvider(
        LocalGlassSpec provides spec,
        LocalGlassQuality provides quality,
        LocalMotionEnabled provides !spec.reducedOrOff,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = nexoraTypography(spec.typographyScale),
            content = content,
        )
    }
}

/** Density helpers so every screen scales its paddings from one place. */
object NexoraSpacing {
    @Composable @ReadOnlyComposable
    fun gutter() = (18.dp * NexoraTheme.spec.density.scale)

    @Composable @ReadOnlyComposable
    fun pane() = (16.dp * NexoraTheme.spec.density.scale)

    @Composable @ReadOnlyComposable
    fun sectionGap() = (14.dp * NexoraTheme.spec.density.scale)

    @Composable @ReadOnlyComposable
    fun radius() = NexoraTheme.spec.cornerDp.dp
}
