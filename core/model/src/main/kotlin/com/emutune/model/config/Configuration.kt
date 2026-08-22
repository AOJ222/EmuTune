package com.emutune.model.config

import com.emutune.model.ids.ConfigSnapshotId
import com.emutune.model.ids.EmulatorId
import kotlinx.serialization.Serializable

/** Canonical key for a single emulator setting. Never a bare string. */
@JvmInline
@Serializable
value class ConfigKey(val value: String)

/**
 * A typed configuration value. Emulator configurations are never modelled as a
 * `Map<String, String>`; each value carries its own type so validation can be
 * exhaustive and fail closed.
 */
@Serializable
sealed interface ConfigValue {
    @Serializable
    data class BooleanValue(val value: Boolean) : ConfigValue

    @Serializable
    data class IntegerValue(val value: Int) : ConfigValue

    @Serializable
    data class DecimalValue(val value: Double) : ConfigValue

    @Serializable
    data class EnumValue(val option: String) : ConfigValue

    @Serializable
    data class TextValue(val value: String) : ConfigValue
}

enum class ConfigCategory {
    PERFORMANCE,
    QUALITY,
    ACCURACY,
    GRAPHICS,
    AUDIO,
    CONTROLS,
    SYSTEM,
    ADVANCED,
}

/** How strongly a setting influences a given axis. Used for candidate generation later. */
enum class Relevance {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
}

/**
 * The typed schema of a single setting. A setting records its valid range/options,
 * default, the builds it is valid for, and its dependencies/conflicts — so unknown
 * schema or config versions fail closed rather than being guessed.
 */
@Serializable
sealed interface ConfigField {
    val key: ConfigKey
    val displayName: String
    val description: String
    val category: ConfigCategory
    val defaultValue: ConfigValue
    val requiresRestart: Boolean
    val performanceRelevance: Relevance
    val visualQualityRelevance: Relevance
    val accuracyRelevance: Relevance
    val introducedInBuild: String?
    val removedInBuild: String?
    val dependencies: List<ConfigKey>
    val conflicts: List<ConfigKey>
}

@Serializable
data class BooleanField(
    override val key: ConfigKey,
    override val displayName: String,
    override val description: String,
    override val category: ConfigCategory,
    override val defaultValue: ConfigValue,
    override val requiresRestart: Boolean = false,
    override val performanceRelevance: Relevance = Relevance.NONE,
    override val visualQualityRelevance: Relevance = Relevance.NONE,
    override val accuracyRelevance: Relevance = Relevance.NONE,
    override val introducedInBuild: String? = null,
    override val removedInBuild: String? = null,
    override val dependencies: List<ConfigKey> = emptyList(),
    override val conflicts: List<ConfigKey> = emptyList(),
) : ConfigField

@Serializable
data class IntegerRangeField(
    override val key: ConfigKey,
    override val displayName: String,
    override val description: String,
    override val category: ConfigCategory,
    override val defaultValue: ConfigValue,
    val min: Int,
    val max: Int,
    val step: Int = 1,
    override val requiresRestart: Boolean = false,
    override val performanceRelevance: Relevance = Relevance.NONE,
    override val visualQualityRelevance: Relevance = Relevance.NONE,
    override val accuracyRelevance: Relevance = Relevance.NONE,
    override val introducedInBuild: String? = null,
    override val removedInBuild: String? = null,
    override val dependencies: List<ConfigKey> = emptyList(),
    override val conflicts: List<ConfigKey> = emptyList(),
) : ConfigField

@Serializable
data class EnumOption(
    val id: String,
    val label: String,
)

@Serializable
data class EnumField(
    override val key: ConfigKey,
    override val displayName: String,
    override val description: String,
    override val category: ConfigCategory,
    override val defaultValue: ConfigValue,
    val options: List<EnumOption>,
    override val requiresRestart: Boolean = false,
    override val performanceRelevance: Relevance = Relevance.NONE,
    override val visualQualityRelevance: Relevance = Relevance.NONE,
    override val accuracyRelevance: Relevance = Relevance.NONE,
    override val introducedInBuild: String? = null,
    override val removedInBuild: String? = null,
    override val dependencies: List<ConfigKey> = emptyList(),
    override val conflicts: List<ConfigKey> = emptyList(),
) : ConfigField

/**
 * Where a candidate configuration came from. Every recommendation carries structured
 * provenance — a candidate is never just "some settings we decided on".
 */
@Serializable
enum class CandidateSource {
    VERIFIED_EVIDENCE,
    RESEARCH_CANDIDATE,
    COMMUNITY_REPORT,
    MANUAL_ENTRY,
}

/** A proposed configuration to validate and apply. */
@Serializable
data class CandidateConfig(
    val emulatorId: EmulatorId,
    val fields: Map<ConfigKey, ConfigValue>,
    val schemaVersion: Int,
    val source: CandidateSource,
)

/**
 * An immutable snapshot of a configuration, taken before any mutation so that a
 * failed apply can be rolled back deterministically. [payload] is an opaque,
 * adapter-provided encoding of the raw config where one exists.
 */
@Serializable
data class ConfigSnapshot(
    val id: ConfigSnapshotId,
    val emulatorId: EmulatorId,
    val schemaVersion: Int,
    val fields: Map<ConfigKey, ConfigValue>,
    val hash: String,
    val payload: String? = null,
    val createdAtEpochMillis: Long,
)
