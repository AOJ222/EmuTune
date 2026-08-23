package com.emutune.data.config

import com.emutune.data.emulator.FakeEmulatorAdapter
import com.emutune.model.config.CandidateConfig
import com.emutune.model.config.CandidateSource
import com.emutune.model.config.ConfigKey
import com.emutune.model.config.ConfigSnapshot
import com.emutune.model.config.ConfigTransaction
import com.emutune.model.config.ConfigTransactionState
import com.emutune.model.config.ConfigTransactionStep
import com.emutune.model.config.ConfigValue
import com.emutune.model.config.IntegerRangeField
import com.emutune.model.config.Relevance
import com.emutune.model.config.ConfigCategory
import com.emutune.model.ids.ConfigSnapshotId
import com.emutune.model.ids.ConfigTransactionId
import com.emutune.model.ids.EmulatorId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigTransactionManagerTest {

    private val key = ConfigKey("cpu_overclock")

    private val schema = mapOf(
        key to IntegerRangeField(
            key = key,
            displayName = "CPU Overclock",
            description = "Emulated CPU clock override",
            category = ConfigCategory.PERFORMANCE,
            defaultValue = ConfigValue.IntegerValue(100),
            min = 0,
            max = 100,
            performanceRelevance = Relevance.HIGH,
        ),
    )

    private fun candidate(value: Int = 75, version: Int = 1) = CandidateConfig(
        emulatorId = EmulatorId("fake"),
        fields = mapOf(key to ConfigValue.IntegerValue(value)),
        schemaVersion = version,
        source = CandidateSource.VERIFIED_EVIDENCE,
    )

    @Test
    fun `successful commit`() = runTest {
        val adapter = FakeEmulatorAdapter(schema = schema)
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())

        val result = manager.apply(adapter, candidate())

        assertTrue(result is ConfigTransactionResult.Committed)
        assertEquals(ConfigValue.IntegerValue(75), adapter.currentFields()[key])
        assertEquals(1, adapter.applyCallCount)
    }

    @Test
    fun `invalid candidate is rejected`() = runTest {
        val adapter = FakeEmulatorAdapter(schema = schema)
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())

        val result = manager.apply(adapter, candidate(value = 150))

        assertTrue(result is ConfigTransactionResult.Rejected)
        assertEquals(0, adapter.applyCallCount)
    }

    @Test
    fun `failed write rolls back`() = runTest {
        val adapter = FakeEmulatorAdapter(
            schema = schema,
            initialConfig = mapOf(key to ConfigValue.IntegerValue(50)),
        )
        adapter.failNextWrite = true
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())

        val result = manager.apply(adapter, candidate())

        assertTrue(result is ConfigTransactionResult.RolledBack)
        assertEquals(1, adapter.applyCallCount)
        // Rolled back to the pre-mutation value.
        assertEquals(ConfigValue.IntegerValue(50), adapter.currentFields()[key])
    }

    @Test
    fun `read back mismatch rolls back`() = runTest {
        val adapter = FakeEmulatorAdapter(
            schema = schema,
            initialConfig = mapOf(key to ConfigValue.IntegerValue(50)),
        )
        adapter.mutateOnApply = true
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())

        val result = manager.apply(adapter, candidate())

        assertTrue(result is ConfigTransactionResult.RolledBack)
        assertEquals(ConfigValue.IntegerValue(50), adapter.currentFields()[key])
    }

    @Test
    fun `rollback verifies the restored state`() = runTest {
        val adapter = FakeEmulatorAdapter(
            schema = schema,
            initialConfig = mapOf(key to ConfigValue.IntegerValue(50)),
        )
        adapter.failNextWrite = true
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())

        val result = manager.apply(adapter, candidate())

        assertTrue(result is ConfigTransactionResult.RolledBack)
        assertTrue((result as ConfigTransactionResult.RolledBack).rollbackVerified)
    }

    @Test
    fun `rollback verification failure is reported`() = runTest {
        val adapter = FakeEmulatorAdapter(
            schema = schema,
            initialConfig = mapOf(key to ConfigValue.IntegerValue(50)),
        )
        adapter.failNextWrite = true
        adapter.corruptOnRestore = true
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())

        val result = manager.apply(adapter, candidate())

        assertTrue(result is ConfigTransactionResult.RolledBack)
        assertEquals(false, (result as ConfigTransactionResult.RolledBack).rollbackVerified)
    }

    @Test
    fun `corrupted backup is rejected`() = runTest {
        val adapter = FakeEmulatorAdapter(schema = schema)
        adapter.corruptBackup = true
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())

        val result = manager.apply(adapter, candidate())

        assertTrue(result is ConfigTransactionResult.Rejected)
        assertEquals(0, adapter.applyCallCount)
    }

    @Test
    fun `unknown config schema version is rejected`() = runTest {
        val adapter = FakeEmulatorAdapter(schema = schema)
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())

        val result = manager.apply(adapter, candidate(version = 2))

        assertTrue(result is ConfigTransactionResult.Rejected)
        assertEquals(0, adapter.applyCallCount)
    }

    @Test
    fun `adapter without write never receives a write`() = runTest {
        val adapter = FakeEmulatorAdapter(schema = schema)
        adapter.writeSupported = false
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())

        val result = manager.apply(adapter, candidate())

        assertTrue(result is ConfigTransactionResult.Rejected)
        assertEquals(0, adapter.applyCallCount)
    }

    @Test
    fun `interrupted transaction is recovered`() = runTest {
        val adapter = FakeEmulatorAdapter(
            schema = schema,
            initialConfig = mapOf(key to ConfigValue.IntegerValue(50)),
        )
        val store = InMemoryConfigTransactionStore()
        val manager = ConfigTransactionManager(store)

        val fields = mapOf(key to ConfigValue.IntegerValue(50))
        val snapshot = ConfigSnapshot(
            id = ConfigSnapshotId(0),
            emulatorId = EmulatorId("fake"),
            schemaVersion = 1,
            fields = fields,
            hash = ConfigHasher.hash(fields),
            payload = null,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        store.save(
            ConfigTransaction(
                id = ConfigTransactionId(99),
                emulatorId = EmulatorId("fake"),
                candidate = candidate(),
                snapshot = snapshot,
                state = ConfigTransactionState.APPLY,
                startedAtEpochMillis = System.currentTimeMillis(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )

        val results = manager.recoverInterrupted(adapter)

        assertEquals(1, results.size)
        assertTrue(results.single() is ConfigTransactionResult.RolledBack)
    }

    @Test
    fun `steps are emitted in order on commit`() = runTest {
        val adapter = FakeEmulatorAdapter(schema = schema)
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())
        val steps = mutableListOf<ConfigTransactionStep>()

        manager.apply(adapter, candidate(), onStep = { steps += it })

        assertTrue(steps.first() is ConfigTransactionStep.ReadingCurrent)
        assertTrue(steps.any { it is ConfigTransactionStep.SnapshotCreated })
        assertTrue(steps.any { it is ConfigTransactionStep.ValidatingCandidate })
        assertTrue(steps.any { it is ConfigTransactionStep.Applying })
        assertTrue(steps.any { it is ConfigTransactionStep.Verifying })
        assertTrue(steps.last() is ConfigTransactionStep.Committed)
    }

    @Test
    fun `rollback emits rolling back then rolled back`() = runTest {
        val adapter = FakeEmulatorAdapter(
            schema = schema,
            initialConfig = mapOf(key to ConfigValue.IntegerValue(50)),
        )
        adapter.failNextWrite = true
        val manager = ConfigTransactionManager(InMemoryConfigTransactionStore())
        val steps = mutableListOf<ConfigTransactionStep>()

        manager.apply(adapter, candidate(), onStep = { steps += it })

        assertTrue(steps.any { it is ConfigTransactionStep.RollingBack })
        val last = steps.last()
        assertTrue(last is ConfigTransactionStep.RolledBack)
        assertTrue((last as ConfigTransactionStep.RolledBack).rollbackVerified)
    }

    @Test
    fun `snapshot is persisted for interruption recovery`() = runTest {
        val adapter = FakeEmulatorAdapter(
            schema = schema,
            initialConfig = mapOf(key to ConfigValue.IntegerValue(50)),
        )
        val store = RecordingStore()
        val manager = ConfigTransactionManager(store)

        manager.apply(adapter, candidate())

        assertTrue(
            store.saved.any { it.snapshot != null },
            "The pre-mutation snapshot was never persisted, so recoverInterrupted cannot restore state",
        )
    }

    /** Records every transaction that passes through save(), so persistence can be asserted. */
    private class RecordingStore : ConfigTransactionStore {
        val saved = mutableListOf<ConfigTransaction>()

        override suspend fun save(transaction: ConfigTransaction) {
            saved += transaction
        }

        override suspend fun load(id: ConfigTransactionId): ConfigTransaction? =
            saved.lastOrNull { it.id == id }

        override suspend fun delete(id: ConfigTransactionId) {
            // Keep recorded saves; deletion is irrelevant to this test.
        }

        override suspend fun findInterrupted(): List<ConfigTransaction> =
            saved.filter { it.state !in setOf(ConfigTransactionState.COMMITTED, ConfigTransactionState.FAILED) }
    }
}
