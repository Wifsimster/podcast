package com.carne.podcast.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.ui.components.EpisodeRow

@Composable
fun HomeScreen(
    onOpenPlayer: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val inProgress by viewModel.inProgress.collectAsStateWithLifecycle()
    val latest by viewModel.latest.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    if (inProgress.isEmpty() && latest.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Loading your podcasts…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        if (inProgress.isNotEmpty()) {
            item { SectionHeader("Continue listening") }
            items(inProgress, key = { "ip_${it.id}" }) { episode ->
                EpisodeRow(
                    episode = episode,
                    isCurrent = playerState.currentEpisodeId == episode.id,
                    isPlaying = playerState.isPlaying,
                    onPlayToggle = { viewModel.playToggle(episode); },
                    onClick = onOpenPlayer,
                    onDownload = { viewModel.download(episode) },
                    onDeleteDownload = { viewModel.deleteDownload(episode) },
                )
                HorizontalDivider(Modifier.padding(start = 84.dp))
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        item { SectionHeader("Latest episodes") }
        items(latest, key = { "lt_${it.id}" }) { episode ->
            EpisodeRow(
                episode = episode,
                isCurrent = playerState.currentEpisodeId == episode.id,
                isPlaying = playerState.isPlaying,
                onPlayToggle = { viewModel.playToggle(episode) },
                onClick = onOpenPlayer,
                onDownload = { viewModel.download(episode) },
                onDeleteDownload = { viewModel.deleteDownload(episode) },
            )
            HorizontalDivider(Modifier.padding(start = 84.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}
