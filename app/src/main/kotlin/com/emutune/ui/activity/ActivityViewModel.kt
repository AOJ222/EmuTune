package com.emutune.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.data.repo.ActivityRepository
import com.emutune.model.activity.ActivityEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ActivityViewModel @Inject constructor(
    activityRepository: ActivityRepository,
) : ViewModel() {

    val events: StateFlow<List<ActivityEvent>> = activityRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
