package com.emutune.data.repo

import com.emutune.data.db.DeviceDao
import com.emutune.data.db.Mappers.toDomain
import com.emutune.data.db.Mappers.toProfileEntity
import com.emutune.data.db.Mappers.toEntity
import com.emutune.data.emulator.EmulatorDetector
import com.emutune.data.hardware.DeviceProfiler
import com.emutune.data.hardware.ThermalMonitor
import com.emutune.model.device.DeviceFingerprint
import com.emutune.model.device.ThermalState
import com.emutune.model.route.EmulatorInstallation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Owns the device profile and detected emulator installations. */
@Singleton
class DeviceRepository @Inject constructor(
    private val profiler: DeviceProfiler,
    private val thermalMonitor: ThermalMonitor,
    private val detector: EmulatorDetector,
    private val deviceDao: DeviceDao,
) {

    fun observeProfile(): Flow<DeviceFingerprint?> =
        deviceDao.observeProfile().map { it?.toDomain() }

    fun observeInstallations(): Flow<List<EmulatorInstallation>> =
        deviceDao.observeInstallations().map { list -> list.map { it.toDomain() } }

    suspend fun refresh() {
        deviceDao.saveProfile(profiler.profile().toProfileEntity())
        deviceDao.saveInstallations(detector.detectInstalled().map { it.toEntity() })
    }

    fun currentThermalState(): ThermalState = thermalMonitor.currentThermalState()
}
