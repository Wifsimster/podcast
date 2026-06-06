package com.carne.podcast.ui.screens.queue

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.R
import com.carne.podcast.data.local.EpisodeEntity
import com.carne.podcast.ui.components.CarneEmptyState
import com.carne.podcast.ui.components.PlayPauseIcon
import com.carne.podcast.ui.components.PodcastArtwork
import com.carne.podcast.ui.components.formatDate
import com.carne.podcast.ui.components.formatDurationLabel
import com.carne.podcast.ui.theme.CarneTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onOpenPlayer: () -> Unit,
    onBrowse: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { QueueTopBar(showClear = queue.isNotEmpty(), onClear = viewModel::clear) },
    ) { padding ->
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                CarneEmptyState(
                    icon = Icons.Rounded.QueueMusic,
                    title = stringResource(R.string.queue_empty_title),
                    message = stringResource(R.string.queue_empty_message),
                    actionLabel = stringResource(R.string.browse_podcasts),
                    onAction = onBrowse,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
        ) {
            itemsIndexed(queue, key = { _, e -> e.id }) { index, episode ->
                QueueRow(
                    episode = episode,
                    index = index,
                    lastIndex = queue.lastIndex,
                    isCurrent = playerState.currentEpisodeId == episode.id,
                    isPlaying = playerState.isPlaying,
                    onPlayToggle = { viewModel.playToggle(episode) },
                    onClick = { viewModel.open(episode); onOpenPlayer() },
                    onMoveUp = { viewModel.moveUp(index) },
                    onMoveDown = { viewModel.moveDown(index) },
                    onRemove = { viewModel.remove(episode) },
                    modifier = Modifier.animateItem(),
                )
                HorizontalDivider(Modifier.padding(start = CarneTheme.spacing.lg))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueTopBar(showClear: Boolean, onClear: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.queue_title)) },
        actions = {
            if (showClear) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.queue_clear)) }
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueRow(
    episode: EpisodeEntity,
    index: Int,
    lastIndex: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playOrPauseLabel = stringResource(R.string.play_or_pause_episode)
    val playingLabel = stringResource(R.string.playing)
    val pausedLabel = stringResource(R.string.paused)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onRemove)
            .padding(horizontal = CarneTheme.spacing.lg, vertical = CarneTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PodcastArtwork(
            url = episode.imageUrl,
            modifier = Modifier.size(48.dp),
            shape = CarneTheme.shapes.artworkSmall,
        )
        Spacer(Modifier.width(CarneTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(formatDate(episode.pubDate))
                    val dur = formatDurationLabel(episode.durationMs)
                    if (dur.isNotEmpty()) append("  ·  $dur")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = onPlayToggle,
            modifier = Modifier.semantics {
                contentDescription = playOrPauseLabel
                stateDescription = if (isCurrent && isPlaying) playingLabel else pausedLabel
            },
        ) {
            PlayPauseIcon(
                isPlaying = isCurrent && isPlaying,
                playIcon = Icons.Rounded.PlayCircle,
                pauseIcon = Icons.Rounded.PauseCircle,
                tint = CarneTheme.colors.brand,
                modifier = Modifier.size(32.dp),
            )
        }
        IconButton(onClick = onMoveUp, enabled = index > 0) {
            Icon(
                Icons.Rounded.KeyboardArrowUp,
                contentDescription = stringResource(R.string.move_up),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onMoveDown, enabled = index < lastIndex) {
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.move_down),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Rounded.RemoveCircleOutline,
                contentDescription = stringResource(R.string.remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
