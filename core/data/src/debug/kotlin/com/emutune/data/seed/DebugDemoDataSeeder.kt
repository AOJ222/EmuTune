package com.emutune.data.seed

import com.emutune.data.db.Mappers.toEntity
import com.emutune.data.db.ObservationDao
import com.emutune.data.repo.GameRepository
import com.emutune.model.device.DeviceEnvironment
import com.emutune.model.evidence.BenchmarkMetrics
import com.emutune.model.evidence.BenchmarkMethod
import com.emutune.model.evidence.EvidenceGrade
import com.emutune.model.evidence.Observation
import com.emutune.model.evidence.RenderingCorrectness
import com.emutune.model.evidence.VisualQuality
import com.emutune.model.game.Game
import com.emutune.model.game.GameEdition
import com.emutune.model.ids.DeviceFingerprintId
import com.emutune.model.ids.EmulatorBuildId
import com.emutune.model.ids.EmulatorId
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.GameId
import com.emutune.model.ids.ObservationId
import com.emutune.model.ids.PlatformId
import com.emutune.model.route.ExecutionRoute
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Debug-only seeder that populates a demo game, its editions, routes and measured
 * observations so the full recommendation flow renders without a real emulator. This
 * class lives in the debug source set and is never compiled into release.
 */
class DebugDemoDataSeeder @Inject constructor(
    private val gameRepository: GameRepository,
    private val observationDao: ObservationDao,
) : DemoDataSeeder {

    override suspend fun seedIfEmpty() {
        if (gameRepository.observeGames().first().isNotEmpty()) return

        val game = Game(
            id = GameId(1),
            title = "Spider-Man: Web of Shadows",
            sortKey = "spider-man: web of shadows",
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )
        val editions = listOf(
            GameEdition(GameEditionId(1), GameId(1), PlatformId("pc"), "PC"),
            GameEdition(GameEditionId(2), GameId(1), PlatformId("ps3"), "PlayStation 3"),
            GameEdition(GameEditionId(3), GameId(1), PlatformId("wii"), "Wii"),
        )
        val routes = listOf(
            ExecutionRoute(
                id = ExecutionRouteId(1),
                gameEditionId = GameEditionId(1),
                platformId = PlatformId("pc"),
                emulatorId = EmulatorId("gamenative"),
                emulatorBuildId = EmulatorBuildId("build-200"),
            ),
            ExecutionRoute(
                id = ExecutionRouteId(2),
                gameEditionId = GameEditionId(1),
                platformId = PlatformId("pc"),
                emulatorId = EmulatorId("winlator"),
                emulatorBuildId = EmulatorBuildId("build-100"),
            ),
            ExecutionRoute(
                id = ExecutionRouteId(3),
                gameEditionId = GameEditionId(2),
                platformId = PlatformId("ps3"),
                emulatorId = EmulatorId("armsx3"),
            ),
            ExecutionRoute(
                id = ExecutionRouteId(4),
                gameEditionId = GameEditionId(3),
                platformId = PlatformId("wii"),
                emulatorId = EmulatorId("dolphin"),
            ),
        )
        gameRepository.upsertGames(listOf(game))
        gameRepository.upsertEditions(editions)
        gameRepository.upsertRoutes(routes)

        val now = Instant.parse("2026-08-20T00:00:00Z")
        val observations = listOf(
            Observation(
                id = ObservationId(1),
                gameEditionId = GameEditionId(1),
                executionRouteId = ExecutionRouteId(1),
                environment = DeviceEnvironment(DeviceFingerprintId(1)),
                metrics = BenchmarkMetrics(
                    averageFps = 47.8,
                    onePercentLowFps = 39.2,
                    sustainedFps = 46.0,
                    thermalDegradation = 0.05,
                    visualQuality = VisualQuality.NATIVE,
                    renderingCorrectness = RenderingCorrectness.CORRECT,
                ),
                durationSeconds = 900,
                benchmarkMethod = BenchmarkMethod.MACHINE_GAMEPLAY,
                evidenceGrade = EvidenceGrade.B_MACHINE_GAMEPLAY,
                recordedAt = now,
            ),
            Observation(
                id = ObservationId(2),
                gameEditionId = GameEditionId(1),
                executionRouteId = ExecutionRouteId(1),
                environment = DeviceEnvironment(DeviceFingerprintId(1)),
                metrics = BenchmarkMetrics(
                    averageFps = 48.2,
                    onePercentLowFps = 39.4,
                    sustainedFps = 46.5,
                ),
                durationSeconds = 600,
                benchmarkMethod = BenchmarkMethod.MACHINE_BENCHMARK,
                evidenceGrade = EvidenceGrade.A_DETERMINISTIC_BENCHMARK,
                recordedAt = now,
            ),
            Observation(
                id = ObservationId(3),
                gameEditionId = GameEditionId(1),
                executionRouteId = ExecutionRouteId(2),
                environment = DeviceEnvironment(DeviceFingerprintId(1)),
                metrics = BenchmarkMetrics(
                    averageFps = 27.1,
                    onePercentLowFps = 22.0,
                    sustainedFps = 26.0,
                    thermalDegradation = 0.12,
                ),
                durationSeconds = 600,
                benchmarkMethod = BenchmarkMethod.MACHINE_GAMEPLAY,
                evidenceGrade = EvidenceGrade.B_MACHINE_GAMEPLAY,
                recordedAt = now,
            ),
        )
        observationDao.upsertAll(observations.map { it.toEntity() })
    }
}
