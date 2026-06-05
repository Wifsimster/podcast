package com.carne.podcast.ui.screens.podcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.ui.components.EpisodeRow
import com.carne.podcast.ui.components.PodcastArtwork
import com.carne.podcast.ui.components.stripHtml
import com.carne.podcast.ui.theme.CarneTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    viewModel: PodcastViewModel = hiltViewModel(),
) {
    val podcast by viewModel.podcast.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        podcast?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
        ) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        PodcastArtwork(
                            url = podcast?.imageUrl,
                            modifier = Modifier.size(110.dp),
                            shape = CarneTheme.shapes.artworkMedium,
                            contentDescription = podcast?.title?.let { "Artwork for $it" },
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                podcast?.title.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() },
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                podcast?.author.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            val subscribed = podcast?.subscribed == true
                            val subscribeSemantics = Modifier.semantics {
                                contentDescription = "Subscribe"
                                stateDescription = if (subscribed) "Subscribed" else "Not subscribed"
                            }
                            if (subscribed) {
                                OutlinedButton(
                                    onClick = viewModel::toggleSubscribe,
                                    modifier = subscribeSemantics,
                                ) {
                                    Text("Subscribed")
                                }
                            } else {
                                Button(
                                    onClick = viewModel::toggleSubscribe,
                                    modifier = subscribeSemantics,
                                ) {
                                    Text("Subscribe")
                                }
                            }
                        }
                    }
                    podcast?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stripHtml(desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${episodes.size} episodes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                HorizontalDivider()
            }

            items(episodes, key = { it.id }) { episode ->
                EpisodeRow(
                    episode = episode,
                    isCurrent = playerState.currentEpisodeId == episode.id,
                    isPlaying = playerState.isPlaying,
                    onPlayToggle = { viewModel.playToggle(episode) },
                    onClick = { viewModel.open(episode); onOpenPlayer() },
                    onDownload = { viewModel.download(episode) },
                    onDeleteDownload = { viewModel.deleteDownload(episode) },
                    showArtwork = false,
                    modifier = Modifier.animateItem(),
                )
                HorizontalDivider(Modifier.padding(start = CarneTheme.spacing.lg))
            }
        }
    }
}
