package com.emutune.model.route

import com.emutune.model.ids.EmulatorId
import java.time.Instant

/**
 * The closed set of integration capabilities an adapter may claim. The UI is
 * capability-driven: if [CONFIG_WRITE] is absent, no automatic-modification surface
 * may be shown. A capability is never implied — it is declared and then honoured.
 */
enum class EmulatorCapability {
    INSTALLATION_DETECTION,
    GAME_DETECTION,
    GAME_LAUNCH,
    CONFIG_READ,
    CONFIG_WRITE,
    PER_GAME_CONFIG,
    CONFIG_IMPORT,
    CONFIG_EXPORT,
    DRIVER_SELECTION,
    TELEMETRY,
    AUTOMATED_BENCHMARK,
    GUIDED_CONFIG,
}

data class EmulatorIdentity(
    val id: EmulatorId,
    val displayName: String,
    val packageIds: List<String>,
)

/** A detected installation of an emulator on this device. */
data class EmulatorInstallation(
    val emulatorId: EmulatorId,
    val packageId: String,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val detectedAt: Instant,
)
