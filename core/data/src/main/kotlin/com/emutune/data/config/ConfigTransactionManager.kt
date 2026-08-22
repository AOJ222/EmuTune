package com.emutune.data.config

import com.emutune.model.config.ApplyConfigResult
import com.emutune.model.config.CandidateConfig
import com.emutune.model.config.ConfigReadResult
import com.emutune.model.config.ConfigSnapshot
import com.emutune.model.config.ConfigTransaction
import com.emutune.model.config.ConfigTransactionState
import com.emutune.model.config.ConfigTransactionStep
import com.emutune.model.config.ConfigValidationResult
import com.emutune.model.config.ConfigVerificationResult
import com.emutune.model.config.RestoreConfigResult
import com.emutune.model.emulator.EmulatorAdapter
import com.emutune.model.emulator.GameIdentity
import com.emutune.model.ids.ConfigSnapshotId
import com.emutune.model.ids.ConfigTransactionId
import com.emutune.model.route.EmulatorCapability
import javax.inject.Inject
import javax.inject.Singleton

/** The outcome of a configuration mutation. */
sealed interface ConfigTransactionResult {
    data class Committed(val snapshot: ConfigSnapshot) : ConfigTransactionResult
    data class RolledBack(val reason: String, val rollbackVerified: Boolean) : ConfigTransactionResult
    data class Rejected(val reason: String) : ConfigTransactionResult
}

/**
 * Orchestrates a configuration mutation as a transactional state machine:
 * read current, snapshot, validate candidate, apply, read back, verify, then commit —
 * or roll back on any failure and verify the rollback.
 *
 * The state machine persists progress via [ConfigTransactionStore] so an interrupted
 * transaction can be detected and recovered after process death.
 */
