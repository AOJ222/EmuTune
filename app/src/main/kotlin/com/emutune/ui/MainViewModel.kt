package com.emutune.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.data.repo.DeviceRepository
import com.emutune.data.seed.DemoDataSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Optional
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Bootstraps the app: profiles the device, detects emulators, and seeds demo data when
 * a debug seeder is present. The seeder is optional so the release graph never resolves
 * debug-only evidence.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val demoSeeder: Optional<DemoDataSeeder>,
) : ViewModel() {

    init {
        viewModelScope.launch {
            deviceRepository.refresh()
            if (demoSeeder.isPresent) {
                demoSeeder.get().seedIfEmpty()
            }
        }
    }
}
