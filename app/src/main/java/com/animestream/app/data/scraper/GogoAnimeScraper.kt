package com.animestream.app.data.scraper

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class GogoAnimeScraper {
    private val baseUrl = "https://anitaku.pe"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    
    suspend fun searchAnime(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "$baseUrl/search.html?keyword=$encodedQuery"
            
            Log.d("GogoScraper", "Searching: $searchUrl")
            
            val doc = Jsoup.connect(searchUrl)
                .userAgent(userAgent)
                .referrer("https://www.google.com/")
                .timeout(15000)
                .followRedirects(true)
                .get()
            
            val results = mutableListOf<SearchResult>()
            val items = doc.select("ul.items li")
            
            Log.d("GogoScraper", "Found ${items.size} items")
            
            items.forEach { item ->
                val link = item.selectFirst("p.name a")
                val img = item.selectFirst("div.img img")
                
                if (link != null) {
                    val id = link.attr("href").removePrefix("/category/")
                    results.add(SearchResult(
                        id = id,
                        title = link.attr("title"),
                        image = img?.attr("src") ?: ""
                    ))
                    Log.d("GogoScraper", "Found: $id")
                }
            }
            
            results
        } catch (e: Exception) {
            Log.e("GogoScraper", "Search failed", e)
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
            Log.d("GogoScraper", "Getting streams from: $url")
            
            val doc = Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(baseUrl)
                .timeout(15000)
                .get()
            
            val links = mutableListOf<StreamLink>()
            
            doc.select("div.anime_muti_link ul li a").forEach { server ->
                val dataVideo = server.attr("data-video")
                if (dataVideo.isNotEmpty()) {
                    val fullUrl = if (dataVideo.startsWith("//")) "https:$dataVideo" else dataVideo
                    links.add(StreamLink(
                        url = fullUrl,
                        quality = server.text(),
                        server = server.text()
                    ))
                    Log.d("GogoScraper", "Server: ${server.text()} -> $fullUrl")
                }
            }
            
            links
        } catch (e: Exception) {
            Log.e("GogoScraper", "Get streaming links failed", e)
            emptyList()
        }
    }
    
    suspend fun extractM3U8(embedUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("GogoScraper", "Extracting from: $embedUrl")
            
            val doc = Jsoup.connect(embedUrl)
                .userAgent(userAgent)
                .referrer(baseUrl)
                .timeout(15000)
                .get()
            
            val scripts = doc.select("script")
            for (script in scripts) {
                val content = script.html()
                
                // Look for m3u8 URLs
                val m3u8Regex = """https?://[^\s"'`]+\.m3u8[^\s"'`]*""".toRegex()
                val match = m3u8Regex.find(content)
                if (match != null) {
                    val url = match.value.replace("\\", "")
                    Log.d("GogoScraper", "Found m3u8: $url")
                    return@withContext url
                }
                
                // Look for file: or sources: patterns
                if (content.contains("file:") || content.contains("sources:")) {
                    val fileRegex = """file:\s*["']([^"']+)["']""".toRegex()
                    val fileMatch = fileRegex.find(content)
                    if (fileMatch != null) {
                        val url = fileMatch.groupValues[1]
                        if (url.contains(".m3u8")) {
                            Log.d("GogoScraper", "Found file: $url")
                            return@withContext url
                        }
                    }
                }
            }
            
            Log.w("GogoScraper", "No m3u8 found in embed")
            null
        } catch (e: Exception) {
            Log.e("GogoScraper", "Extract m3u8 failed", e)
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
