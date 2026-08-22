package com.emutune.model.recommendation

import com.emutune.model.ids.DeviceFingerprintId
import com.emutune.model.evidence.BenchmarkMetrics
import com.emutune.model.evidence.EvidenceGrade
import com.emutune.model.ids.EmulatorBuildId
import com.emutune.model.ids.EmulatorId
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecommendationEngineTest {

    private val engine = RecommendationEngine()

    private fun request(
        editionId: Long,
        goal: OptimizationGoal,
        routes: List<com.emutune.model.route.ExecutionRoute>,
        observations: List<com.emutune.model.evidence.Observation>,
        observationDevices: Map<DeviceFingerprintId, com.emutune.model.device.DeviceFingerprint> =
            mapOf(DeviceFingerprintId(1) to TestFixtures.targetDevice),
        comparableEditionIds: Set<GameEditionId> = emptySet(),
        latestBuilds: Map<EmulatorId, EmulatorBuildId> = emptyMap(),
    ) = RecommendationRequest(
        gameEditionId = GameEditionId(editionId),
        goal = goal,
        routes = routes,
        observations = observations,
        targetDevice = TestFixtures.targetDevice,
        observationDevices = observationDevices,
        comparableEditionIds = comparableEditionIds,
        latestBuilds = latestBuilds,
    )

    @Test
    fun `higher average fps with terrible lows loses under balanced`() {
        val routeA = TestFixtures.route(1, 1)
        val routeB = TestFixtures.route(2, 1)
        val obsA = TestFixtures.observation(
            1, 1, 1, 1,
            BenchmarkMetrics(averageFps = 60.0, onePercentLowFps = 20.0),
        )
        val obsB = TestFixtures.observation(
            2, 1, 2, 1,
            BenchmarkMetrics(averageFps = 48.0, onePercentLowFps = 44.0),
        )

        val recommendation = engine.recommend(
            request(1, OptimizationGoal.BALANCED, listOf(routeA, routeB), listOf(obsA, obsB)),
        )

        assertEquals(ExecutionRouteId(2), recommendation.recommendedRouteId)
    }

    @Test
    fun `max performance prefers highest valid fps`() {
        val routeA = TestFixtures.route(1, 1)
        val routeB = TestFixtures.route(2, 1)
        val obsA = TestFixtures.observation(1, 1, 1, 1, BenchmarkMetrics(averageFps = 58.0))
        val obsB = TestFixtures.observation(2, 1, 2, 1, BenchmarkMetrics(averageFps = 40.0))

        val recommendation = engine.recommend(
            request(1, OptimizationGoal.MAX_PERFORMANCE, listOf(routeA, routeB), listOf(obsA, obsB)),
        )

        assertEquals(ExecutionRouteId(1), recommendation.recommendedRouteId)
    }

    @Test
    fun `stable 30 rejects candidates that cannot sustain target`() {
        val routeA = TestFixtures.route(1, 1)
        val obsA = TestFixtures.observation(1, 1, 1, 1, BenchmarkMetrics(averageFps = 27.0))

        val recommendation = engine.recommend(
            request(1, OptimizationGoal.STABLE_30, listOf(routeA), listOf(obsA)),
        )

        assertEquals(
            DisqualificationReason.FAILS_OPTIMIZATION_TARGET,
            recommendation.evaluations.single().disqualified,
        )
        assertNull(recommendation.recommendedRouteId)
    }

    @Test
    fun `exact device evidence outranks weak hardware match`() {
        val routeA = TestFixtures.route(1, 1)
        val routeB = TestFixtures.route(2, 1)
        val metrics = BenchmarkMetrics(averageFps = 48.0, onePercentLowFps = 40.0)
        val obsA = TestFixtures.observation(1, 1, 1, 1, metrics, grade = EvidenceGrade.A_DETERMINISTIC_BENCHMARK)
        val obsB = TestFixtures.observation(2, 1, 2, 2, metrics, grade = EvidenceGrade.D_USER_SUBMITTED)

        val recommendation = engine.recommend(
            request(
                editionId = 1,
                goal = OptimizationGoal.BALANCED,
                routes = listOf(routeA, routeB),
                observations = listOf(obsA, obsB),
                observationDevices = mapOf(
                    DeviceFingerprintId(1) to TestFixtures.targetDevice,
                    DeviceFingerprintId(2) to TestFixtures.device(2, model = "Other", codename = "other", soc = null),
                ),
            ),
        )

        assertEquals(ExecutionRouteId(1), recommendation.recommendedRouteId)
        val weak = recommendation.evaluations.first { it.executionRouteId == ExecutionRouteId(2) }
        assertEquals(ConfidenceLevel.LOW, weak.confidence.level)
    }

    @Test
    fun `stale emulator build evidence is discounted`() {
        val routeA = TestFixtures.route(1, 1, buildId = "build-200")
        val routeB = TestFixtures.route(2, 1, buildId = "build-100")
        val metrics = BenchmarkMetrics(averageFps = 48.0)
        val obsA = TestFixtures.observation(1, 1, 1, 1, metrics)
        val obsB = TestFixtures.observation(2, 1, 2, 1, metrics)

        val recommendation = engine.recommend(
            request(
                editionId = 1,
                goal = OptimizationGoal.BALANCED,
                routes = listOf(routeA, routeB),
                observations = listOf(obsA, obsB),
                latestBuilds = mapOf(EmulatorId("gamenative") to EmulatorBuildId("build-200")),
            ),
        )

        val current = recommendation.evaluations.first { it.executionRouteId == ExecutionRouteId(1) }
        val stale = recommendation.evaluations.first { it.executionRouteId == ExecutionRouteId(2) }
        assertTrue(stale.confidence.numeric < current.confidence.numeric)
    }

    @Test
    fun `crash results are heavily penalized`() {
        val routeA = TestFixtures.route(1, 1)
        val routeB = TestFixtures.route(2, 1)
        val metrics = BenchmarkMetrics(averageFps = 50.0, onePercentLowFps = 45.0)
        val obsA = TestFixtures.observation(1, 1, 1, 1, metrics, crashCount = 0)
        val obsB = TestFixtures.observation(2, 1, 2, 1, metrics, crashCount = 2)

        val recommendation = engine.recommend(
            request(1, OptimizationGoal.BALANCED, listOf(routeA, routeB), listOf(obsA, obsB)),
        )

        assertEquals(ExecutionRouteId(1), recommendation.recommendedRouteId)
    }

    @Test
    fun `materially different edition is not naively compared`() {
        val pcEdition = GameEditionId(1)
        val wiiEdition = GameEditionId(2)
        val routePc = TestFixtures.route(1, 1)
        val routeWii = TestFixtures.route(2, 2)
        val obsPc = TestFixtures.observation(1, 1, 1, 1, BenchmarkMetrics(averageFps = 40.0))
        val obsWii = TestFixtures.observation(2, 2, 2, 1, BenchmarkMetrics(averageFps = 60.0))

        val recommendation = engine.recommend(
            request(
                editionId = 1,
                goal = OptimizationGoal.BALANCED,
                routes = listOf(routePc, routeWii),
                observations = listOf(obsPc, obsWii),
                comparableEditionIds = emptySet(),
            ),
        )

        assertEquals(ExecutionRouteId(1), recommendation.recommendedRouteId)
        assertEquals(1, recommendation.evaluations.size)
    }
}
