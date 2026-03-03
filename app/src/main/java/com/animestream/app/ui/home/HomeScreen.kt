package com.animestream.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.animestream.app.data.model.Anime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val trendingAnime by viewModel.trendingAnime.collectAsState()
    val popularAnime by viewModel.popularAnime.collectAsState()
    val recentEpisodes by viewModel.recentEpisodes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AnimeStream") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(androidx.compose.material.icons.Icons.Default.Search, "Search")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading && trendingAnime.isEmpty() -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                trendingAnime.isEmpty() && popularAnime.isEmpty() && recentEpisodes.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Unable to load anime")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadHomeData() }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (trendingAnime.isNotEmpty()) {
                    item {
                        Text(
                            "Trending Now",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(trendingAnime) { anime ->
                                AnimeCard(anime, onAnimeClick)
                            }
                        }
                    }
                }

                if (popularAnime.isNotEmpty()) {
                    item {
                        Text(
                            "Popular Anime",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(popularAnime) { anime ->
                                AnimeCard(anime, onAnimeClick)
                            }
                        }
                    }
                }

                if (recentEpisodes.isNotEmpty()) {
                    item {
                        Text(
                            "Recent Episodes",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentEpisodes) { anime ->
                                AnimeCard(anime, onAnimeClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeCard(anime: Anime, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick(anime.id) }
    ) {
        AsyncImage(
            model = anime.image,
            contentDescription = anime.title.getPreferredTitle(),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = anime.title.getPreferredTitle(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
