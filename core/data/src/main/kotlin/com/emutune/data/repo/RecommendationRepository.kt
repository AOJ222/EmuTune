package com.emutune.data.repo

import com.emutune.data.db.DeviceDao
import com.emutune.data.db.GameDao
import com.emutune.data.db.Mappers.toDomain
import com.emutune.data.db.ObservationDao
import com.emutune.model.device.DeviceFingerprint
import com.emutune.model.ids.GameEditionId
import com.emutune.model.recommendation.OptimizationGoal
import com.emutune.model.recommendation.Recommendation
import com.emutune.model.recommendation.RecommendationEngine
import com.emutune.model.recommendation.RecommendationRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Produces deterministic recommendations by gathering current evidence and running it
 * through the [RecommendationEngine]. Recommendations are derived on demand, never
 * persisted as a mutable "best settings" record.
 */
@Singleton
class RecommendationRepository @Inject constructor(
    private val gameDao: GameDao,
    private val observationDao: ObservationDao,
    private val deviceDao: DeviceDao,
    private val engine: RecommendationEngine,
) {

    suspend fun recommendFor(editionId: GameEditionId, goal: OptimizationGoal): Recommendation {
        val profile = deviceDao.observeProfile().first()?.toDomain()
        val targetDevice = profile ?: DeviceFingerprint()
        val routes = gameDao.allRoutes().first().map { it.toDomain() }
        val observations = observationDao.observeAll().first().map { it.toDomain() }

        // Milestone 1 is offline and single-device, so only the current profile is
        // known. Observations carry a deviceFingerprintId, but the fingerprints those
        // ids refer to are not persisted, so every observation that was not recorded on
        // this device currently resolves to UNKNOWN_HARDWARE. Real cross-device hardware
        // matching arrives with the evidence network (Milestone 2+).
        val observationDevices = profile?.let { mapOf(it.id to it) } ?: emptyMap()

        return engine.recommend(
            RecommendationRequest(
                gameEditionId = editionId,
                goal = goal,
                routes = routes,
                observations = observations,
                targetDevice = targetDevice,
                observationDevices = observationDevices,
            ),
        )
    }
}
