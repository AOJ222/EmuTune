package com.emutune.model.session

import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import java.time.Instant

/**
 * The current play session: what the user is (or was) playing and how that was
 * established. EmuTune cannot read another app's foreground game, so the session is
 * how it knows "this is the game I'm on" — either by launching the game itself
 * ([PlaySessionSource.LAUNCHED]) or by the user declaring it ([PlaySessionSource.MANUAL]).
 */
data class PlaySession(
    val gameEditionId: GameEditionId,
    val routeId: ExecutionRouteId?,
    val source: PlaySessionSource,
    val startedAt: Instant,
)

enum class PlaySessionSource {
    /** EmuTune launched the game through the emulator adapter. */
    LAUNCHED,

    /** Launch was unavailable or failed and the user marked the game as playing. */
    MANUAL,
}
