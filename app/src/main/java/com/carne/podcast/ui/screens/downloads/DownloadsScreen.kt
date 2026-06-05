package com.carne.podcast.ui.screens.downloads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.ui.components.EpisodeRow

@Composable
fun DownloadsScreen(
    onOpenPlayer: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    if (downloads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No downloads yet.\nTap the download icon on any episode to save it offline.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(downloads, key = { it.id }) { episode ->
            EpisodeRow(
                episode = episode,
                isCurrent = playerState.currentEpisodeId == episode.id,
                isPlaying = playerState.isPlaying,
                onPlayToggle = { viewModel.playToggle(episode) },
                onClick = onOpenPlayer,
                onDownload = {},
                onDeleteDownload = { viewModel.deleteDownload(episode) },
            )
            HorizontalDivider(Modifier.padding(start = 84.dp))
        }
    }
}
