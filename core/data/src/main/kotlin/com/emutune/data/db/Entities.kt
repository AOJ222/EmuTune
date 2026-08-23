package com.emutune.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities use primitive IDs rather than the domain value classes. Mapping to and
 * from the domain happens in [Mappers], keeping persistence concerns out of the domain
 * model.
 */

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val sortKey: String,
    val createdAt: Long,
)

@Entity(tableName = "game_editions")
data class GameEditionEntity(
    @PrimaryKey val id: Long,
    val gameId: Long,
    val platformId: String,
    val name: String,
    val region: String?,
    val platformIdentifier: String?,
)

@Entity(tableName = "execution_routes")
data class ExecutionRouteEntity(
    @PrimaryKey val id: Long,
    val gameEditionId: Long,
    val platformId: String,
    val emulatorId: String,
    val emulatorBuildId: String?,
    val gpuDriverId: String?,
    val translationLayerId: String?,
    val configurationId: Long?,
)

@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey val id: Long,
    val gameEditionId: Long,
    val executionRouteId: Long,
    val deviceFingerprintId: Long,
    val metricsJson: String,
    val durationSeconds: Int?,
    val benchmarkMethod: String,
    val evidenceGrade: String,
    val success: Boolean,
    val crashCount: Int,
    val recordedAt: Long,
)

@Entity(tableName = "device_profile")
data class DeviceProfileEntity(
    @PrimaryKey val id: Long = 1,
    val fingerprintJson: String,
)

@Entity(tableName = "emulator_installations", primaryKeys = ["emulatorId", "packageId"])
data class EmulatorInstallationEntity(
    val emulatorId: String,
    val packageId: String,
    val versionName: String?,
    val versionCode: Long?,
    val detectedAt: Long,
)

/** The single current play session (one row, keyed at 1 like [DeviceProfileEntity]). */
@Entity(tableName = "play_session")
data class PlaySessionEntity(
    @PrimaryKey val id: Long = 1,
    val gameEditionId: Long,
    val routeId: Long?,
    val source: String,
    val startedAt: Long,
)
