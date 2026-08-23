package com.emutune.data.benchmark

import com.emutune.data.db.DeviceDao
import com.emutune.data.db.DeviceProfileEntity
import com.emutune.data.db.EmulatorInstallationEntity
import com.emutune.data.db.ObservationDao
import com.emutune.data.db.ObservationEntity
import com.emutune.data.repo.ObservationRepository
import com.emutune.model.device.DeviceFingerprint
import com.emutune.model.evidence.BenchmarkMetrics
import com.emutune.model.evidence.EvidenceGrade
import com.emutune.model.ids.DeviceFingerprintId
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MeasurementRecorderTest {

    @Test
    fun `record persists an observation with route device and grade identity`() = runTest {
        val observationDao = RecordingObservationDao()
        val recorder = MeasurementRecorder(
            observationRepository = ObservationRepository(observationDao),
            deviceDao = RecordingDeviceDao(),
        )

        val id = recorder.record(
            gameEditionId = GameEditionId(7),
            executionRouteId = ExecutionRouteId(9),
            metrics = BenchmarkMetrics(averageFps = 26.8, onePercentLowFps = 21.3),
            providerId = "screen-capture-frame-delta",
            durationSeconds = 10,
        )

        assertEquals(1L, id.value)
        val persisted = observationDao.observations.single()
        assertEquals(7L, persisted.gameEditionId)
        assertEquals(9L, persisted.executionRouteId)
        assertEquals("screen-capture-frame-delta", persisted.providerId)
        assertEquals(EvidenceGrade.C_PARTIAL_MEASUREMENT.name, persisted.evidenceGrade)
        assertTrue(persisted.success)
    }

    @Test
    fun `record defaults to partial measurement evidence`() = runTest {
        val observationDao = RecordingObservationDao()
        val recorder = MeasurementRecorder(
            observationRepository = ObservationRepository(observationDao),
            deviceDao = RecordingDeviceDao(),
        )

        recorder.record(
            gameEditionId = GameEditionId(1),
            executionRouteId = ExecutionRouteId(1),
            metrics = BenchmarkMetrics(averageFps = 30.0),
            providerId = "screen-capture-frame-delta",
        )

        assertEquals("PARTIAL_MEASUREMENT", observationDao.observations.single().benchmarkMethod)
    }

    private class RecordingObservationDao : ObservationDao {
        val observations = mutableListOf<ObservationEntity>()
        private var nextId = 1L

        override fun observeAll() = flowOf(observations.toList())
        override fun observeForEdition(editionId: Long) = flowOf(observations.filter { it.gameEditionId == editionId })
        override suspend fun upsertAll(entities: List<ObservationEntity>) { observations += entities }
        override suspend fun insert(entity: ObservationEntity): Long {
            val id = entity.id.takeIf { it != 0L } ?: nextId++
            observations += entity.copy(id = id)
            return id
        }
    }

    private class RecordingDeviceDao : DeviceDao {
        override fun observeProfile() = flowOf(
            DeviceProfileEntity(
                id = 1,
                fingerprintJson = """{"id":1,"manufacturer":"AYN","model":"Thor Max"}""",
            ),
        )
        override suspend fun saveProfile(entity: DeviceProfileEntity) = Unit
        override fun observeInstallations() = flowOf<List<EmulatorInstallationEntity>>(emptyList())
        override suspend fun saveInstallations(entities: List<EmulatorInstallationEntity>) = Unit
    }
}
