package com.emutune.data.repo

import com.emutune.data.db.Mappers.toDomain
import com.emutune.data.db.Mappers.toEntity
import com.emutune.data.db.ObservationDao
import com.emutune.model.evidence.Observation
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.ObservationId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Owns persistence of observations — the immutable evidence records. A completed
 * measurement becomes an [Observation] through [record]; a cancelled or failed
 * measurement never reaches this repository, so it never becomes evidence.
 */
@Singleton
class ObservationRepository @Inject constructor(
    private val observationDao: ObservationDao,
) {

    fun observeAll(): Flow<List<Observation>> =
        observationDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeForEdition(editionId: GameEditionId): Flow<List<Observation>> =
        observationDao.observeForEdition(editionId.value).map { list -> list.map { it.toDomain() } }

    suspend fun record(observation: Observation): ObservationId =
        ObservationId(observationDao.insert(observation.toEntity()))
}
