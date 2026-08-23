package com.emutune.data.repo

import com.emutune.data.db.GameDao
import com.emutune.data.db.Mappers.toDomain
import com.emutune.data.db.Mappers.toEntity
import com.emutune.data.db.SessionDao
import com.emutune.model.emulator.EmulatorAdapter
import com.emutune.model.emulator.GameIdentity
import com.emutune.model.game.Game
import com.emutune.model.game.GameEdition
import com.emutune.model.config.LaunchResult
import com.emutune.model.route.ExecutionRoute
import com.emutune.model.session.PlaySession
import com.emutune.model.session.PlaySessionSource
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Outcome of attempting to begin a session by launching the game. */
sealed interface LaunchOutcome {
    data object Launched : LaunchOutcome

    /** Launch was unavailable or failed — the caller should offer the manual path. */
    data class Unavailable(val reason: String) : LaunchOutcome
}

/** A session joined with its game/edition for display on the Home screen. */
data class NowPlaying(
    val session: PlaySession,
    val game: Game?,
    val edition: GameEdition?,
)

/**
 * Owns the current play session — the app's notion of "the game I'm on". It can only
 * be established by launch-through ([beginSession]) or by the user declaring it
 * ([markManual]); Android offers no way to passively read another app's current game.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val gameDao: GameDao,
) {

    fun observeCurrent(): Flow<PlaySession?> = sessionDao.observeCurrent().map { it?.toDomain() }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeNowPlaying(): Flow<NowPlaying?> = sessionDao.observeCurrent().flatMapLatest { session ->
        if (session == null) {
            flowOf(null)
        } else {
            gameDao.editionById(session.gameEditionId).flatMapLatest { edition ->
                val editionDomain = edition?.toDomain()
                if (editionDomain == null) {
                    flowOf(NowPlaying(session.toDomain(), null, null))
                } else {
                    gameDao.gameById(editionDomain.gameId.value)
                        .map { game -> NowPlaying(session.toDomain(), game?.toDomain(), editionDomain) }
                }
            }
        }
    }

    /**
     * Attempts to launch the game through its route's adapter. On success the session
     * is recorded as [PlaySessionSource.LAUNCHED]; on failure no session is recorded and
     * the reason is returned so the UI can offer the manual path.
     */
    suspend fun beginSession(
        game: Game,
        edition: GameEdition,
        route: ExecutionRoute,
        adapter: EmulatorAdapter?,
    ): LaunchOutcome {
        if (adapter == null) return LaunchOutcome.Unavailable("No adapter supports this emulator")

        val identity = GameIdentity(
            gameId = game.id,
            editionId = edition.id,
            title = game.title,
            platformId = edition.platformId,
            platformIdentifier = edition.platformIdentifier,
        )

        return when (adapter.launch(identity, route)) {
            is LaunchResult.Launched -> {
                save(edition, route, PlaySessionSource.LAUNCHED)
                LaunchOutcome.Launched
            }
            is LaunchResult.NotSupported -> LaunchOutcome.Unavailable("Launch is not supported for this emulator")
            is LaunchResult.Failed -> LaunchOutcome.Unavailable("Launch failed")
        }
    }

    /** Records the session as declared by the user (the fallback when launch is unavailable). */
    suspend fun markManual(edition: GameEdition, route: ExecutionRoute) {
        save(edition, route, PlaySessionSource.MANUAL)
    }

    suspend fun clear() = sessionDao.clear()

    private suspend fun save(edition: GameEdition, route: ExecutionRoute, source: PlaySessionSource) {
        sessionDao.upsert(
            PlaySession(
                gameEditionId = edition.id,
                routeId = route.id,
                source = source,
                startedAt = Instant.now(),
            ).toEntity(),
        )
    }
}
