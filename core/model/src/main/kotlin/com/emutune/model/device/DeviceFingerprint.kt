package com.emutune.model.device

import com.emutune.model.ids.DeviceFingerprintId
import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * A snapshot of everything reliably discoverable about the device through legitimate
 * Android APIs. Every field is nullable because Android does not expose a reliable
 * value for every attribute (notably there is no trustworthy SoC-name API). Unknown
 * is represented explicitly, never fabricated.
 */
@Serializable
data class DeviceFingerprint(
    val id: DeviceFingerprintId = DeviceFingerprintId(0),
    val manufacturer: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val deviceCodename: String? = null,
    val product: String? = null,
    val androidApiLevel: Int? = null,
    val androidBuild: String? = null,
    val supportedAbis: List<String> = emptyList(),
    val cpuInfo: String? = null,
    val gpuVendor: String? = null,
    val gpuRenderer: String? = null,
    val ramBytes: Long? = null,
    val displayWidthPx: Int? = null,
    val displayHeightPx: Int? = null,
    val refreshRatesHz: List<Float> = emptyList(),
    val socModel: SoCModel? = null,
)

/**
 * A normalized hardware match used for evidence matching. Derived separately from the
 * raw fingerprint — the raw [cpuInfo] string is never treated as proof of a SoC.
 */
@Serializable
data class SoCModel(
    val name: String,
    val vendor: String,
    val gpu: String? = null,
    val matchConfidence: HardwareMatchConfidence,
)

/**
 * How strongly a normalized SoC is believed to be correct. [EXACT] means a reliable
 * source identified it; [INFERRED] means a heuristic guessed it and must be treated
 * as a lead, not a fact.
 */
@Serializable
enum class HardwareMatchConfidence {
    EXACT,
    INFERRED,
    UNKNOWN,
}

/**
 * How closely an observation's hardware matches the current user's device. This is
 * the independent hardware-match axis — computed, not stored on the observation.
 */
enum class HardwareMatchQuality {
    EXACT_DEVICE,
    EXACT_SOC_GPU,
    RELATED_HARDWARE,
    UNKNOWN_HARDWARE,
}

/** The environment a benchmark or observation was recorded under. */
data class DeviceEnvironment(
    val deviceFingerprintId: DeviceFingerprintId,
    val androidVersion: String? = null,
    val performanceState: PerformanceState? = null,
    val thermalState: ThermalState? = null,
)

enum class PerformanceState {
    DEFAULT,
    HIGH_PERFORMANCE,
    POWER_SAVING,
    GAME_MODE,
}

/**
 * Thermal context at a point in time. Mobile benchmark results without thermal
 * context are misleading — a cold 30-second run differs from a 15-minute soak.
 * [headroom] is the 0..1 value from PowerManager.getThermalHeadroom when available.
 */
data class ThermalState(
    val headroom: Float? = null,
    val status: ThermalStatus? = null,
    val sampledAt: Instant,
)

enum class ThermalStatus {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,
}
