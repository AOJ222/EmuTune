package com.emutune.data.emulator

import com.emutune.model.config.ApplyConfigResult
import com.emutune.model.config.CandidateConfig
import com.emutune.model.config.BooleanField
import com.emutune.model.config.ConfigField
import com.emutune.model.config.ConfigKey
import com.emutune.model.config.ConfigReadResult
import com.emutune.model.config.ConfigSnapshot
import com.emutune.model.config.ConfigValidationError
import com.emutune.model.config.ConfigValidationResult
import com.emutune.model.config.ConfigValue
import com.emutune.model.config.ConfigVerificationResult
import com.emutune.model.config.EnumField
import com.emutune.model.config.IntegerRangeField
import com.emutune.model.config.LaunchResult
import com.emutune.model.config.RestoreConfigResult
import com.emutune.model.emulator.EmulatorAdapter
import com.emutune.model.emulator.GameIdentity
import com.emutune.model.ids.ConfigSnapshotId
import com.emutune.model.ids.EmulatorId
import com.emutune.model.route.EmulatorCapability
import com.emutune.model.route.EmulatorIdentity
import com.emutune.model.route.EmulatorInstallation
import com.emutune.model.route.ExecutionRoute
import com.emutune.data.config.ConfigHasher
import java.time.Instant

/**
 * A fully controllable fake adapter used to exercise the entire configuration
 * transaction lifecycle without touching a real emulator. It is the test double that
 * proves backup/validate/apply/verify/rollback, and is the only adapter whose write
 * path is exercised by default.
 *
 * Debug-only — this class lives in the debug source set and is never compiled into
 * release.
 */
class FakeEmulatorAdapter(
    private val schema: Map<ConfigKey, ConfigField> = emptyMap(),
    initialConfig: Map<ConfigKey, ConfigValue> = emptyMap(),
) : EmulatorAdapter {

    override val identity = EmulatorIdentity(
        id = EmulatorId("fake"),
        displayName = "Fake Emulator",
        packageIds = listOf("com.emutune.fake"),
    )

    private val store = initialConfig.toMutableMap()

    var writeSupported: Boolean = true
    var failNextWrite: Boolean = false
    var corruptBackup: Boolean = false
    var corruptOnRestore: Boolean = false
    var mutateOnApply: Boolean = false
    var supportedSchemaVersion: Int = 1

    var applyCallCount: Int = 0
        private set

    override suspend fun detectInstallation(): EmulatorInstallation = EmulatorInstallation(
        emulatorId = EmulatorId("fake"),
        packageId = "com.emutune.fake",
        versionName = "0.0.0-fake",
        versionCode = 1,
        detectedAt = Instant.now(),
    )

    override suspend fun detectCapabilities(installation: EmulatorInstallation): Set<EmulatorCapability> {
        val capabilities = mutableSetOf(
            EmulatorCapability.INSTALLATION_DETECTION,
            EmulatorCapability.CONFIG_READ,
            EmulatorCapability.GUIDED_CONFIG,
        )
        if (writeSupported) {
            capabilities += EmulatorCapability.CONFIG_WRITE
            capabilities += EmulatorCapability.PER_GAME_CONFIG
        }
        return capabilities
    }

    override suspend fun readConfiguration(game: GameIdentity?): ConfigReadResult {
        if (corruptBackup) return ConfigReadResult.Failure("Backup is corrupted")
        return ConfigReadResult.Success(currentSnapshot())
    }

    override suspend fun validateConfiguration(candidate: CandidateConfig): ConfigValidationResult {
        if (candidate.schemaVersion != supportedSchemaVersion) {
            return ConfigValidationResult.Unsupported("Unknown schema version ${candidate.schemaVersion}")
        }
        val errors = candidate.fields.mapNotNull { (key, value) ->
            val field = schema[key]
            when {
                schema.isNotEmpty() && field == null ->
                    ConfigValidationError(key, "Unknown config key")
                field != null && !isValid(field, value) ->
                    ConfigValidationError(key, "Value invalid for ${field.displayName}")
                else -> null
            }
        }
        return if (errors.isEmpty()) ConfigValidationResult.Valid else ConfigValidationResult.Invalid(errors)
    }

    override suspend fun applyConfiguration(candidate: CandidateConfig): ApplyConfigResult {
        applyCallCount += 1
        if (!writeSupported) return ApplyConfigResult.NotSupported("CONFIG_WRITE unavailable")
        if (failNextWrite) {
            failNextWrite = false
            return ApplyConfigResult.Failed("Simulated write failure")
        }
        store.putAll(candidate.fields)
        if (mutateOnApply) {
            // Simulate a write that silently produced the wrong value for one key.
            candidate.fields.keys.firstOrNull()?.let { key ->
                store[key] = ConfigValue.IntegerValue(-1)
            }
        }
        return ApplyConfigResult.Applied
    }

    override suspend fun verifyAppliedConfiguration(expected: CandidateConfig): ConfigVerificationResult {
        val actual = store.toMap()
        val matches = expected.fields.all { (key, value) -> actual[key] == value }
        return if (matches) {
            ConfigVerificationResult.Verified(currentSnapshot())
        } else {
            ConfigVerificationResult.Mismatch(expected.fields, actual)
        }
    }

    override suspend fun restoreConfiguration(snapshot: ConfigSnapshot): RestoreConfigResult {
        store.clear()
        store.putAll(snapshot.fields)
        if (corruptOnRestore) {
            store[ConfigKey("__corrupt__")] = ConfigValue.BooleanValue(true)
        }
        return RestoreConfigResult.Restored(currentSnapshot())
    }

    override suspend fun launch(game: GameIdentity, route: ExecutionRoute): LaunchResult = LaunchResult.Launched

    fun currentFields(): Map<ConfigKey, ConfigValue> = store.toMap()

    private fun currentSnapshot(): ConfigSnapshot = ConfigSnapshot(
        id = ConfigSnapshotId(0),
        emulatorId = EmulatorId("fake"),
        schemaVersion = 1,
        fields = store.toMap(),
        hash = ConfigHasher.hash(store),
        payload = null,
        createdAtEpochMillis = System.currentTimeMillis(),
    )

    private fun isValid(field: ConfigField, value: ConfigValue): Boolean = when (field) {
        is BooleanField -> value is ConfigValue.BooleanValue
        is IntegerRangeField -> value is ConfigValue.IntegerValue && value.value in field.min..field.max
        is EnumField -> value is ConfigValue.EnumValue && field.options.any { it.id == value.option }
    }
}
