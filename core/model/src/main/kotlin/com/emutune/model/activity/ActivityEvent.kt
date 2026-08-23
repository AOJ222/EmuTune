package com.emutune.model.activity

import com.emutune.model.ids.ActivityEventId
import java.time.Instant

/**
 * A curated, user-facing event in the app's history. This is not a raw log line — each
 * entry is a single meaningful domain occurrence (a game was added, a measurement
 * completed, a recommendation changed) with just enough context to read at a glance.
 */
data class ActivityEvent(
    val id: ActivityEventId = ActivityEventId(0),
    val type: ActivityEventType,
    val title: String,
    val detail: String? = null,
    val gameId: Long? = null,
    val occurredAt: Instant,
)

/** The closed taxonomy of activity events. Screens never invent their own. */
enum class ActivityEventType {
    GAME_ADDED,
    MEASUREMENT_COMPLETED,
    RECOMMENDATION_CHANGED,
    OPTIMISATION_STARTED,
    OPTIMISATION_COMPLETED,
    OPTIMISATION_ROLLED_BACK,
    ROUTE_CHANGED,
}
