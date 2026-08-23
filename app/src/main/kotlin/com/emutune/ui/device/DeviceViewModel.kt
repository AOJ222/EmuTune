package com.emutune.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.data.emulator.DolphinConfigReader
import com.emutune.data.emulator.SafGrantStore
import com.emutune.data.repo.DeviceRepository
import com.emutune.model.config.IniDocument
import com.emutune.model.device.DeviceFingerprint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DeviceViewModel @Inject constructor(
    deviceRepository: DeviceRepository,
    private val dolphinConfigReader: DolphinConfigReader,
    grantStore: SafGrantStore,
) : ViewModel() {

    val profile: StateFlow<DeviceFingerprint?> = deviceRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True once the user has granted Dolphin config access via SAF. */
    val hasDolphinGrant: StateFlow<Boolean> = grantStore.dolphinUri
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _dolphinConfig = MutableStateFlow<IniDocument?>(null)
    val dolphinConfig: StateFlow<IniDocument?> = _dolphinConfig

    init {
        viewModelScope.launch {
            grantStore.dolphinUri.collect { uri ->
                _dolphinConfig.value = if (uri == null) null else dolphinConfigReader.readDolphinIni()
            }
        }
    }
}
