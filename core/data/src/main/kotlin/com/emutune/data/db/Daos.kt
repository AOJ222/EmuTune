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

    @Query("SELECT * FROM execution_routes WHERE gameEditionId = :editionId")
    fun routesFor(editionId: Long): Flow<List<ExecutionRouteEntity>>

    @Query("SELECT * FROM execution_routes")
    fun allRoutes(): Flow<List<ExecutionRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGames(entities: List<GameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEditions(entities: List<GameEditionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutes(entities: List<ExecutionRouteEntity>)
}

@Dao
interface ObservationDao {
    @Query("SELECT * FROM observations")
    fun observeAll(): Flow<List<ObservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ObservationEntity>)
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
