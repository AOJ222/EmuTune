package com.emutune.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameEntity::class,
        GameEditionEntity::class,
        ExecutionRouteEntity::class,
        ObservationEntity::class,
        DeviceProfileEntity::class,
        EmulatorInstallationEntity::class,
        PlaySessionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class EmuTuneDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun observationDao(): ObservationDao
    abstract fun deviceDao(): DeviceDao
    abstract fun sessionDao(): SessionDao
}
