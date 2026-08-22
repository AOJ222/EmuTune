package com.emutune.model.config

/**
 * A single observable stage in a configuration transaction. The transaction manager
 * emits these so a debug UI can show the lifecycle (read → snapshot → validate → apply
 * → verify → commit/rollback) rather than only the final result. Pure data — no UI
 * formatting, no persistence concerns.
 */
sealed interface ConfigTransactionStep {
    data object ReadingCurrent : ConfigTransactionStep

    data class SnapshotCreated(val hash: String) : ConfigTransactionStep

    data object ValidatingCandidate : ConfigTransactionStep

    data object Applying : ConfigTransactionStep

    data object Verifying : ConfigTransactionStep

    data class Committed(val hash: String) : ConfigTransactionStep

    data class RollingBack(val reason: String) : ConfigTransactionStep

    data class RolledBack(val rollbackVerified: Boolean) : ConfigTransactionStep

    data class Rejected(val reason: String) : ConfigTransactionStep
}
