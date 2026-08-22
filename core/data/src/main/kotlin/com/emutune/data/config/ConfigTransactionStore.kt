package com.emutune.data.config

import com.emutune.model.config.ConfigTransaction
import com.emutune.model.ids.ConfigTransactionId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persistence seam for configuration transactions. Persisting state lets a process
 * death or reboot be detected and an interrupted mutation be rolled back. The
 * in-memory implementation is the default; a Room-backed implementation can be
 * substituted without changing [ConfigTransactionManager].
 */
interface ConfigTransactionStore {
    suspend fun save(transaction: ConfigTransaction)
    suspend fun load(id: ConfigTransactionId): ConfigTransaction?
    suspend fun delete(id: ConfigTransactionId)
    suspend fun findInterrupted(): List<ConfigTransaction>
}

class InMemoryConfigTransactionStore : ConfigTransactionStore {

    private val mutex = Mutex()
    private val transactions = mutableMapOf<Long, ConfigTransaction>()

    override suspend fun save(transaction: ConfigTransaction) = mutex.withLock {
        transactions[transaction.id.value] = transaction
    }

    override suspend fun load(id: ConfigTransactionId): ConfigTransaction? = mutex.withLock {
        transactions[id.value]
    }

    override suspend fun delete(id: ConfigTransactionId) {
        mutex.withLock { transactions.remove(id.value) }
    }

    override suspend fun findInterrupted(): List<ConfigTransaction> = mutex.withLock {
        transactions.values.filter { it.state !in TERMINAL_STATES }
    }

    private companion object {
        val TERMINAL_STATES = setOf(
            com.emutune.model.config.ConfigTransactionState.COMMITTED,
            com.emutune.model.config.ConfigTransactionState.FAILED,
        )
    }
}
