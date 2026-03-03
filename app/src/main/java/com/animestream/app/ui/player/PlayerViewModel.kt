package com.animestream.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animestream.app.data.model.StreamingLinks
import com.animestream.app.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {

    private val _streamingLinks = MutableStateFlow<StreamingLinks?>(null)
    val streamingLinks: StateFlow<StreamingLinks?> = _streamingLinks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadStreamingLinks(episodeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getStreamingLinks(episodeId).collect { result ->
                _streamingLinks.value = result.getOrNull()
                _isLoading.value = false
            }
        }
    }
}
