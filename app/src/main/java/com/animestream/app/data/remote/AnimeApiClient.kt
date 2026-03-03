package com.animestream.app.data.remote

import android.util.Log
import com.animestream.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeApiClient @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) {
    private val anilistUrl = "https://graphql.anilist.co"
    private val jikanUrl = "https://api.jikan.moe/v4"

    suspend fun getTrending(page: Int = 1): AnimeSearchResult {
        return try {
            val query = """
                query {
                    Page(page: $page, perPage: 20) {
                        pageInfo { hasNextPage currentPage }
                        media(type: ANIME, sort: TRENDING_DESC) {
                            id title { romaji english native }
                            coverImage { large }
                            bannerImage
                            description
                            status
                            startDate { year }
                            genres
                            episodes
                            averageScore
                            format
                        }
                    }
                }
            """.trimIndent()
            
            val response: HttpResponse = client.post(anilistUrl) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            
            parseAnilistResponse(response.bodyAsText())
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Error fetching trending", e)
            getJikanFallback()
        }
    }

    suspend fun getPopular(page: Int = 1): AnimeSearchResult {
        return try {
            val query = """
                query {
                    Page(page: $page, perPage: 20) {
                        pageInfo { hasNextPage currentPage }
                        media(type: ANIME, sort: POPULARITY_DESC) {
                            id title { romaji english native }
                            coverImage { large }
                            bannerImage
                            description
                            status
                            startDate { year }
                            genres
                            episodes
                            averageScore
                            format
                        }
                    }
                }
            """.trimIndent()
            
            val response: HttpResponse = client.post(anilistUrl) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            
            parseAnilistResponse(response.bodyAsText())
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Error fetching popular", e)
            getJikanFallback()
        }
    }

    suspend fun getRecentEpisodes(page: Int = 1): AnimeSearchResult {
        return try {
            val query = """
                query {
                    Page(page: $page, perPage: 20) {
                        pageInfo { hasNextPage currentPage }
                        airingSchedules(notYetAired: false, sort: TIME_DESC) {
                            media {
                                id title { romaji english native }
                                coverImage { large }
                                bannerImage
                                description
                                status
                                startDate { year }
                                genres
                                episodes
                                averageScore
                                format
                            }
                        }
                    }
                }
            """.trimIndent()
            
            val response: HttpResponse = client.post(anilistUrl) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            
            parseAnilistAiringResponse(response.bodyAsText())
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Error fetching recent", e)
            getTrending(page)
        }
    }

    suspend fun searchAnime(query: String, page: Int = 1): AnimeSearchResult {
        return try {
            val graphqlQuery = """
                query {
                    Page(page: $page, perPage: 20) {
                        pageInfo { hasNextPage currentPage }
                        media(search: "$query", type: ANIME) {
                            id title { romaji english native }
                            coverImage { large }
                            bannerImage
                            description
                            status
                            startDate { year }
                            genres
                            episodes
                            averageScore
                            format
                        }
                    }
                }
            """.trimIndent()
            
            val response: HttpResponse = client.post(anilistUrl) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to graphqlQuery))
            }
            
            parseAnilistResponse(response.bodyAsText())
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Error searching anime", e)
            searchJikan(query, page)
        }
    }

    suspend fun getAnimeInfo(id: String): AnimeInfo? {
        return try {
            val query = """
                query {
                    Media(id: $id, type: ANIME) {
                        id title { romaji english native }
                        coverImage { large }
                        bannerImage
                        description
                        status
                        startDate { year }
                        genres
                        episodes
                        averageScore
                        format
                        streamingEpisodes {
                            title
                            thumbnail
                        }
                    }
                }
            """.trimIndent()
            
            val response: HttpResponse = client.post(anilistUrl) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            
            parseAnilistInfoResponse(response.bodyAsText())
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Error fetching anime info", e)
            null
        }
    }

    suspend fun getStreamingLinks(episodeId: String): StreamingLinks {
        return try {
            val sources = mutableListOf<VideoSource>()
            
            // Parse episode info from ID (format: "anime-title-episode-X")
            val episodeNumber = episodeId.substringAfterLast("-").toIntOrNull() ?: 1
            val animeSlug = episodeId.substringBeforeLast("-episode")
            
            Log.d("AnimeAPI", "Searching streams for: $animeSlug episode $episodeNumber")
            
            // Try GogoAnime
            try {
                // Search for anime first
                val searchResponse: HttpResponse = client.get("https://api.consumet.org/anime/gogoanime/$animeSlug") {
                    timeout { requestTimeoutMillis = 8000 }
                }
                
                if (searchResponse.status.value == 200) {
                    val animeData = json.parseToJsonElement(searchResponse.bodyAsText()).jsonObject
                    val episodes = animeData["episodes"]?.jsonArray
                    
                    // Find matching episode
                    val targetEpisode = episodes?.find { ep ->
                        ep.jsonObject["number"]?.jsonPrimitive?.intOrNull == episodeNumber
                    }?.jsonObject
                    
                    val gogoEpisodeId = targetEpisode?.get("id")?.jsonPrimitive?.contentOrNull
                    
                    if (gogoEpisodeId != null) {
                        // Get streaming links
                        val streamResponse: HttpResponse = client.get("https://api.consumet.org/anime/gogoanime/watch/$gogoEpisodeId") {
                            timeout { requestTimeoutMillis = 8000 }
                        }
                        
                        if (streamResponse.status.value == 200) {
                            val streamData = json.parseToJsonElement(streamResponse.bodyAsText()).jsonObject
                            streamData["sources"]?.jsonArray?.forEach { source ->
                                val obj = source.jsonObject
                                val url = obj["url"]?.jsonPrimitive?.contentOrNull
                                val quality = obj["quality"]?.jsonPrimitive?.contentOrNull ?: "default"
                                if (url != null) {
                                    sources.add(VideoSource(url = url, quality = quality, isM3U8 = url.contains(".m3u8")))
                                    Log.d("AnimeAPI", "Added GogoAnime source: $quality")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AnimeAPI", "GogoAnime failed: ${e.message}", e)
            }
            
            // Try Zoro/HiAnime
            try {
                val zoroSearchResponse: HttpResponse = client.get("https://api.consumet.org/anime/zoro/$animeSlug") {
                    timeout { requestTimeoutMillis = 8000 }
                }
                
                if (zoroSearchResponse.status.value == 200) {
                    val animeData = json.parseToJsonElement(zoroSearchResponse.bodyAsText()).jsonObject
                    val episodes = animeData["episodes"]?.jsonArray
                    
                    val targetEpisode = episodes?.find { ep ->
                        ep.jsonObject["number"]?.jsonPrimitive?.intOrNull == episodeNumber
                    }?.jsonObject
                    
                    val zoroEpisodeId = targetEpisode?.get("id")?.jsonPrimitive?.contentOrNull
                    
                    if (zoroEpisodeId != null) {
                        val streamResponse: HttpResponse = client.get("https://api.consumet.org/anime/zoro/watch?episodeId=$zoroEpisodeId") {
                            timeout { requestTimeoutMillis = 8000 }
                        }
                        
                        if (streamResponse.status.value == 200) {
                            val streamData = json.parseToJsonElement(streamResponse.bodyAsText()).jsonObject
                            streamData["sources"]?.jsonArray?.forEach { source ->
                                val obj = source.jsonObject
                                val url = obj["url"]?.jsonPrimitive?.contentOrNull
                                val quality = obj["quality"]?.jsonPrimitive?.contentOrNull ?: "default"
                                if (url != null && !sources.any { it.url == url }) {
                                    sources.add(VideoSource(url = url, quality = "Zoro $quality", isM3U8 = url.contains(".m3u8")))
                                    Log.d("AnimeAPI", "Added Zoro source: $quality")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AnimeAPI", "Zoro failed: ${e.message}", e)
            }
            
            // Fallback: Working demo streams
            if (sources.isEmpty()) {
                Log.w("AnimeAPI", "No real sources found, using demo streams")
                sources.add(VideoSource(
                    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    quality = "1080p (Demo)",
                    isM3U8 = true
                ))
                sources.add(VideoSource(
                    url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                    quality = "720p (Demo)",
                    isM3U8 = true
                ))
                sources.add(VideoSource(
                    url = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                    quality = "HD (Demo)",
                    isM3U8 = true
                ))
            }
            
            Log.d("AnimeAPI", "Total sources found: ${sources.size}")
            StreamingLinks(sources = sources)
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Error getting streaming links", e)
            // Return demo streams on error
            StreamingLinks(sources = listOf(
                VideoSource("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Demo", true)
            ))
        }
    }

    suspend fun getAnimeByGenre(genre: String, page: Int = 1): AnimeSearchResult {
        return try {
            val query = """
                query {
                    Page(page: $page, perPage: 20) {
                        pageInfo { hasNextPage currentPage }
                        media(genre: "$genre", type: ANIME, sort: POPULARITY_DESC) {
                            id title { romaji english native }
                            coverImage { large }
                            bannerImage
                            description
                            status
                            startDate { year }
                            genres
                            episodes
                            averageScore
                            format
                        }
                    }
                }
            """.trimIndent()
            
            val response: HttpResponse = client.post(anilistUrl) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            
            parseAnilistResponse(response.bodyAsText())
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Error fetching by genre", e)
            AnimeSearchResult()
        }
    }

    private fun parseAnilistResponse(responseBody: String): AnimeSearchResult {
        val jsonElement = json.parseToJsonElement(responseBody).jsonObject
        val data = jsonElement["data"]?.jsonObject ?: return AnimeSearchResult()
        val page = data["Page"]?.jsonObject ?: return AnimeSearchResult()
        val pageInfo = page["pageInfo"]?.jsonObject
        val media = page["media"]?.jsonArray ?: return AnimeSearchResult()

        val animeList = media.mapNotNull { mediaElement ->
            try {
                val mediaObj = mediaElement.jsonObject
                val id = mediaObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val titleObj = mediaObj["title"]?.jsonObject
                val title = Title(
                    romaji = titleObj?.get("romaji")?.jsonPrimitive?.contentOrNull,
                    english = titleObj?.get("english")?.jsonPrimitive?.contentOrNull,
                    native = titleObj?.get("native")?.jsonPrimitive?.contentOrNull
                )
                val coverImage = mediaObj["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.content ?: ""
                val bannerImage = mediaObj["bannerImage"]?.jsonPrimitive?.contentOrNull
                val description = mediaObj["description"]?.jsonPrimitive?.contentOrNull
                val status = mediaObj["status"]?.jsonPrimitive?.contentOrNull
                val year = mediaObj["startDate"]?.jsonObject?.get("year")?.jsonPrimitive?.intOrNull
                val genres = mediaObj["genres"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                val episodes = mediaObj["episodes"]?.jsonPrimitive?.intOrNull
                val score = mediaObj["averageScore"]?.jsonPrimitive?.intOrNull
                val format = mediaObj["format"]?.jsonPrimitive?.contentOrNull

                Anime(
                    id = id,
                    title = title,
                    image = coverImage,
                    cover = bannerImage,
                    description = description,
                    status = status,
                    releaseDate = year,
                    genres = genres,
                    totalEpisodes = episodes,
                    rating = score,
                    type = format
                )
            } catch (e: Exception) {
                Log.e("AnimeAPI", "Error parsing anime", e)
                null
            }
        }

        return AnimeSearchResult(
            currentPage = pageInfo?.get("currentPage")?.jsonPrimitive?.intOrNull ?: 1,
            hasNextPage = pageInfo?.get("hasNextPage")?.jsonPrimitive?.booleanOrNull ?: false,
            results = animeList
        )
    }

    private fun parseAnilistAiringResponse(responseBody: String): AnimeSearchResult {
        val jsonElement = json.parseToJsonElement(responseBody).jsonObject
        val data = jsonElement["data"]?.jsonObject ?: return AnimeSearchResult()
        val page = data["Page"]?.jsonObject ?: return AnimeSearchResult()
        val pageInfo = page["pageInfo"]?.jsonObject
        val schedules = page["airingSchedules"]?.jsonArray ?: return AnimeSearchResult()

        val animeList = schedules.mapNotNull { scheduleElement ->
            try {
                val mediaObj = scheduleElement.jsonObject["media"]?.jsonObject ?: return@mapNotNull null
                val id = mediaObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val titleObj = mediaObj["title"]?.jsonObject
                val title = Title(
                    romaji = titleObj?.get("romaji")?.jsonPrimitive?.contentOrNull,
                    english = titleObj?.get("english")?.jsonPrimitive?.contentOrNull,
                    native = titleObj?.get("native")?.jsonPrimitive?.contentOrNull
                )
                val coverImage = mediaObj["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.content ?: ""
                val bannerImage = mediaObj["bannerImage"]?.jsonPrimitive?.contentOrNull
                val description = mediaObj["description"]?.jsonPrimitive?.contentOrNull
                val status = mediaObj["status"]?.jsonPrimitive?.contentOrNull
                val year = mediaObj["startDate"]?.jsonObject?.get("year")?.jsonPrimitive?.intOrNull
                val genres = mediaObj["genres"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                val episodes = mediaObj["episodes"]?.jsonPrimitive?.intOrNull
                val score = mediaObj["averageScore"]?.jsonPrimitive?.intOrNull
                val format = mediaObj["format"]?.jsonPrimitive?.contentOrNull

                Anime(
                    id = id,
                    title = title,
                    image = coverImage,
                    cover = bannerImage,
                    description = description,
                    status = status,
                    releaseDate = year,
                    genres = genres,
                    totalEpisodes = episodes,
                    rating = score,
                    type = format
                )
            } catch (e: Exception) {
                Log.e("AnimeAPI", "Error parsing airing anime", e)
                null
            }
        }

        return AnimeSearchResult(
            currentPage = pageInfo?.get("currentPage")?.jsonPrimitive?.intOrNull ?: 1,
            hasNextPage = pageInfo?.get("hasNextPage")?.jsonPrimitive?.booleanOrNull ?: false,
            results = animeList
        )
    }

    private fun parseAnilistInfoResponse(responseBody: String): AnimeInfo? {
        return try {
            val jsonElement = json.parseToJsonElement(responseBody).jsonObject
            val data = jsonElement["data"]?.jsonObject ?: return null
            val mediaObj = data["Media"]?.jsonObject ?: return null
            
            val id = mediaObj["id"]?.jsonPrimitive?.content ?: return null
            val titleObj = mediaObj["title"]?.jsonObject
            val title = Title(
                romaji = titleObj?.get("romaji")?.jsonPrimitive?.contentOrNull,
                english = titleObj?.get("english")?.jsonPrimitive?.contentOrNull,
                native = titleObj?.get("native")?.jsonPrimitive?.contentOrNull
            )
            val coverImage = mediaObj["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.content ?: ""
            val bannerImage = mediaObj["bannerImage"]?.jsonPrimitive?.contentOrNull
            val description = mediaObj["description"]?.jsonPrimitive?.contentOrNull
            val status = mediaObj["status"]?.jsonPrimitive?.contentOrNull
            val year = mediaObj["startDate"]?.jsonObject?.get("year")?.jsonPrimitive?.intOrNull
            val genres = mediaObj["genres"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val totalEpisodes = mediaObj["episodes"]?.jsonPrimitive?.intOrNull
            val score = mediaObj["averageScore"]?.jsonPrimitive?.intOrNull
            val format = mediaObj["format"]?.jsonPrimitive?.contentOrNull
            
            val streamingEpisodes = mediaObj["streamingEpisodes"]?.jsonArray?.mapIndexed { index, episodeElement ->
                val episodeObj = episodeElement.jsonObject
                Episode(
                    id = "$id-${index + 1}",
                    number = index + 1,
                    title = episodeObj["title"]?.jsonPrimitive?.contentOrNull,
                    image = episodeObj["thumbnail"]?.jsonPrimitive?.contentOrNull
                )
            } ?: (1..(totalEpisodes ?: 12)).map { Episode(id = "$id-$it", number = it) }

            AnimeInfo(
                id = id,
                title = title,
                image = coverImage,
                cover = bannerImage,
                description = description,
                status = status,
                releaseDate = year,
                genres = genres,
                totalEpisodes = totalEpisodes,
                rating = score,
                type = format,
                episodes = streamingEpisodes
            )
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Error parsing anime info", e)
            null
        }
    }

    private suspend fun getJikanFallback(): AnimeSearchResult {
        return try {
            val response: HttpResponse = client.get("$jikanUrl/top/anime") {
                parameter("limit", 20)
            }
            parseJikanResponse(response.bodyAsText())
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Jikan fallback failed", e)
            AnimeSearchResult()
        }
    }

    private suspend fun searchJikan(query: String, page: Int): AnimeSearchResult {
        return try {
            val response: HttpResponse = client.get("$jikanUrl/anime") {
                parameter("q", query)
                parameter("page", page)
                parameter("limit", 20)
            }
            parseJikanResponse(response.bodyAsText())
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Jikan search failed", e)
            AnimeSearchResult()
        }
    }

    private fun parseJikanResponse(responseBody: String): AnimeSearchResult {
        return try {
            val jsonElement = json.parseToJsonElement(responseBody).jsonObject
            val data = jsonElement["data"]?.jsonArray ?: return AnimeSearchResult()
            val pagination = jsonElement["pagination"]?.jsonObject

            val animeList = data.mapNotNull { animeElement ->
                try {
                    val animeObj = animeElement.jsonObject
                    val id = animeObj["mal_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val titleObj = animeObj["title"]?.jsonPrimitive?.content
                    val titleEnglish = animeObj["title_english"]?.jsonPrimitive?.contentOrNull
                    val titleJapanese = animeObj["title_japanese"]?.jsonPrimitive?.contentOrNull
                    
                    val title = Title(
                        romaji = titleObj,
                        english = titleEnglish,
                        native = titleJapanese
                    )
                    
                    val images = animeObj["images"]?.jsonObject?.get("jpg")?.jsonObject
                    val coverImage = images?.get("large_image_url")?.jsonPrimitive?.content 
                        ?: images?.get("image_url")?.jsonPrimitive?.content ?: ""
                    
                    val description = animeObj["synopsis"]?.jsonPrimitive?.contentOrNull
                    val status = animeObj["status"]?.jsonPrimitive?.contentOrNull
                    val year = animeObj["year"]?.jsonPrimitive?.intOrNull
                    val genres = animeObj["genres"]?.jsonArray?.mapNotNull { 
                        it.jsonObject["name"]?.jsonPrimitive?.contentOrNull 
                    } ?: emptyList()
                    val episodes = animeObj["episodes"]?.jsonPrimitive?.intOrNull
                    val score = animeObj["score"]?.jsonPrimitive?.doubleOrNull?.times(10)?.toInt()
                    val type = animeObj["type"]?.jsonPrimitive?.contentOrNull

                    Anime(
                        id = id,
                        title = title,
                        image = coverImage,
                        description = description,
                        status = status,
                        releaseDate = year,
                        genres = genres,
                        totalEpisodes = episodes,
                        rating = score,
                        type = type
                    )
                } catch (e: Exception) {
                    Log.e("AnimeAPI", "Error parsing Jikan anime", e)
                    null
                }
            }

            AnimeSearchResult(
                currentPage = pagination?.get("current_page")?.jsonPrimitive?.intOrNull ?: 1,
                hasNextPage = pagination?.get("has_next_page")?.jsonPrimitive?.booleanOrNull ?: false,
                results = animeList
            )
        } catch (e: Exception) {
            Log.e("AnimeAPI", "Error parsing Jikan response", e)
            AnimeSearchResult()
        }
    }
}
