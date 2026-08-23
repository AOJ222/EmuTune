package com.emutune.data.benchmark

import com.emutune.data.db.DeviceDao
import com.emutune.data.db.Mappers.toDomain
import com.emutune.data.repo.ObservationRepository
import com.emutune.model.device.DeviceEnvironment
import com.emutune.model.evidence.BenchmarkMethod
import com.emutune.model.evidence.BenchmarkMetrics
import com.emutune.model.evidence.EvidenceGrade
import com.emutune.model.evidence.Observation
import com.emutune.model.ids.DeviceFingerprintId
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.ObservationId
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Turns a completed measurement into a persisted [Observation]. This is the conversion
 * that closes the loop — a successful screen-capture measurement becomes immutable
 * evidence, which the recommendation engine then re-reads.
 *
 * A cancelled or failed measurement never reaches this recorder, so it can never
 * become evidence. The evidence grade defaults to [EvidenceGrade.C_PARTIAL_MEASUREMENT]
 * (screen-capture observes displayed updates, not internal render telemetry) but the
 * provider's declared grade takes precedence.
 */
@Singleton
class MeasurementRecorder @Inject constructor(
    private val observationRepository: ObservationRepository,
    private val deviceDao: DeviceDao,
) {

    suspend fun record(
        gameEditionId: GameEditionId,
        executionRouteId: ExecutionRouteId,
        metrics: BenchmarkMetrics,
        providerId: String,
        durationSeconds: Int? = null,
        evidenceGrade: EvidenceGrade = EvidenceGrade.C_PARTIAL_MEASUREMENT,
        benchmarkMethod: BenchmarkMethod = BenchmarkMethod.PARTIAL_MEASUREMENT,
    ): ObservationId {
        val profile = deviceDao.observeProfile().first()?.toDomain()
        val deviceId = profile?.id ?: DeviceFingerprintId(1)

        return observationRepository.record(
            Observation(
                gameEditionId = gameEditionId,
                executionRouteId = executionRouteId,
                environment = DeviceEnvironment(deviceFingerprintId = deviceId),
                metrics = metrics,
                durationSeconds = durationSeconds,
                benchmarkMethod = benchmarkMethod,
                evidenceGrade = evidenceGrade,
                success = true,
                recordedAt = Instant.now(),
                providerId = providerId,
            ),
        )
    }
}
