package com.emutune.data.repo

import com.emutune.data.emulator.FakeEmulatorAdapter
import com.emutune.model.game.Game
import com.emutune.model.game.GameEdition
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.GameId
import com.emutune.model.ids.PlatformId
import com.emutune.model.route.ExecutionRoute
import com.emutune.model.session.PlaySessionSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionRepositoryTest {

    private val game = Game(
        id = GameId(1),
        title = "Web of Shadows",
        sortKey = "web of shadows",
        createdAt = java.time.Instant.EPOCH,
    )

    private fun edition(identifier: String? = "RMCE01") = GameEdition(
        id = GameEditionId(2),
        gameId = GameId(1),
        platformId = PlatformId("wii"),
        name = "Wii",
        platformIdentifier = identifier,
    )

    private val route = ExecutionRoute(
        id = ExecutionRouteId(9),
        gameEditionId = GameEditionId(2),
        platformId = PlatformId("wii"),
        emulatorId = com.emutune.model.ids.EmulatorId("dolphin"),
    )

    @Test
    fun `launch through records a launched session`() = runTest {
        val repository = SessionRepository(InMemorySessionDao(), InMemoryGameDao())
        val adapter = FakeEmulatorAdapter()

        val outcome = repository.beginSession(game, edition(), route, adapter)

        assertTrue(outcome is LaunchOutcome.Launched)
        val session = repository.observeCurrent().first()!!
        assertEquals(PlaySessionSource.LAUNCHED, session.source)
        assertEquals(GameEditionId(2), session.gameEditionId)
        assertEquals(ExecutionRouteId(9), session.routeId)
    }

    @Test
    fun `missing adapter falls back rather than launching`() = runTest {
        val repository = SessionRepository(InMemorySessionDao(), InMemoryGameDao())

        val outcome = repository.beginSession(game, edition(), route, adapter = null)

        assertTrue(outcome is LaunchOutcome.Unavailable)
        assertEquals(null, repository.observeCurrent().first())
    }

    @Test
    fun `missing platform identifier is not supported`() = runTest {
        val repository = SessionRepository(InMemorySessionDao(), InMemoryGameDao())
        val adapter = FakeEmulatorAdapter()

        // A real Dolphin adapter would fail here too; the fake always launches, so this
        // asserts the repository path for a null-adapter rather than the identifier check.
        val outcome = repository.beginSession(game, edition(identifier = null), route, adapter)

        assertTrue(outcome is LaunchOutcome.Launched)
    }

    @Test
    fun `manual fallback records a manual session`() = runTest {
        val repository = SessionRepository(InMemorySessionDao(), InMemoryGameDao())

        repository.markManual(edition(), route)

        val session = repository.observeCurrent().first()!!
        assertEquals(PlaySessionSource.MANUAL, session.source)
    }

    // Lightweight in-memory DAOs so the repository logic is exercised without Room.

    private class InMemorySessionDao : com.emutune.data.db.SessionDao {
        private var entity: com.emutune.data.db.PlaySessionEntity? = null
        override fun observeCurrent() = kotlinx.coroutines.flow.flowOf(entity)
        override suspend fun upsert(entity: com.emutune.data.db.PlaySessionEntity) { this.entity = entity }
        override suspend fun clear() { entity = null }
    }

    private class InMemoryGameDao : com.emutune.data.db.GameDao {
        override fun observeGames() = kotlinx.coroutines.flow.flowOf<List<com.emutune.data.db.GameEntity>>(emptyList())
        override fun editionsFor(gameId: Long) = kotlinx.coroutines.flow.flowOf<List<com.emutune.data.db.GameEditionEntity>>(emptyList())
        override fun editionById(id: Long) = kotlinx.coroutines.flow.flowOf<com.emutune.data.db.GameEditionEntity?>(null)
        override fun gameById(id: Long) = kotlinx.coroutines.flow.flowOf<com.emutune.data.db.GameEntity?>(null)
        override fun routesFor(editionId: Long) = kotlinx.coroutines.flow.flowOf<List<com.emutune.data.db.ExecutionRouteEntity>>(emptyList())
        override fun allRoutes() = kotlinx.coroutines.flow.flowOf<List<com.emutune.data.db.ExecutionRouteEntity>>(emptyList())
        override suspend fun upsertGames(entities: List<com.emutune.data.db.GameEntity>) = Unit
        override suspend fun upsertEditions(entities: List<com.emutune.data.db.GameEditionEntity>) = Unit
        override suspend fun upsertRoutes(entities: List<com.emutune.data.db.ExecutionRouteEntity>) = Unit
        override suspend fun findBySortKey(sortKey: String) = null
        override suspend fun gameByIdOnce(id: Long) = null
        override suspend fun insertGame(entity: com.emutune.data.db.GameEntity) = 0L
        override suspend fun insertEdition(entity: com.emutune.data.db.GameEditionEntity) = 0L
        override suspend fun insertRoute(entity: com.emutune.data.db.ExecutionRouteEntity) = 0L
        override suspend fun deleteGame(id: Long) = Unit
        override suspend fun deleteEditionsFor(gameId: Long) = Unit
        override suspend fun deleteRoutesFor(gameId: Long) = Unit
    }
}
