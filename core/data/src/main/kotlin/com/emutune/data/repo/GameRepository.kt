package com.emutune.data.repo

import com.emutune.data.db.GameDao
import com.emutune.data.db.Mappers.toDomain
import com.emutune.data.db.Mappers.toEntity
import com.emutune.model.game.Game
import com.emutune.model.game.GameEdition
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.GameId
import com.emutune.model.route.ExecutionRoute
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Owns games, editions and execution routes — the canonical game collection. */
@Singleton
class GameRepository @Inject constructor(
    private val gameDao: GameDao,
) {

    fun observeGames(): Flow<List<Game>> = gameDao.observeGames().map { list -> list.map { it.toDomain() } }

    fun observeEditions(gameId: GameId): Flow<List<GameEdition>> =
        gameDao.editionsFor(gameId.value).map { list -> list.map { it.toDomain() } }

    fun observeRoutes(editionId: GameEditionId): Flow<List<ExecutionRoute>> =
        gameDao.routesFor(editionId.value).map { list -> list.map { it.toDomain() } }

    fun observeAllRoutes(): Flow<List<ExecutionRoute>> =
        gameDao.allRoutes().map { list -> list.map { it.toDomain() } }

    suspend fun upsertGames(games: List<Game>) = gameDao.upsertGames(games.map { it.toEntity() })

    suspend fun upsertEditions(editions: List<GameEdition>) =
        gameDao.upsertEditions(editions.map { it.toEntity() })

    suspend fun upsertRoutes(routes: List<ExecutionRoute>) =
        gameDao.upsertRoutes(routes.map { it.toEntity() })

    /**
     * Adds a game, its edition and (optionally) a route, linking the generated ids so
     * edition → game and route → edition are consistent. Returns the new game id.
     *
     * Duplicate detection is the caller's responsibility via [findByTitle]; this method
     * does not silently merge or overwrite an existing game.
     */
    suspend fun addGame(game: Game, edition: GameEdition, route: ExecutionRoute?): GameId {
        val gameId = GameId(gameDao.insertGame(game.toEntity()))
        val editionId = GameEditionId(
            gameDao.insertEdition(edition.copy(gameId = gameId).toEntity()),
        )
        if (route != null) {
            gameDao.insertRoute(route.copy(gameEditionId = editionId).toEntity())
        }
        return gameId
    }

    /** Case-insensitive title lookup used to reject duplicate additions. */
    suspend fun findByTitle(title: String): Game? =
        gameDao.findBySortKey(normalizeSortKey(title))?.toDomain()

    suspend fun deleteGame(gameId: GameId) {
        gameDao.deleteRoutesFor(gameId.value)
        gameDao.deleteEditionsFor(gameId.value)
        gameDao.deleteGame(gameId.value)
    }

    suspend fun gameById(gameId: GameId): Game? = gameDao.gameByIdOnce(gameId.value)?.toDomain()

    fun normalizeSortKey(title: String): String = title.trim().lowercase()
}

/** Creates a [Game] with a canonical sort key and now timestamp. */
fun newGame(title: String): Game = Game(
    id = GameId(0),
    title = title.trim(),
    sortKey = title.trim().lowercase(),
    createdAt = Instant.now(),
)
