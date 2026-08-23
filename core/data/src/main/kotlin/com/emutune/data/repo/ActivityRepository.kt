package com.emutune.data.repo

import com.emutune.data.db.ActivityDao
import com.emutune.data.db.Mappers.toDomain
import com.emutune.data.db.Mappers.toEntity
import com.emutune.model.activity.ActivityEvent
import com.emutune.model.activity.ActivityEventType
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Owns the curated activity feed — meaningful domain events, never raw logs. */
@Singleton
class ActivityRepository @Inject constructor(
    private val activityDao: ActivityDao,
) {

    fun observeAll(): Flow<List<ActivityEvent>> =
        activityDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun record(
        type: ActivityEventType,
        title: String,
        detail: String? = null,
        gameId: Long? = null,
    ) {
        activityDao.insert(
            ActivityEvent(
                type = type,
                title = title,
                detail = detail,
                gameId = gameId,
                occurredAt = Instant.now(),
            ).toEntity(),
        )
    }
}
