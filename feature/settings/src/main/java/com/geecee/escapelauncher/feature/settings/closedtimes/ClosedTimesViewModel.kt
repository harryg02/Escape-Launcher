package com.geecee.escapelauncher.feature.settings.closedtimes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geecee.escapelauncher.core.domain.repository.settings.AppPauseSettingsRepository
import com.geecee.escapelauncher.core.model.ClosedPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ClosedTimesViewModel @Inject constructor(
    private val appPauseSettingsRepository: AppPauseSettingsRepository
) : ViewModel() {
    val closedPeriods: StateFlow<List<ClosedPeriod>> = appPauseSettingsRepository.closedPeriods
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Adds [period], or replaces the one at [index] when an existing period was edited
     */
    fun savePeriod(index: Int?, period: ClosedPeriod) {
        viewModelScope.launch {
            // Read the stored list rather than the UI state, which may not have loaded yet
            val current = appPauseSettingsRepository.closedPeriods.first()
            val updated = if (index != null && index in current.indices) {
                current.toMutableList().apply { set(index, period) }
            } else {
                current + period
            }
            appPauseSettingsRepository.setClosedPeriods(updated)
        }
    }

    fun removePeriod(index: Int) {
        viewModelScope.launch {
            val current = appPauseSettingsRepository.closedPeriods.first()
            if (index in current.indices) {
                appPauseSettingsRepository.setClosedPeriods(current.filterIndexed { i, _ -> i != index })
            }
        }
    }
}
