package com.emutune.data.db

import com.emutune.model.device.DeviceEnvironment
import com.emutune.model.device.DeviceFingerprint
import com.emutune.model.evidence.BenchmarkMetrics
import com.emutune.model.evidence.BenchmarkMethod
import com.emutune.model.evidence.EvidenceGrade
import com.emutune.model.evidence.Observation
import com.emutune.model.game.Game
import com.emutune.model.game.GameEdition
import com.emutune.model.ids.ConfigurationId
import com.emutune.model.ids.DeviceFingerprintId
import com.emutune.model.ids.EmulatorBuildId
import com.emutune.model.ids.EmulatorId
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.GameId
import com.emutune.model.ids.GpuDriverId
import com.emutune.model.ids.ObservationId
import com.emutune.model.ids.PlatformId
import com.emutune.model.ids.TranslationLayerId
import com.emutune.model.route.EmulatorInstallation
import com.emutune.model.route.ExecutionRoute
import java.time.Instant

/**
 * Maps between Room entities (primitive fields) and domain types (value classes). The
 * mapping is the single place persistence concerns are translated, so the domain stays
 * free of Room annotations and JSON encoding.
 */
object Mappers {

    fun GameEntity.toDomain() = Game(
        id = GameId(id),
        title = title,
        sortKey = sortKey,
        createdAt = Instant.ofEpochMilli(createdAt),
    )

    fun Game.toEntity() = GameEntity(
        id = id.value,
        title = title,
        sortKey = sortKey,
        createdAt = createdAt.toEpochMilli(),
    )

    fun GameEditionEntity.toDomain() = GameEdition(
        id = GameEditionId(id),
        gameId = GameId(gameId),
        platformId = PlatformId(platformId),
        name = name,
        region = region,
    )

    fun GameEdition.toEntity() = GameEditionEntity(
        id = id.value,
        gameId = gameId.value,
        platformId = platformId.value,
        name = name,
        region = region,
    )

    fun ExecutionRouteEntity.toDomain() = ExecutionRoute(
        id = ExecutionRouteId(id),
        gameEditionId = GameEditionId(gameEditionId),
        platformId = PlatformId(platformId),
        emulatorId = EmulatorId(emulatorId),
        emulatorBuildId = emulatorBuildId?.let { EmulatorBuildId(it) },
        gpuDriverId = gpuDriverId?.let { GpuDriverId(it) },
        translationLayerId = translationLayerId?.let { TranslationLayerId(it) },
        configurationId = configurationId?.let { ConfigurationId(it) },
    )

    fun ExecutionRoute.toEntity() = ExecutionRouteEntity(
        id = id.value,
        gameEditionId = gameEditionId.value,
        platformId = platformId.value,
        emulatorId = emulatorId.value,
        emulatorBuildId = emulatorBuildId?.value,
        gpuDriverId = gpuDriverId?.value,
        translationLayerId = translationLayerId?.value,
        configurationId = configurationId?.value,
    )

    fun ObservationEntity.toDomain() = Observation(
        id = ObservationId(id),
        gameEditionId = GameEditionId(gameEditionId),
        executionRouteId = ExecutionRouteId(executionRouteId),
        environment = DeviceEnvironment(deviceFingerprintId = DeviceFingerprintId(deviceFingerprintId)),
        metrics = JsonCodec.json.decodeFromString<BenchmarkMetrics>(metricsJson),
        durationSeconds = durationSeconds,
        benchmarkMethod = BenchmarkMethod.valueOf(benchmarkMethod),
        evidenceGrade = EvidenceGrade.valueOf(evidenceGrade),
        success = success,
        crashCount = crashCount,
        recordedAt = Instant.ofEpochMilli(recordedAt),
    )

    fun Observation.toEntity() = ObservationEntity(
        id = id.value,
        gameEditionId = gameEditionId.value,
        executionRouteId = executionRouteId.value,
        deviceFingerprintId = environment.deviceFingerprintId.value,
        metricsJson = JsonCodec.json.encodeToString(metrics),
        durationSeconds = durationSeconds,
        benchmarkMethod = benchmarkMethod.name,
        evidenceGrade = evidenceGrade.name,
        success = success,
        crashCount = crashCount,
        recordedAt = recordedAt.toEpochMilli(),
    )

    fun DeviceProfileEntity.toDomain() = JsonCodec.json.decodeFromString<DeviceFingerprint>(fingerprintJson)

    fun DeviceFingerprint.toProfileEntity() = DeviceProfileEntity(
        id = 1,
        fingerprintJson = JsonCodec.json.encodeToString(this),
    )

    fun EmulatorInstallationEntity.toDomain() = EmulatorInstallation(
        emulatorId = EmulatorId(emulatorId),
        packageId = packageId,
        versionName = versionName,
        versionCode = versionCode,
        detectedAt = Instant.ofEpochMilli(detectedAt),
    )

    fun EmulatorInstallation.toEntity() = EmulatorInstallationEntity(
        emulatorId = emulatorId.value,
        packageId = packageId,
        versionName = versionName,
        versionCode = versionCode,
        detectedAt = detectedAt.toEpochMilli(),
    )
}
