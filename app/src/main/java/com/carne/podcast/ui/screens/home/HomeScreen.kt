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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.R
import com.carne.podcast.ui.components.CarneEmptyState
import com.carne.podcast.ui.components.EpisodeRow
import com.carne.podcast.ui.components.EpisodeRowDividerStartInset
import com.carne.podcast.ui.theme.CarneTheme

@Composable
fun HomeScreen(
    onOpenPlayer: () -> Unit,
    onBrowse: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    when {
        uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                ContainedLoadingIndicator()
                Spacer(Modifier.height(CarneTheme.spacing.lg))
                Text(
                    stringResource(R.string.loading_podcasts),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        uiState.inProgress.isEmpty() && uiState.latest.isEmpty() -> CarneEmptyState(
            icon = Icons.Rounded.Whatshot,
            title = stringResource(R.string.home_welcome_title),
            message = stringResource(R.string.home_welcome_message),
            actionLabel = stringResource(R.string.find_podcasts),
            onAction = onBrowse,
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            if (uiState.inProgress.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.continue_listening)) }
                items(uiState.inProgress, key = { "ip_${it.id}" }) { episode ->
                    EpisodeRow(
                        episode = episode,
                        isCurrent = playerState.currentEpisodeId == episode.id,
                        isPlaying = playerState.isPlaying,
                        onPlayToggle = { viewModel.playToggle(episode) },
                        onClick = { viewModel.open(episode); onOpenPlayer() },
                        onDownload = { viewModel.download(episode) },
                        onDeleteDownload = { viewModel.deleteDownload(episode) },
                        onTogglePlayed = { viewModel.markPlayed(episode, !episode.isFinished) },
                        onPlayNext = { viewModel.playNext(episode) },
                        onAddToQueue = { viewModel.addToQueue(episode) },
                        modifier = Modifier.animateItem(),
                    )
                    HorizontalDivider(Modifier.padding(start = EpisodeRowDividerStartInset))
                }
                item { Spacer(Modifier.height(CarneTheme.spacing.sm)) }
            }

            item { SectionHeader(stringResource(R.string.latest_episodes)) }
            items(uiState.latest, key = { "lt_${it.id}" }) { episode ->
                EpisodeRow(
                    episode = episode,
                    isCurrent = playerState.currentEpisodeId == episode.id,
                    isPlaying = playerState.isPlaying,
                    onPlayToggle = { viewModel.playToggle(episode) },
                    onClick = { viewModel.open(episode); onOpenPlayer() },
                    onDownload = { viewModel.download(episode) },
                    onDeleteDownload = { viewModel.deleteDownload(episode) },
                    onTogglePlayed = { viewModel.markPlayed(episode, !episode.isFinished) },
                    onPlayNext = { viewModel.playNext(episode) },
                    onAddToQueue = { viewModel.addToQueue(episode) },
                    modifier = Modifier.animateItem(),
                )
                HorizontalDivider(Modifier.padding(start = EpisodeRowDividerStartInset))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(CarneTheme.spacing.lg)
            .semantics { heading() },
    )
}
