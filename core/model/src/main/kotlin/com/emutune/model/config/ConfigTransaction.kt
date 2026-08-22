package com.emutune.model.config

import com.emutune.model.ids.ConfigTransactionId
import com.emutune.model.ids.EmulatorId
import kotlinx.serialization.Serializable

/** Result of reading the current configuration. */
sealed interface ConfigReadResult {
    data class Success(val snapshot: ConfigSnapshot) : ConfigReadResult
    data class Empty(val reason: String) : ConfigReadResult
    data class NotSupported(val reason: String) : ConfigReadResult
    data class Failure(val reason: String) : ConfigReadResult
}

data class ConfigValidationError(val key: ConfigKey, val message: String)

/** Result of validating a candidate against the emulator's typed schema. */
sealed interface ConfigValidationResult {
    data object Valid : ConfigValidationResult
    data class Invalid(val errors: List<ConfigValidationError>) : ConfigValidationResult
    data class Unsupported(val reason: String) : ConfigValidationResult
}

/** Result of applying a candidate configuration. */
sealed interface ApplyConfigResult {
    data object Applied : ApplyConfigResult
    data class NotSupported(val reason: String) : ApplyConfigResult
    data class Failed(val reason: String) : ApplyConfigResult
}

/** Result of reading back an applied configuration and comparing to what was asked for. */
sealed interface ConfigVerificationResult {
    data class Verified(val snapshot: ConfigSnapshot) : ConfigVerificationResult
    data class Mismatch(
        val expected: Map<ConfigKey, ConfigValue>,
        val actual: Map<ConfigKey, ConfigValue>,
    ) : ConfigVerificationResult
    data class Failed(val reason: String) : ConfigVerificationResult
}

/** Result of restoring a snapshot during rollback. */
sealed interface RestoreConfigResult {
    data class Restored(val snapshot: ConfigSnapshot) : RestoreConfigResult
    data class Failed(val reason: String) : RestoreConfigResult
}

/** Result of launching a route through the adapter. */
sealed interface LaunchResult {
    data object Launched : LaunchResult
    data class NotSupported(val reason: String) : LaunchResult
    data class Failed(val reason: String) : LaunchResult
}

/**
 * The state machine a configuration mutation moves through. Persisted so that a
 * process death or reboot mid-transaction can be detected and recovered from.
 */
enum class ConfigTransactionState {
    READ_CURRENT,
    VALIDATE_CURRENT,
    SNAPSHOT_CREATED,
    VALIDATE_CANDIDATE,
    APPLY,
    READ_BACK,
    VERIFY,
    COMMITTED,
    ROLLBACK,
    ROLLBACK_VERIFY,
    FAILED,
}

/**
 * A persisted record of an in-flight (or completed) configuration mutation. The
 * [snapshot] is the pre-mutation state used for rollback; [state] records how far
 * the transaction got so interruption can be handled deterministically.
 */
@Serializable
data class ConfigTransaction(
    val id: ConfigTransactionId,
    val emulatorId: EmulatorId,
    val candidate: CandidateConfig,
    val snapshot: ConfigSnapshot? = null,
    val state: ConfigTransactionState,
    val startedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
