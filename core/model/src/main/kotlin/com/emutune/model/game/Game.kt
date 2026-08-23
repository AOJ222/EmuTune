package com.emutune.model.game

import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.GameId
import com.emutune.model.ids.PlatformId
import java.time.Instant

/**
 * A game is the top-level curated entity. It owns editions, never the reverse.
 * The title is canonical; presentation sorting uses [sortKey] so "The Legend of
 * Zelda" sorts under "legend" rather than "the".
 */
data class Game(
    val id: GameId,
    val title: String,
    val sortKey: String,
    val createdAt: Instant,
)

/**
 * A distinct release of a game on one platform. A PC edition and a Wii edition of
 * the same title are different [GameEdition]s and are never assumed equivalent —
 * see [EditionRelationship].
 *
 * [platformIdentifier] is the edition's native serial/id on [platformId] (for Dolphin,
 * its short game id such as `RMCE01`). It is what a launch-through needs to deep-link
 * into the game; absent until a real, verified identifier is curated.
 */
data class GameEdition(
    val id: GameEditionId,
    val gameId: GameId,
    val platformId: PlatformId,
    val name: String,
    val region: String? = null,
    val platformIdentifier: String? = null,
)

/**
 * Whether two editions may legitimately be compared when ranking routes.
 *
 * [MATERIALLY_DIFFERENT] and [DEMAKE] editions must not be ranked against the
 * primary edition's routes — a Wii build reaching 60 FPS does not "defeat" a
 * materially richer PC build.
 */
enum class EditionRelationship {
    SAME_CORE_EDITION,
    CLOSE_EQUIVALENT,
    PORT,
    MATERIALLY_DIFFERENT,
    DEMAKE,
}

/**
 * A directed statement that [sourceEditionId] relates to [targetEditionId] in the
 * given [relationship]. Stored as separate records so many-to-many comparisons stay
 * explicit and auditable.
 */
data class EditionRelationshipRecord(
    val sourceEditionId: GameEditionId,
    val targetEditionId: GameEditionId,
    val relationship: EditionRelationship,
)

data class Platform(
    val id: PlatformId,
    val name: String,
    val family: PlatformFamily,
)

enum class PlatformFamily {
    DESKTOP,
    HOME_CONSOLE,
    HANDHELD,
    ARCADE,
    MOBILE,
}
