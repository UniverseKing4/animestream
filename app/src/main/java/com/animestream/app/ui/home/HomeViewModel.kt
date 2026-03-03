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
                repository.getTrending().collect { result ->
                    result.getOrNull()?.let {
                        _trendingAnime.value = it.results
                    }
                }
            }
            
            launch {
                repository.getPopular().collect { result ->
                    result.getOrNull()?.let {
                        _popularAnime.value = it.results
                    }
                }
            }
            
            launch {
                repository.getRecentEpisodes().collect { result ->
                    result.getOrNull()?.let {
                        _recentEpisodes.value = it.results
                    }
                }
            }
            
            _isLoading.value = false
        }
    }
}
