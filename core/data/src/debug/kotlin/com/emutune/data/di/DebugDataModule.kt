package com.emutune.data.di

import com.emutune.data.seed.DebugDemoDataSeeder
import com.emutune.data.seed.DemoDataSeeder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug-only binding that fills the optional [DemoDataSeeder] declared in [SeederModule]. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DebugDataModule {
    @Binds
    abstract fun bindDemoDataSeeder(impl: DebugDemoDataSeeder): DemoDataSeeder
}
