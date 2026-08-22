package com.emutune.model.recommendation

import com.emutune.model.device.DeviceEnvironment
import com.emutune.model.device.DeviceFingerprint
import com.emutune.model.ids.DeviceFingerprintId
import com.emutune.model.device.HardwareMatchConfidence
import com.emutune.model.device.SoCModel
import com.emutune.model.evidence.BenchmarkMetrics
import com.emutune.model.evidence.BenchmarkMethod
import com.emutune.model.evidence.EvidenceGrade
import com.emutune.model.evidence.Observation
import com.emutune.model.ids.EmulatorBuildId
import com.emutune.model.ids.EmulatorId
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.ObservationId
import com.emutune.model.ids.PlatformId
import com.emutune.model.route.ExecutionRoute
import java.time.Instant

/** Shared builders for recommendation/confidence tests. */
object TestFixtures {

    val now: Instant = Instant.parse("2026-08-21T00:00:00Z")

    val snapdragon8Gen2 = SoCModel(
        name = "Snapdragon 8 Gen 2",
        vendor = "Qualcomm",
        gpu = "Adreno 740",
        matchConfidence = HardwareMatchConfidence.EXACT,
    )

    val targetDevice = DeviceFingerprint(
        id = DeviceFingerprintId(1),
        manufacturer = "AYN",
        brand = "AYN",
        model = "Thor Max",
        deviceCodename = "thormax",
        socModel = snapdragon8Gen2,
    )

    fun device(id: Long, model: String = "Thor Max", codename: String = "thormax", soc: SoCModel? = snapdragon8Gen2) =
        DeviceFingerprint(
            id = DeviceFingerprintId(id),
            manufacturer = "AYN",
            brand = "AYN",
            model = model,
            deviceCodename = codename,
            socModel = soc,
        )

    fun route(
        id: Long,
        editionId: Long,
        emulator: String = "gamenative",
        buildId: String? = null,
    ) = ExecutionRoute(
        id = ExecutionRouteId(id),
        gameEditionId = GameEditionId(editionId),
        platformId = PlatformId("pc"),
        emulatorId = EmulatorId(emulator),
        emulatorBuildId = buildId?.let { EmulatorBuildId(it) },
    )

    fun observation(
        id: Long,
        editionId: Long,
        routeId: Long,
        deviceId: Long,
        metrics: BenchmarkMetrics,
        grade: EvidenceGrade = EvidenceGrade.A_DETERMINISTIC_BENCHMARK,
        success: Boolean = true,
        crashCount: Int = 0,
        recordedAt: Instant = now,
    ) = Observation(
        id = ObservationId(id),
        gameEditionId = GameEditionId(editionId),
        executionRouteId = ExecutionRouteId(routeId),
        environment = DeviceEnvironment(deviceFingerprintId = DeviceFingerprintId(deviceId)),
        metrics = metrics,
        benchmarkMethod = BenchmarkMethod.MACHINE_BENCHMARK,
        evidenceGrade = grade,
        success = success,
        crashCount = crashCount,
        recordedAt = recordedAt,
    )
}
