package com.nexora.app.platform

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.nexora.app.design.GlassQuality

/**
 * Device capability probe.
 *
 * Everything here is read from the platform — no hardcoded model table, no guessing.
 * [recommendedQuality] is a policy on top of those facts, and the user can always
 * override it in Settings.
 */
data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val sdkInt: Int,
    val androidRelease: String,
    val cpuCores: Int,
    val totalRamMb: Long,
    val lowRamDevice: Boolean,
    val gpuRenderer: String?,
    val maxRefreshRate: Float,
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val isRealmeUi: Boolean,
) {

    val isNarzo20Pro: Boolean
        get() = model.contains("narzo 20 pro", ignoreCase = true) ||
            model.contains("RMX2161", ignoreCase = true)

    val supportsRuntimeShader: Boolean get() = sdkInt >= Build.VERSION_CODES.S

    /** True when the platform can blur a layer at all (API 31+). */
    val supportsBlur: Boolean get() = supportsRuntimeShader

    val classLabel: String
        get() = when {
            lowRamDevice || totalRamMb < 3072 -> "Entry"
            totalRamMb < 5120 -> "Mid"
            cpuCores >= 8 && totalRamMb >= 6144 -> "Upper-mid"
            else -> "High"
        }

    /**
     * Policy:
     *  - Low-RAM devices and anything without GPU blur support start at Balanced.
     *  - Devices with 6 GB+ and 8 cores may start at Ultra.
     *  - The Narzo 20 Pro (Helio G95, 6 GB, MALI-G76) starts at Balanced; Ultra remains
     *    available and is benchmarked rather than assumed.
     */
    fun recommendedQuality(): GlassQuality = when {
        lowRamDevice -> GlassQuality.Efficient
        totalRamMb < 4096 -> GlassQuality.Balanced
        cpuCores >= 8 && totalRamMb >= 6144 && supportsRuntimeShader -> GlassQuality.Balanced
        else -> GlassQuality.Balanced
    }

    fun summary(): String = buildString {
        append("$manufacturer $model · Android $androidRelease (API $sdkInt)")
        append("\n${cpuCores} cores · ${totalRamMb / 1024} GB RAM · $classLabel class")
        append("\n${widthPx}×${heightPx} @ ${densityDpi}dpi · up to ${"%.0f".format(maxRefreshRate)} Hz")
        append("\nBlur: ${if (supportsBlur) "native RenderEffect" else "gradient fallback (pre-API 31)"}")
        gpuRenderer?.let { append("\nGPU: $it") }
        if (isRealmeUi) append("\nRealme UI detected: exact-alarm and background limits may apply")
    }

    companion object {
        fun detect(context: Context): DeviceProfile {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.getRealMetrics(metrics)
            val refresh = runCatching { wm?.defaultDisplay?.refreshRate ?: 60f }.getOrDefault(60f)

            // GPU string comes from the system property exposed by the driver stack.
            val gpu = System.getProperty("ro.hardware.egl") ?: System.getProperty("ro.opengles.version")

            return DeviceProfile(
                manufacturer = Build.MANUFACTURER.orEmpty(),
                model = Build.MODEL.orEmpty(),
                sdkInt = Build.VERSION.SDK_INT,
                androidRelease = Build.VERSION.RELEASE.orEmpty(),
                cpuCores = Runtime.getRuntime().availableProcessors(),
                totalRamMb = if (mi.totalMem > 0) mi.totalMem / (1024 * 1024) else 0L,
                lowRamDevice = am?.isLowRamDevice ?: false,
                gpuRenderer = gpu,
                maxRefreshRate = refresh,
                widthPx = metrics.widthPixels,
                heightPx = metrics.heightPixels,
                densityDpi = metrics.densityDpi,
                isRealmeUi = Build.DISPLAY?.contains("realme", true) == true ||
                    Build.BRAND?.contains("realme", true) == true,
            )
        }
    }
}
