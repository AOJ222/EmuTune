package com.emutune.model.route

import com.emutune.model.ids.ConfigurationId
import com.emutune.model.ids.EmulatorBuildId
import com.emutune.model.ids.EmulatorId
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.GpuDriverId
import com.emutune.model.ids.PlatformId
import com.emutune.model.ids.TranslationLayerId
import java.time.Instant

/**
 * The central domain object. An [ExecutionRoute] describes exactly how one game
 * edition is executed: which emulator/runtime, which build, which driver and
 * translation layer, and which configuration. It is *not* "recommended settings";
 * it is the precise, identity-bearing combination the evidence attaches to.
 */
data class ExecutionRoute(
    val id: ExecutionRouteId,
    val gameEditionId: GameEditionId,
    val platformId: PlatformId,
    val emulatorId: EmulatorId,
    val emulatorBuildId: EmulatorBuildId? = null,
    val gpuDriverId: GpuDriverId? = null,
    val translationLayerId: TranslationLayerId? = null,
    val configurationId: ConfigurationId? = null,
)

/**
 * A curated emulator identity owned by the emulator registry. Package identifiers
 * live here — and only here — so package detection, adapters and UI all reference
 * this one record rather than re-declaring package names.
 */
data class Emulator(
    val id: EmulatorId,
    val displayName: String,
    val packageIds: List<String>,
    val supportedPlatformIds: List<PlatformId>,
    val adapterId: EmulatorId? = null,
    val channels: List<EmulatorChannel> = emptyList(),
)

data class EmulatorChannel(
    val id: String,
    val name: String,
    val packageId: String,
)

/**
 * A specific released build of an emulator. Kept separate from [Emulator] because
 * evidence is tied to a build — when a new build ships, old observations remain
 * attached to the old build rather than being mutated.
 */
data class EmulatorBuild(
    val id: EmulatorBuildId,
    val emulatorId: EmulatorId,
    val version: String,
    val channel: String,
    val releasedAt: Instant? = null,
)

/** A selectable GPU driver package (e.g. an Adreno Turnip build). */
data class GpuDriver(
    val id: GpuDriverId,
    val displayName: String,
    val vendor: String? = null,
)

/** A translation layer (e.g. Winlator's Box64) when a route uses one. */
data class TranslationLayer(
    val id: TranslationLayerId,
    val displayName: String,
)

/**
 * A typed configuration applied to a route. Values are never a raw `Map<String, String>`;
 * they are a map of [com.emutune.model.config.ConfigKey] to a typed
 * [com.emutune.model.config.ConfigValue].
 */
data class Configuration(
    val id: ConfigurationId,
    val routeId: ExecutionRouteId,
    val schemaVersion: Int,
    val fields: Map<com.emutune.model.config.ConfigKey, com.emutune.model.config.ConfigValue>,
)
