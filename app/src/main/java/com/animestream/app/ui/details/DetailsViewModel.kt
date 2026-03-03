package com.animestream.app.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animestream.app.data.model.AnimeInfo
import com.animestream.app.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {

    private val _animeInfo = MutableStateFlow<AnimeInfo?>(null)
    val animeInfo: StateFlow<AnimeInfo?> = _animeInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAnimeInfo(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val info = repository.getAnimeInfo(id)
                _animeInfo.value = info
            } catch (e: Exception) {
                _animeInfo.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}
