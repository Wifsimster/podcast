package com.carne.podcast.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carne.podcast.data.local.DownloadState
import com.carne.podcast.data.local.EpisodeEntity
import com.carne.podcast.ui.theme.CarneTheme

/**
 * Start inset that aligns a list divider with an [EpisodeRow]'s text column,
 * clearing the leading artwork: row start padding + artwork width + gap.
 */
val EpisodeRowDividerStartInset = 84.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeRow(
    episode: EpisodeEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
    onTogglePlayed: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true },
                )
                .padding(horizontal = CarneTheme.spacing.lg, vertical = CarneTheme.spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
        if (showArtwork) {
            PodcastArtwork(
                url = episode.imageUrl,
                modifier = Modifier.size(56.dp),
                shape = CarneTheme.shapes.artworkSmall,
            )
            Spacer(Modifier.width(CarneTheme.spacing.md))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (episode.isFinished) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Played",
                        tint = CarneTheme.colors.played,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
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
            if (episode.positionMs > 0 && !episode.isFinished && episode.durationMs > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = {
                        (episode.positionMs.toFloat() / episode.durationMs).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                )
            }
        }
        Spacer(Modifier.width(CarneTheme.spacing.sm))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onPlayToggle,
                modifier = Modifier.semantics {
                    contentDescription = "Play or pause episode"
                    stateDescription = if (isCurrent && isPlaying) "Playing" else "Paused"
                },
            ) {
                PlayPauseIcon(
                    isPlaying = isCurrent && isPlaying,
                    playIcon = Icons.Rounded.PlayCircle,
                    pauseIcon = Icons.Rounded.PauseCircle,
                    tint = CarneTheme.colors.brand,
                    modifier = Modifier.size(36.dp),
                )
            }
            DownloadAffordance(episode, onDownload, onDeleteDownload)
        }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(if (episode.isFinished) "Mark as unplayed" else "Mark as played")
                },
                leadingIcon = {
                    Icon(
                        if (episode.isFinished) Icons.Rounded.RemoveDone
                        else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                    )
                },
                onClick = {
                    onTogglePlayed()
                    menuExpanded = false
                },
            )
        }
    }
}

@Composable
private fun DownloadAffordance(
    episode: EpisodeEntity,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
) {
    when (episode.downloadState) {
        DownloadState.DOWNLOADED -> IconButton(onClick = onDeleteDownload) {
            Icon(
                Icons.Rounded.DownloadDone,
                contentDescription = "Downloaded — tap to delete",
                tint = CarneTheme.colors.downloaded,
                modifier = Modifier.size(20.dp),
            )
        }
        // Expressive activity indicator while the episode is fetching.
        DownloadState.DOWNLOADING, DownloadState.QUEUED -> IconButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.semantics { contentDescription = "Downloading" },
        ) {
            LoadingIndicator(
                color = CarneTheme.colors.ember,
                modifier = Modifier.size(20.dp),
            )
        }
        else -> IconButton(onClick = onDownload) {
            Icon(
                Icons.Rounded.Download,
                contentDescription = "Download",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
