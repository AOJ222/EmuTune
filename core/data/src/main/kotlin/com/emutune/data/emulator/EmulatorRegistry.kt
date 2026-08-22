package com.emutune.data.emulator

import com.emutune.model.game.Platform
import com.emutune.model.game.PlatformFamily
import com.emutune.model.ids.EmulatorId
import com.emutune.model.ids.PlatformId
import com.emutune.model.route.Emulator
import com.emutune.model.route.EmulatorChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The authoritative emulator registry. Package identifiers live here — and only here —
 * so package detection, adapters and the UI all reference this one curated record.
 *
 * Only integrations whose package identifiers have been verified are listed. A new
 * emulator is added here before any adapter or detection code references it.
 */
@Singleton
class EmulatorRegistry @Inject constructor() {

    private val platforms: List<Platform> = listOf(
        Platform(PlatformId("gamecube"), "GameCube", PlatformFamily.HOME_CONSOLE),
        Platform(PlatformId("wii"), "Wii", PlatformFamily.HOME_CONSOLE),
        Platform(PlatformId("pc"), "PC", PlatformFamily.DESKTOP),
        Platform(PlatformId("ps3"), "PlayStation 3", PlatformFamily.HOME_CONSOLE),
    )

    private val emulators: List<Emulator> = listOf(
        Emulator(
            id = EmulatorId("dolphin"),
            displayName = "Dolphin",
            packageIds = listOf("org.dolphinemu.dolphinemu"),
            supportedPlatformIds = listOf(PlatformId("gamecube"), PlatformId("wii")),
            adapterId = EmulatorId("dolphin"),
            channels = listOf(
                EmulatorChannel("stable", "Stable", "org.dolphinemu.dolphinemu"),
            ),
        ),
    )

    fun all(): List<Emulator> = emulators

    fun find(id: EmulatorId): Emulator? = emulators.firstOrNull { it.id == id }

    fun findByPackage(packageId: String): Emulator? =
        emulators.firstOrNull { packageId in it.packageIds }

    fun platforms(): List<Platform> = platforms

    fun platform(id: PlatformId): Platform? = platforms.firstOrNull { it.id == id }
}
