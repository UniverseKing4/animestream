package com.animestream.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Anime(
    val id: String,
    val title: Title,
    val image: String,
    val cover: String? = null,
    val description: String? = null,
    val status: String? = null,
    val releaseDate: Int? = null,
    val genres: List<String> = emptyList(),
    val totalEpisodes: Int? = null,
    val rating: Int? = null,
    val type: String? = null
)

@Serializable
data class Title(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
) {
    fun getPreferredTitle(): String = english ?: romaji ?: native ?: "Unknown"
}

@Serializable
data class AnimeSearchResult(
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val results: List<Anime> = emptyList()
)

@Serializable
data class AnimeInfo(
    val id: String,
    val title: Title,
    val image: String,
    val cover: String? = null,
    val description: String? = null,
    val status: String? = null,
    val releaseDate: Int? = null,
    val genres: List<String> = emptyList(),
    val totalEpisodes: Int? = null,
    val rating: Int? = null,
    val type: String? = null,
    val episodes: List<Episode> = emptyList()
)

@Serializable
data class Episode(
    val id: String,
    val number: Int,
    val title: String? = null,
    val image: String? = null
)

@Serializable
data class StreamingLinks(
    val sources: List<VideoSource> = emptyList(),
    val subtitles: List<Subtitle> = emptyList()
)

@Serializable
data class VideoSource(
    val url: String,
    val quality: String = "default",
    val isM3U8: Boolean = false
)

@Serializable
data class Subtitle(
    val url: String,
    val lang: String
)
