package com.emutune.model.emulator

import com.emutune.model.config.ApplyConfigResult
import com.emutune.model.config.CandidateConfig
import com.emutune.model.config.ConfigReadResult
import com.emutune.model.config.ConfigSnapshot
import com.emutune.model.config.ConfigValidationResult
import com.emutune.model.config.ConfigVerificationResult
import com.emutune.model.config.LaunchResult
import com.emutune.model.config.RestoreConfigResult
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.GameId
import com.emutune.model.ids.PlatformId
import com.emutune.model.route.EmulatorCapability
import com.emutune.model.route.EmulatorIdentity
import com.emutune.model.route.EmulatorInstallation
import com.emutune.model.route.ExecutionRoute

/**
 * A minimal, game-scoped identity handed to adapters so per-game configuration can be
 * addressed without leaking the full game model into integration code.
 */
data class GameIdentity(
    val gameId: GameId,
    val editionId: GameEditionId,
    val title: String,
    val platformId: PlatformId,
)

/**
 * The capability-based contract every emulator/runtime integration implements. No
 * emulator-specific behaviour lives outside this boundary. Each operation returns an
 * explicit result — an unknown or unsupported capability fails closed rather than
 * silently pretending it worked.
 */
interface EmulatorAdapter {
    val identity: EmulatorIdentity

    suspend fun detectInstallation(): EmulatorInstallation?

    suspend fun detectCapabilities(installation: EmulatorInstallation): Set<EmulatorCapability>

    suspend fun readConfiguration(game: GameIdentity?): ConfigReadResult

    suspend fun validateConfiguration(candidate: CandidateConfig): ConfigValidationResult

    suspend fun applyConfiguration(candidate: CandidateConfig): ApplyConfigResult

    suspend fun verifyAppliedConfiguration(expected: CandidateConfig): ConfigVerificationResult

    suspend fun restoreConfiguration(snapshot: ConfigSnapshot): RestoreConfigResult

    suspend fun launch(route: ExecutionRoute): LaunchResult
}
