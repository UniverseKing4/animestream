package com.animestream.app.data.repository

import com.animestream.app.data.model.*
import com.animestream.app.data.remote.AnimeApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepository @Inject constructor(
    private val apiClient: AnimeApiClient
) {
    fun getTrending(page: Int = 1): Flow<Result<AnimeSearchResult>> = flow {
        emit(Result.success(apiClient.getTrending(page)))
    }

    fun getPopular(page: Int = 1): Flow<Result<AnimeSearchResult>> = flow {
        emit(Result.success(apiClient.getPopular(page)))
    }

    fun getRecentEpisodes(page: Int = 1): Flow<Result<AnimeSearchResult>> = flow {
        emit(Result.success(apiClient.getRecentEpisodes(page)))
    }

    fun searchAnime(query: String, page: Int = 1): Flow<Result<AnimeSearchResult>> = flow {
        emit(Result.success(apiClient.searchAnime(query, page)))
    }

    fun getAnimeInfo(id: String): Flow<Result<AnimeInfo?>> = flow {
        emit(Result.success(apiClient.getAnimeInfo(id)))
    }

    fun getStreamingLinks(episodeId: String): Flow<Result<StreamingLinks>> = flow {
        emit(Result.success(apiClient.getStreamingLinks(episodeId)))
    }

    fun getAnimeByGenre(genre: String, page: Int = 1): Flow<Result<AnimeSearchResult>> = flow {
        emit(Result.success(apiClient.getAnimeByGenre(genre, page)))
    }
}
