package com.emutune.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.data.emulator.EmulatorRegistry
import com.emutune.data.repo.DeviceRepository
import com.emutune.model.device.DeviceFingerprint
import com.emutune.model.ids.EmulatorId
import com.emutune.model.route.EmulatorInstallation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    deviceRepository: DeviceRepository,
    private val registry: EmulatorRegistry,
) : ViewModel() {

    val profile: StateFlow<DeviceFingerprint?> = deviceRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val installations: StateFlow<List<EmulatorInstallation>> = deviceRepository.observeInstallations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun emulatorName(id: EmulatorId): String = registry.displayName(id)
}
