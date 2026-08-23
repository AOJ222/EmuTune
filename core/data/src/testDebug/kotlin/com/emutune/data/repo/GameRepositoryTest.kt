package com.emutune.data.repo

import com.emutune.data.db.GameDao
import com.emutune.data.db.GameEditionEntity
import com.emutune.data.db.GameEntity
import com.emutune.data.db.ExecutionRouteEntity
import com.emutune.model.game.GameEdition
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.GameId
import com.emutune.model.ids.PlatformId
import com.emutune.model.route.ExecutionRoute
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameRepositoryTest {

    @Test
    fun `addGame links edition to game and route to edition`() = runTest {
        val dao = RecordingGameDao()
        val repo = GameRepository(dao)

        val gameId = repo.addGame(
            game = newGame("Web of Shadows"),
            edition = GameEdition(
                id = GameEditionId(0),
                gameId = GameId(0),
                platformId = PlatformId("ps3"),
                name = "PlayStation 3",
                platformIdentifier = "BLUS30123",
            ),
            route = ExecutionRoute(
                id = com.emutune.model.ids.ExecutionRouteId(0),
                gameEditionId = GameEditionId(0),
                platformId = PlatformId("ps3"),
                emulatorId = com.emutune.model.ids.EmulatorId("armsx3"),
            ),
        )

        assertEquals(GameId(1), gameId)
        assertEquals(1, dao.games.size)
        assertEquals(1, dao.editions.size)
        assertEquals(1, dao.routes.size)

        assertEquals(gameId.value, dao.editions.single().gameId)
        assertEquals(dao.editions.single().id, dao.routes.single().gameEditionId)
    }

    @Test
    fun `findByTitle is case insensitive`() = runTest {
        val dao = RecordingGameDao()
        dao.games += GameEntity(id = 1, title = "Web of Shadows", sortKey = "web of shadows", createdAt = 0L)
        val repo = GameRepository(dao)

        assertNotNull(repo.findByTitle("  WEB of Shadows  "))
    }

    @Test
    fun `findByTitle returns null when absent`() = runTest {
        val repo = GameRepository(RecordingGameDao())
        assertNull(repo.findByTitle("Nonexistent"))
    }

    @Test
    fun `deleteGame removes game editions and routes`() = runTest {
        val dao = RecordingGameDao()
        dao.games += GameEntity(id = 1, title = "Web of Shadows", sortKey = "web of shadows", createdAt = 0L)
        dao.editions += GameEditionEntity(id = 10, gameId = 1, platformId = "ps3", name = "PS3", region = null, platformIdentifier = null)
        dao.routes += ExecutionRouteEntity(
            id = 20, gameEditionId = 10, platformId = "ps3", emulatorId = "armsx3",
            emulatorBuildId = null, gpuDriverId = null, translationLayerId = null, configurationId = null,
        )
        val repo = GameRepository(dao)

        repo.deleteGame(GameId(1))

        assertTrue(dao.games.isEmpty())
        assertTrue(dao.editions.isEmpty())
        assertTrue(dao.routes.isEmpty())
    }

    /** Mimics Room's autoGenerate + the queries [GameRepository] relies on. */
    private class RecordingGameDao : GameDao {
        val games = mutableListOf<GameEntity>()
        val editions = mutableListOf<GameEditionEntity>()
        val routes = mutableListOf<ExecutionRouteEntity>()
        private var nextId = 1L

        override fun observeGames() = flowOf(games.toList())
        override fun editionsFor(gameId: Long) = flowOf(editions.filter { it.gameId == gameId })
        override fun editionById(id: Long) = flowOf(editions.firstOrNull { it.id == id })
        override fun gameById(id: Long) = flowOf(games.firstOrNull { it.id == id })
        override fun routesFor(editionId: Long) = flowOf(routes.filter { it.gameEditionId == editionId })
        override fun allRoutes() = flowOf(routes.toList())

        override suspend fun upsertGames(entities: List<GameEntity>) { games += entities }
        override suspend fun upsertEditions(entities: List<GameEditionEntity>) { editions += entities }
        override suspend fun upsertRoutes(entities: List<ExecutionRouteEntity>) { routes += entities }

        override suspend fun findBySortKey(sortKey: String) = games.firstOrNull { it.sortKey == sortKey }
        override suspend fun gameByIdOnce(id: Long) = games.firstOrNull { it.id == id }

        override suspend fun insertGame(entity: GameEntity): Long {
            val id = entity.id.takeIf { it != 0L } ?: nextId++
            games += entity.copy(id = id)
            return id
        }

        override suspend fun insertEdition(entity: GameEditionEntity): Long {
            val id = entity.id.takeIf { it != 0L } ?: nextId++
            editions += entity.copy(id = id)
            return id
        }

        override suspend fun insertRoute(entity: ExecutionRouteEntity): Long {
            val id = entity.id.takeIf { it != 0L } ?: nextId++
            routes += entity.copy(id = id)
            return id
        }

        override suspend fun deleteGame(id: Long) { games.removeAll { it.id == id } }
        override suspend fun deleteEditionsFor(gameId: Long) { editions.removeAll { it.gameId == gameId } }
        override suspend fun deleteRoutesFor(gameId: Long) {
            val editionIds = editions.filter { it.gameId == gameId }.map { it.id }.toSet()
            routes.removeAll { it.gameEditionId in editionIds }
        }
    }
}
