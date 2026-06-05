package com.carne.podcast.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carne.podcast.playback.PlayerUiState

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.currentEpisodeId == null) return
    val progress = if (state.durationMs > 0)
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
            Row(
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PodcastArtwork(
                    url = state.artworkUri,
                    modifier = Modifier.size(44.dp),
                    cornerRadius = 6.dp,
                )
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    Text(
                        text = state.title.ifEmpty { "Playing" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause
                        else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(30.dp),
                    )
                }
                IconButton(onClick = onForward) {
                    Icon(
                        Icons.Rounded.Forward30,
                        contentDescription = "Forward 30 seconds",
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