@Singleton
class ConfigTransactionManager @Inject constructor(
    private val transactionStore: ConfigTransactionStore,
) {

    private var nextId = 1L

    suspend fun apply(
        adapter: EmulatorAdapter,
        candidate: CandidateConfig,
        game: GameIdentity? = null,
        onStep: suspend (ConfigTransactionStep) -> Unit = {},
    ): ConfigTransactionResult {
        val installation = adapter.detectInstallation()
        if (installation == null) {
            val step = ConfigTransactionStep.Rejected("Emulator is not installed")
            onStep(step)
            return ConfigTransactionResult.Rejected("Emulator is not installed")
        }
        val capabilities = adapter.detectCapabilities(installation)
        if (EmulatorCapability.CONFIG_WRITE !in capabilities) {
            val step = ConfigTransactionStep.Rejected("Adapter does not support CONFIG_WRITE")
            onStep(step)
            return ConfigTransactionResult.Rejected("Adapter does not support CONFIG_WRITE")
        }

        val transaction = newTransaction(adapter, candidate)
        transactionStore.save(transaction)

        // Read current, snapshot it.
        onStep(ConfigTransactionStep.ReadingCurrent)
        val snapshot = when (val read = adapter.readConfiguration(game)) {
            is ConfigReadResult.Success -> read.snapshot.also {
                onStep(ConfigTransactionStep.SnapshotCreated(it.hash))
            }
            is ConfigReadResult.Empty -> emptySnapshot(adapter, candidate).also {
                onStep(ConfigTransactionStep.SnapshotCreated(it.hash))
            }
            is ConfigReadResult.NotSupported -> return reject(transaction, "Current configuration cannot be read", onStep)
            is ConfigReadResult.Failure -> return reject(transaction, "Failed to read current configuration: ${read.reason}", onStep)
        }

        // Validate the candidate against the emulator's schema.
        onStep(ConfigTransactionStep.ValidatingCandidate)
        when (val validation = adapter.validateConfiguration(candidate)) {
            is ConfigValidationResult.Valid -> Unit
            is ConfigValidationResult.Invalid -> return reject(transaction, "Candidate is invalid: ${validation.errors}", onStep)
            is ConfigValidationResult.Unsupported -> return reject(transaction, "Validation unsupported", onStep)
        }

        // Apply.
        onStep(ConfigTransactionStep.Applying)
        when (val apply = adapter.applyConfiguration(candidate)) {
            is ApplyConfigResult.Applied -> Unit
            is ApplyConfigResult.NotSupported -> return rollback(adapter, transaction, snapshot, "Write not supported", onStep)
            is ApplyConfigResult.Failed -> return rollback(adapter, transaction, snapshot, "Write failed: ${apply.reason}", onStep)
        }

        // Read back and verify.
        onStep(ConfigTransactionStep.Verifying)
        when (val verify = adapter.verifyAppliedConfiguration(candidate)) {
            is ConfigVerificationResult.Verified -> {
                mark(transaction, ConfigTransactionState.COMMITTED)
                transactionStore.delete(transaction.id)
                onStep(ConfigTransactionStep.Committed(verify.snapshot.hash))
                return ConfigTransactionResult.Committed(verify.snapshot)
            }
            is ConfigVerificationResult.Mismatch -> return rollback(adapter, transaction, snapshot, "Read-back mismatch", onStep)
            is ConfigVerificationResult.Failed -> return rollback(adapter, transaction, snapshot, "Verification failed: ${verify.reason}", onStep)
        }
    }

    /** Rolls back any non-terminal transaction found in the store after a restart. */
    suspend fun recoverInterrupted(adapter: EmulatorAdapter): List<ConfigTransactionResult> {
        return transactionStore.findInterrupted().map { transaction ->
            val snapshot = transaction.snapshot
            if (snapshot == null) {
                mark(transaction, ConfigTransactionState.FAILED)
                transactionStore.delete(transaction.id)
                ConfigTransactionResult.RolledBack("No snapshot available", rollbackVerified = false)
            } else {
                rollback(adapter, transaction, snapshot, "Recovered interrupted transaction")
            }
        }
    }

    private suspend fun rollback(
        adapter: EmulatorAdapter,
        transaction: ConfigTransaction,
        snapshot: ConfigSnapshot,
        reason: String,
        onStep: suspend (ConfigTransactionStep) -> Unit = {},
    ): ConfigTransactionResult {
        mark(transaction, ConfigTransactionState.ROLLBACK)
        onStep(ConfigTransactionStep.RollingBack(reason))
        val restored = when (adapter.restoreConfiguration(snapshot)) {
            is RestoreConfigResult.Restored -> true
            is RestoreConfigResult.Failed -> false
        }
        val verified = if (restored) {
            when (val readBack = adapter.readConfiguration(null)) {
                is ConfigReadResult.Success -> readBack.snapshot.hash == snapshot.hash
                else -> false
            }
        } else {
            false
        }
        mark(transaction, ConfigTransactionState.FAILED)
        transactionStore.delete(transaction.id)
        onStep(ConfigTransactionStep.RolledBack(verified))
        return ConfigTransactionResult.RolledBack(reason, verified)
    }

    private suspend fun reject(
        transaction: ConfigTransaction,
        reason: String,
        onStep: suspend (ConfigTransactionStep) -> Unit = {},
    ): ConfigTransactionResult {
        mark(transaction, ConfigTransactionState.FAILED)
        transactionStore.delete(transaction.id)
        onStep(ConfigTransactionStep.Rejected(reason))
        return ConfigTransactionResult.Rejected(reason)
    }

    private suspend fun mark(transaction: ConfigTransaction, state: ConfigTransactionState) {
        transactionStore.save(transaction.copy(state = state, updatedAtEpochMillis = System.currentTimeMillis()))
    }

    private fun newTransaction(adapter: EmulatorAdapter, candidate: CandidateConfig): ConfigTransaction {
        val now = System.currentTimeMillis()
        return ConfigTransaction(
            id = ConfigTransactionId(nextId++),
            emulatorId = adapter.identity.id,
            candidate = candidate,
            snapshot = null,
            state = ConfigTransactionState.READ_CURRENT,
            startedAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
    }

    private fun emptySnapshot(adapter: EmulatorAdapter, candidate: CandidateConfig): ConfigSnapshot {
        val empty = emptyMap<com.emutune.model.config.ConfigKey, com.emutune.model.config.ConfigValue>()
        return ConfigSnapshot(
            id = ConfigSnapshotId(0),
            emulatorId = adapter.identity.id,
            schemaVersion = candidate.schemaVersion,
            fields = empty,
            hash = ConfigHasher.hash(empty),
            payload = null,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
    }
}
