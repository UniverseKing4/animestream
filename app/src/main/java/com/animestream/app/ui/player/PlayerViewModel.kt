package com.animestream.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animestream.app.data.model.StreamingLinks
import com.animestream.app.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {

    private val _streamingLinks = MutableStateFlow<StreamingLinks?>(null)
    val streamingLinks: StateFlow<StreamingLinks?> = _streamingLinks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadStreamingLinks(episodeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val links = repository.getStreamingLinks(episodeId)
                if (links.sources.isEmpty()) {
                    _error.value = "No streaming sources found"
                } else {
                    _streamingLinks.value = links
                }
            } catch (e: Exception) {
                _error.value = "Failed to load video: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
