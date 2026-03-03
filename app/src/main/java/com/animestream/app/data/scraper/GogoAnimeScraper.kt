package com.animestream.app.data.scraper

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder

class GogoAnimeScraper {
    private val baseUrl = "https://anitaku.pe"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    
    suspend fun searchAnime(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "$baseUrl/search.html?keyword=$encodedQuery"
            
            val doc: Document = Jsoup.connect(searchUrl)
                .userAgent(userAgent)
                .timeout(10000)
                .get()
            
            val results = mutableListOf<SearchResult>()
            val items = doc.select("ul.items li")
            
            items.forEach { item ->
                val link = item.selectFirst("p.name a")
                val img = item.selectFirst("div.img img")
                
                if (link != null) {
                    results.add(SearchResult(
                        id = link.attr("href").removePrefix("/category/"),
                        title = link.attr("title"),
                        image = img?.attr("src") ?: ""
                    ))
                }
            }
            
            Log.d("GogoScraper", "Found ${results.size} results for: $query")
            results
        } catch (e: Exception) {
            Log.e("GogoScraper", "Search failed: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun getEpisodes(animeId: String): List<EpisodeInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/category/$animeId"
            val doc: Document = Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(10000)
                .get()
            
            val episodes = mutableListOf<EpisodeInfo>()
            val epStart = doc.selectFirst("#episode_page li a")?.attr("ep_start")?.toIntOrNull() ?: 0
            val epEnd = doc.selectFirst("#episode_page li a")?.attr("ep_end")?.toIntOrNull() ?: 0
            val movieId = doc.selectFirst("#movie_id")?.attr("value") ?: ""
            
            if (movieId.isNotEmpty()) {
                val ajaxUrl = "https://ajax.gogocdn.net/ajax/load-list-episode?ep_start=$epStart&ep_end=$epEnd&id=$movieId"
                val epDoc: Document = Jsoup.connect(ajaxUrl)
                    .userAgent(userAgent)
                    .timeout(10000)
                    .get()
                
                epDoc.select("li a").forEach { ep ->
                    val epNum = ep.selectFirst("div.name")?.text()?.replace("EP ", "")?.toIntOrNull() ?: 0
                    episodes.add(EpisodeInfo(
                        id = ep.attr("href").trim().removePrefix("/"),
                        number = epNum,
                        title = "Episode $epNum"
                    ))
                }
            }
            
            Log.d("GogoScraper", "Found ${episodes.size} episodes for: $animeId")
            episodes.reversed()
        } catch (e: Exception) {
            Log.e("GogoScraper", "Get episodes failed: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun getStreamingLinks(episodeId: String): List<StreamLink> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/$episodeId"
            val doc: Document = Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(10000)
                .get()
            
            val links = mutableListOf<StreamLink>()
            
            // Get streaming server links
            doc.select("div.anime_muti_link ul li a").forEach { server ->
                val dataVideo = server.attr("data-video")
                if (dataVideo.isNotEmpty()) {
                    links.add(StreamLink(
                        url = if (dataVideo.startsWith("//")) "https:$dataVideo" else dataVideo,
                        quality = server.text(),
                        server = server.text()
                    ))
                }
            }
            
            Log.d("GogoScraper", "Found ${links.size} stream links for: $episodeId")
            links
        } catch (e: Exception) {
            Log.e("GogoScraper", "Get streaming links failed: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun extractM3U8(embedUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc: Document = Jsoup.connect(embedUrl)
                .userAgent(userAgent)
                .timeout(10000)
                .get()
            
            // Extract m3u8 from various embed sources
            val scripts = doc.select("script")
            for (script in scripts) {
                val scriptContent = script.html()
                if (scriptContent.contains(".m3u8")) {
                    val m3u8Regex = """https?://[^\s"']+\.m3u8[^\s"']*""".toRegex()
                    val match = m3u8Regex.find(scriptContent)
                    if (match != null) {
                        Log.d("GogoScraper", "Extracted m3u8: ${match.value}")
                        return@withContext match.value
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e("GogoScraper", "Extract m3u8 failed: ${e.message}", e)
            null
        }
    }
}

data class SearchResult(
    val id: String,
    val title: String,
    val image: String
)

data class EpisodeInfo(
    val id: String,
    val number: Int,
    val title: String
)

data class StreamLink(
    val url: String,
    val quality: String,
    val server: String
)
