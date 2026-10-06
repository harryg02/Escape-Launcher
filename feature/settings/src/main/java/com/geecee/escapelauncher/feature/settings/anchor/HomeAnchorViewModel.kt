package com.geecee.escapelauncher.feature.settings.anchor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geecee.escapelauncher.core.domain.repository.settings.HomeAnchorRepository
import com.geecee.escapelauncher.core.model.HomeAnchor
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class HomeAnchorViewModel @Inject constructor(
    private val homeAnchorRepository: HomeAnchorRepository
) : ViewModel() {
    /**
     * The saved anchor, read once to fill in the text fields
     */
    suspend fun loadHomeAnchor(): HomeAnchor = homeAnchorRepository.homeAnchor.first()

    fun saveHomeAnchor(text: String, phoneNumber: String) {
        viewModelScope.launch {
            if (text.isBlank()) {
                homeAnchorRepository.clearHomeAnchor()
            } else {
                homeAnchorRepository.setHomeAnchor(HomeAnchor(text = text, phoneNumber = phoneNumber))
            }
        }
    }

    fun clearHomeAnchor() {
        viewModelScope.launch {
            homeAnchorRepository.clearHomeAnchor()
        }
    }
}
