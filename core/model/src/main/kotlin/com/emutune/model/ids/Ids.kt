package com.emutune.model.ids

import kotlinx.serialization.Serializable

/**
 * Typed identifiers for every domain aggregate.
 *
 * Identifiers that represent a fixed, human-meaningful key (emulators, platforms,
 * drivers, translation layers, builds) use [String]; identifiers that are only ever
 * auto-incrementing database keys use [Long]. This prevents an [EmulatorId] from being
 * passed where a [PlatformId] is expected, which raw strings/longs would silently allow.
 */
@JvmInline
@Serializable
value class GameId(val value: Long)

@JvmInline
@Serializable
value class GameEditionId(val value: Long)

@JvmInline
@Serializable
value class ExecutionRouteId(val value: Long)

@JvmInline
@Serializable
value class EmulatorId(val value: String)

@JvmInline
@Serializable
value class EmulatorBuildId(val value: String)

@JvmInline
@Serializable
value class PlatformId(val value: String)

@JvmInline
@Serializable
value class GpuDriverId(val value: String)

@JvmInline
@Serializable
value class TranslationLayerId(val value: String)

@JvmInline
@Serializable
value class ConfigurationId(val value: Long)

@JvmInline
@Serializable
value class ObservationId(val value: Long)

@JvmInline
@Serializable
value class EvidenceId(val value: Long)

@JvmInline
@Serializable
value class BenchmarkSessionId(val value: Long)

@JvmInline
@Serializable
value class DeviceFingerprintId(val value: Long)

@JvmInline
@Serializable
value class ConfigSnapshotId(val value: Long)

@JvmInline
@Serializable
value class ConfigTransactionId(val value: Long)

@JvmInline
@Serializable
value class RecommendationId(val value: Long)

@JvmInline
@Serializable
value class ActivityEventId(val value: Long)
