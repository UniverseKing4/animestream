package com.animestream.app.data.repository

import com.animestream.app.data.model.*
import com.animestream.app.data.remote.AnimeApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepository @Inject constructor(
    private val apiClient: AnimeApiClient
) {
    suspend fun getTrending(page: Int = 1): AnimeSearchResult {
        return apiClient.getTrending(page)
    }

    suspend fun getPopular(page: Int = 1): AnimeSearchResult {
        return apiClient.getPopular(page)
    }

    suspend fun getRecentEpisodes(page: Int = 1): AnimeSearchResult {
        return apiClient.getRecentEpisodes(page)
    }

    suspend fun searchAnime(query: String, page: Int = 1): AnimeSearchResult {
        return apiClient.searchAnime(query, page)
    }

    suspend fun getAnimeInfo(id: String): AnimeInfo? {
        return apiClient.getAnimeInfo(id)
    }

    suspend fun getStreamingLinks(episodeId: String): StreamingLinks {
        return apiClient.getStreamingLinks(episodeId)
    }

    suspend fun getAnimeByGenre(genre: String, page: Int = 1): AnimeSearchResult {
        return apiClient.getAnimeByGenre(genre, page)
    }
}
