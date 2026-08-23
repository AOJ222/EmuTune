package com.emutune.data.emulator

import com.emutune.model.emulator.EmulatorAdapter
import com.emutune.model.ids.EmulatorId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the [EmulatorAdapter] responsible for an [EmulatorId]. Fail-closed: an id
 * with no adapter returns null, so launch-through reports "no adapter" rather than
 * guessing. Only Dolphin has a real adapter in Milestone 1; the fake adapter is
 * constructed directly by the debug simulation, not resolved here.
 */
@Singleton
class AdapterResolver @Inject constructor(
    private val dolphin: DolphinAdapter,
) {
    fun adapterFor(emulatorId: EmulatorId): EmulatorAdapter? =
        if (emulatorId == dolphin.identity.id) dolphin else null
}
