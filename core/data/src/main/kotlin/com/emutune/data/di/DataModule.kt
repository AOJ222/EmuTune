package com.emutune.data.di

import android.content.Context
import androidx.room.Room
import com.emutune.data.config.ConfigTransactionStore
import com.emutune.data.config.InMemoryConfigTransactionStore
import com.emutune.data.db.DeviceDao
import com.emutune.data.db.EmuTuneDatabase
import com.emutune.data.db.GameDao
import com.emutune.data.db.ObservationDao
import com.emutune.data.db.SessionDao
import com.emutune.data.seed.DemoDataSeeder
import com.emutune.model.recommendation.RecommendationEngine
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EmuTuneDatabase =
        Room.databaseBuilder(context, EmuTuneDatabase::class.java, "emutune.db").build()

    @Provides
    fun provideGameDao(db: EmuTuneDatabase): GameDao = db.gameDao()

    @Provides
    fun provideObservationDao(db: EmuTuneDatabase): ObservationDao = db.observationDao()

    @Provides
    fun provideDeviceDao(db: EmuTuneDatabase): DeviceDao = db.deviceDao()

    @Provides
    fun provideSessionDao(db: EmuTuneDatabase): SessionDao = db.sessionDao()

    @Provides
    @Singleton
    fun provideRecommendationEngine(): RecommendationEngine = RecommendationEngine()

    @Provides
    @Singleton
    fun provideConfigTransactionStore(): ConfigTransactionStore = InMemoryConfigTransactionStore()
}

/**
 * Declares an optional demo-data seeder. The debug build supplies a concrete binding;
 * the release build has none, so release cannot resolve seeded evidence.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SeederModule {
    @BindsOptionalOf
    abstract fun optionalDemoDataSeeder(): DemoDataSeeder
}
