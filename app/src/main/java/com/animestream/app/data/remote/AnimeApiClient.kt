package com.animestream.app.data.remote

import com.animestream.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeApiClient @Inject constructor(
    private val client: HttpClient
) {
    private val baseUrl = "https://api.consumet.org/meta/anilist"

    suspend fun getTrending(page: Int = 1): AnimeSearchResult {
        return try {
            client.get("$baseUrl/trending") {
                parameter("page", page)
            }.body()
        } catch (e: Exception) {
            AnimeSearchResult()
        }
    }

    suspend fun getPopular(page: Int = 1): AnimeSearchResult {
        return try {
            client.get("$baseUrl/popular") {
                parameter("page", page)
            }.body()
        } catch (e: Exception) {
            AnimeSearchResult()
        }
    }

    suspend fun getRecentEpisodes(page: Int = 1): AnimeSearchResult {
        return try {
            client.get("$baseUrl/recent-episodes") {
                parameter("page", page)
            }.body()
        } catch (e: Exception) {
            AnimeSearchResult()
        }
    }

    suspend fun searchAnime(query: String, page: Int = 1): AnimeSearchResult {
        return try {
            client.get("$baseUrl/$query") {
                parameter("page", page)
            }.body()
        } catch (e: Exception) {
            AnimeSearchResult()
        }
    }

    suspend fun getAnimeInfo(id: String): AnimeInfo? {
        return try {
            client.get("$baseUrl/info/$id").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStreamingLinks(episodeId: String): StreamingLinks {
        return try {
            client.get("$baseUrl/watch/$episodeId").body()
        } catch (e: Exception) {
            StreamingLinks()
        }
    }

    suspend fun getAnimeByGenre(genre: String, page: Int = 1): AnimeSearchResult {
        return try {
            client.get("$baseUrl/advanced-search") {
                parameter("genres", "[\"$genre\"]")
                parameter("page", page)
            }.body()
        } catch (e: Exception) {
            AnimeSearchResult()
        }
    }
}
