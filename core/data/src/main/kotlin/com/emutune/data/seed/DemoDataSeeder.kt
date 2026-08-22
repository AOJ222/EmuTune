package com.emutune.data.seed

/**
 * Seeds demo data (games, editions, routes, observations) for the debug build so the
 * full recommendation flow can be exercised without a real emulator or benchmark run.
 * The release build has no concrete implementation of this interface.
 */
interface DemoDataSeeder {
    suspend fun seedIfEmpty()
}
