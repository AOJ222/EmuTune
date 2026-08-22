package com.emutune.data.hardware

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.emutune.model.device.DeviceFingerprint
import com.emutune.model.device.HardwareMatchConfidence
import com.emutune.model.device.SoCModel
import com.emutune.model.ids.DeviceFingerprintId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gathers reliable, non-invasive hardware information through legitimate Android APIs.
 * Values Android cannot expose are left null rather than inferred from weak evidence.
 * The SoC name comes from [Build.SOC_MODEL] on API 31+ and is best-effort — it is
 * surfaced only when it reports a real value, and always labelled as inferred.
 */
@Singleton
class DeviceProfiler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun profile(): DeviceFingerprint {
        return DeviceFingerprint(
            id = DeviceFingerprintId(1),
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL,
            deviceCodename = Build.DEVICE,
            product = Build.PRODUCT,
            androidApiLevel = Build.VERSION.SDK_INT,
            androidBuild = Build.DISPLAY,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            cpuInfo = readCpuInfo(),
            ramBytes = totalMemoryBytes(),
            displayWidthPx = context.resources.displayMetrics.widthPixels,
            displayHeightPx = context.resources.displayMetrics.heightPixels,
            refreshRatesHz = refreshRates(),
            socModel = readSocModel(),
            // GPU is intentionally null: Android exposes no reliable, non-invasive
            // GPU renderer/vendor API. Unknown is represented explicitly.
        )
    }

    /**
     * Reads the best-effort SoC identity from [Build.SOC_MODEL] on API 31+. Because the
     * platform returns the literal string "UNKNOWN" (or empty) when the property is
     * unset, those are treated as absent. The result is labelled [HardwareMatchConfidence.INFERRED]:
     * it is the platform's own report, not a confirmed measurement.
     */
    private fun readSocModel(): SoCModel? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val model = Build.SOC_MODEL.orUnknownToNull() ?: return null
        val manufacturer = Build.SOC_MANUFACTURER.orUnknownToNull()
        return SoCModel(
            name = model,
            vendor = manufacturer ?: "Unknown",
            gpu = null,
            matchConfidence = HardwareMatchConfidence.INFERRED,
        )
    }

    private fun String.orUnknownToNull(): String? {
        val trimmed = trim()
        return if (trimmed.isEmpty() || trimmed.equals("unknown", ignoreCase = true)) null else trimmed
    }

    private fun totalMemoryBytes(): Long? {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return null
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return info.totalMem.takeIf { it > 0 }
    }

    private fun refreshRates(): List<Float> {
        return runCatching {
            val displayManager =
                context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
            val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            display.supportedModes.map { it.refreshRate }.distinct()
        }.getOrDefault(emptyList())
    }

    private fun readCpuInfo(): String? {
        return runCatching { File("/proc/cpuinfo").readText() }.getOrNull()
    }
}
