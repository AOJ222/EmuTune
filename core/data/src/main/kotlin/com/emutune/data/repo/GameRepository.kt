package com.emutune.data.repo

import com.emutune.data.db.GameDao
import com.emutune.data.db.Mappers.toDomain
import com.emutune.data.db.Mappers.toEntity
import com.emutune.model.game.Game
import com.emutune.model.game.GameEdition
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.GameId
import com.emutune.model.route.ExecutionRoute
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
}
