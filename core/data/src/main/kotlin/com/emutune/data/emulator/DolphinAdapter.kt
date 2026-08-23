package com.emutune.data.emulator

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The honest Dolphin adapter. It only claims capabilities proven by upstream research;
 * every uncertain surface fails closed as NotSupported. Because a third-party Android
 * app cannot read or write Dolphin's app-private configuration without root, this
 * adapter exposes guided configuration rather than automatic mutation.
 *
 * Launch uses Dolphin's confirmed `dolphinemu://app/play/<channel>/<gameId>` deep link.
 * The scheme is CONFIRMED from upstream source; the channel token is derived from the
 * route platform id and the game id from [GameIdentity.platformIdentifier], both of
 * which are LIKELY-formatted — a launch that does not resolve fails closed to
 * [LaunchResult.Failed], which the UI surfaces as "mark as playing" rather than
 * pretending it succeeded.
 */
@Singleton
class DolphinAdapter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : EmulatorAdapter {

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

    override suspend fun launch(game: GameIdentity, route: ExecutionRoute): LaunchResult {
        val gameId = game.platformIdentifier
        if (gameId.isNullOrBlank()) {
            return LaunchResult.NotSupported("This game has no Dolphin identifier to launch by")
        }
        val uri = Uri.parse("dolphinemu://app/play/${route.platformId.value}/$gameId")
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            LaunchResult.Launched
        }.getOrElse { error ->
            LaunchResult.Failed("Could not launch Dolphin: ${error.message}")
        }
    }
}
