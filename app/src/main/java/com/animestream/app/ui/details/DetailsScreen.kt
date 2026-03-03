package com.animestream.app.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.animestream.app.data.model.Episode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    animeId: String,
    onBackClick: () -> Unit,
    onEpisodeClick: (String) -> Unit,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val animeInfo by viewModel.animeInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(animeId) {
        viewModel.loadAnimeInfo(animeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            animeInfo?.let { info ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AsyncImage(
                            model = info.cover ?: info.image,
                            contentDescription = info.title.getPreferredTitle(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    item {
                        Text(
                            text = info.title.getPreferredTitle(),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            info.status?.let {
                                AssistChip(onClick = {}, label = { Text(it) })
                            }
                            info.type?.let {
                                AssistChip(onClick = {}, label = { Text(it) })
                            }
                            info.totalEpisodes?.let {
                                AssistChip(onClick = {}, label = { Text("$it Episodes") })
                            }
                        }
                    }

                    item {
                        info.description?.let {
                            Text(
                                text = it.replace(Regex("<[^>]*>"), ""),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (info.genres.isNotEmpty()) {
                        item {
                            Text("Genres", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                info.genres.take(5).forEach { genre ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(genre) }
                                    )
                                }
                            }
                        }
                    }

                    if (info.episodes.isNotEmpty()) {
                        item {
                            Text("Episodes", style = MaterialTheme.typography.titleLarge)
                        }
                        items(info.episodes) { episode ->
                            EpisodeCard(episode, onEpisodeClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeCard(episode: Episode, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(episode.id) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            episode.image?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp, 60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Episode ${episode.number}",
                    style = MaterialTheme.typography.titleMedium
                )
                episode.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
