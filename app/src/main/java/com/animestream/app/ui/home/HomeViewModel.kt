package com.animestream.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animestream.app.data.model.Anime
import com.animestream.app.data.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {

    private val _trendingAnime = MutableStateFlow<List<Anime>>(emptyList())
    val trendingAnime: StateFlow<List<Anime>> = _trendingAnime.asStateFlow()

    private val _popularAnime = MutableStateFlow<List<Anime>>(emptyList())
    val popularAnime: StateFlow<List<Anime>> = _popularAnime.asStateFlow()

    private val _recentEpisodes = MutableStateFlow<List<Anime>>(emptyList())
    val recentEpisodes: StateFlow<List<Anime>> = _recentEpisodes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            launch {
                try {
                    val result = repository.getTrending()
                    _trendingAnime.value = result.results
                } catch (e: Exception) {
                    // Handle error
                }
            }
            
            launch {
                try {
                    val result = repository.getPopular()
                    _popularAnime.value = result.results
                } catch (e: Exception) {
                    // Handle error
                }
            }
            
            launch {
                try {
                    val result = repository.getRecentEpisodes()
                    _recentEpisodes.value = result.results
                } catch (e: Exception) {
                    // Handle error
                }
            }
            
            _isLoading.value = false
        }
    }
}
