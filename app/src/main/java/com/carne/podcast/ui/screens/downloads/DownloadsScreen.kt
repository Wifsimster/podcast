package com.carne.podcast.ui.screens.downloads

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.ui.components.CarneEmptyState
import com.carne.podcast.ui.components.EpisodeRow
import com.carne.podcast.ui.components.EpisodeRowDividerStartInset

@Composable
fun DownloadsScreen(
    onOpenPlayer: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    if (downloads.isEmpty()) {
        CarneEmptyState(
            icon = Icons.Rounded.DownloadForOffline,
            title = "Nothing downloaded yet",
            message = "Tap the download icon on any episode to keep it offline — perfect for flights and dead zones.",
        )
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
                onClick = { viewModel.open(episode); onOpenPlayer() },
                onDownload = {},
                onDeleteDownload = { viewModel.deleteDownload(episode) },
                modifier = Modifier.animateItem(),
            )
            HorizontalDivider(Modifier.padding(start = EpisodeRowDividerStartInset))
        }
    }
}
