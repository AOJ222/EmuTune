package com.emutune.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY sortKey")
    fun observeGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM game_editions WHERE gameId = :gameId")
    fun editionsFor(gameId: Long): Flow<List<GameEditionEntity>>

    @Query("SELECT * FROM game_editions WHERE id = :id")
    fun editionById(id: Long): Flow<GameEditionEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    fun gameById(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM execution_routes WHERE gameEditionId = :editionId")
    fun routesFor(editionId: Long): Flow<List<ExecutionRouteEntity>>

    @Query("SELECT * FROM execution_routes")
    fun allRoutes(): Flow<List<ExecutionRouteEntity>>

    @Query("SELECT * FROM games WHERE sortKey = :sortKey LIMIT 1")
    suspend fun findBySortKey(sortKey: String): GameEntity?

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun gameByIdOnce(id: Long): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGames(entities: List<GameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEditions(entities: List<GameEditionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutes(entities: List<ExecutionRouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(entity: GameEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdition(entity: GameEditionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(entity: ExecutionRouteEntity): Long

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGame(id: Long)

    @Query("DELETE FROM game_editions WHERE gameId = :gameId")
    suspend fun deleteEditionsFor(gameId: Long)

    @Query("DELETE FROM execution_routes WHERE gameEditionId IN (SELECT id FROM game_editions WHERE gameId = :gameId)")
    suspend fun deleteRoutesFor(gameId: Long)
}

@Dao
interface ObservationDao {
    @Query("SELECT * FROM observations")
    fun observeAll(): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM observations WHERE gameEditionId = :editionId ORDER BY recordedAt DESC")
    fun observeForEdition(editionId: Long): Flow<List<ObservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ObservationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ObservationEntity): Long
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM device_profile LIMIT 1")
    fun observeProfile(): Flow<DeviceProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(entity: DeviceProfileEntity)

    @Query("SELECT * FROM emulator_installations")
    fun observeInstallations(): Flow<List<EmulatorInstallationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveInstallations(entities: List<EmulatorInstallationEntity>)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM play_session LIMIT 1")
    fun observeCurrent(): Flow<PlaySessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaySessionEntity)

    @Query("DELETE FROM play_session")
    suspend fun clear()
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_events ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<ActivityEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ActivityEventEntity): Long
}
