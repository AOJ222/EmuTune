package com.emutune.data.emulator

import android.content.Context
import android.content.pm.PackageManager
import com.emutune.model.route.EmulatorInstallation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects installed supported emulators using the package identifiers owned by
 * [EmulatorRegistry]. Detection relies on manifest `<queries>` declarations, never on
 * `QUERY_ALL_PACKAGES`; packages not declared in `<queries>` are invisible and simply
 * report as not installed.
 */
@Singleton
class EmulatorDetector @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val registry: EmulatorRegistry,
) {

    fun detectInstalled(): List<EmulatorInstallation> {
        val packageManager = context.packageManager
        val now = Instant.now()
        return registry.all().flatMap { emulator ->
            emulator.packageIds.mapNotNull { packageId ->
                resolve(packageManager, emulator.id, packageId, now)
            }
        }
    }

    private fun resolve(
        packageManager: PackageManager,
        emulatorId: com.emutune.model.ids.EmulatorId,
        packageId: String,
        now: Instant,
    ): EmulatorInstallation? {
        return runCatching {
            val info = packageManager.getPackageInfo(packageId, 0)
            EmulatorInstallation(
                emulatorId = emulatorId,
                packageId = packageId,
                versionName = info.versionName,
                versionCode = info.longVersionCode,
                detectedAt = now,
            )
        }.getOrNull()
    }
}
