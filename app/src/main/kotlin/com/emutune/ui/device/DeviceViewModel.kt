package com.emutune.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.data.repo.DeviceRepository
import com.emutune.model.device.DeviceFingerprint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DeviceViewModel @Inject constructor(
    deviceRepository: DeviceRepository,
) : ViewModel() {

    val profile: StateFlow<DeviceFingerprint?> = deviceRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
