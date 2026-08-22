package com.emutune.data.emulator

import com.emutune.model.config.ApplyConfigResult
import com.emutune.model.config.CandidateConfig
import com.emutune.model.config.ConfigReadResult
import com.emutune.model.config.ConfigSnapshot
import com.emutune.model.config.ConfigValidationResult
import com.emutune.model.config.ConfigVerificationResult
import com.emutune.model.config.LaunchResult
import com.emutune.model.config.RestoreConfigResult
import com.emutune.model.emulator.EmulatorAdapter
import com.emutune.model.emulator.GameIdentity
import com.emutune.model.ids.EmulatorId
import com.emutune.model.route.EmulatorCapability
import com.emutune.model.route.EmulatorIdentity
import com.emutune.model.route.EmulatorInstallation
import com.emutune.model.route.ExecutionRoute
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The honest Dolphin adapter. It only claims capabilities proven by upstream research;
 * every uncertain surface fails closed as NotSupported. Because a third-party Android
 * app cannot read or write Dolphin's app-private configuration without root, this
 * adapter exposes guided configuration rather than automatic mutation.
 */
@Singleton
class DolphinAdapter @Inject constructor() : EmulatorAdapter {

    override val identity = EmulatorIdentity(
        id = EmulatorId("dolphin"),
        displayName = "Dolphin",
        packageIds = listOf("org.dolphinemu.dolphinemu"),
    )

    override suspend fun detectInstallation(): EmulatorInstallation? = null

    override suspend fun detectCapabilities(installation: EmulatorInstallation): Set<EmulatorCapability> =
        setOf(
            EmulatorCapability.INSTALLATION_DETECTION,
            EmulatorCapability.GAME_LAUNCH,
            EmulatorCapability.GUIDED_CONFIG,
        )

    override suspend fun readConfiguration(game: GameIdentity?): ConfigReadResult =
        ConfigReadResult.NotSupported("Dolphin stores its configuration in app-private storage, inaccessible without root")

    override suspend fun validateConfiguration(candidate: CandidateConfig): ConfigValidationResult =
        ConfigValidationResult.Unsupported("Dolphin configuration cannot be applied externally")

    override suspend fun applyConfiguration(candidate: CandidateConfig): ApplyConfigResult =
        ApplyConfigResult.NotSupported("CONFIG_WRITE is not supported for Dolphin")

    override suspend fun verifyAppliedConfiguration(expected: CandidateConfig): ConfigVerificationResult =
        ConfigVerificationResult.Failed("CONFIG_WRITE is not supported for Dolphin")

    override suspend fun restoreConfiguration(snapshot: ConfigSnapshot): RestoreConfigResult =
        RestoreConfigResult.Failed("No configuration was written to restore")

    override suspend fun launch(route: ExecutionRoute): LaunchResult =
        // GAME_LAUNCH is confirmed via the `dolphinemu://app/play/<channelId>/<gameId>`
        // deep link, but the route does not yet carry Dolphin's per-game library id.
        LaunchResult.NotSupported("Requires the game's Dolphin library id, not yet modelled on the route")
}
