package com.emutune.data.hardware

import android.content.Context
import android.os.PowerManager
import com.emutune.model.device.ThermalState
import com.emutune.model.device.ThermalStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads thermal context from the PowerManager APIs available on API 29+. Headroom is
 * reported only when the platform actually exposes it; NaN and unsupported states are
 * represented as absent rather than synthesised.
 */
@Singleton
class ThermalMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun currentThermalState(): ThermalState {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return ThermalState(sampledAt = Instant.now())

        val headroom = powerManager.getThermalHeadroom(0)
            .takeIf { !it.isNaN() }
        val status = powerManager.currentThermalStatus
            .let { mapStatus(it) }

        return ThermalState(
            headroom = headroom,
            status = status,
            sampledAt = Instant.now(),
        )
    }

    private fun mapStatus(code: Int): ThermalStatus? = when (code) {
        PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
        else -> null
    }
}
