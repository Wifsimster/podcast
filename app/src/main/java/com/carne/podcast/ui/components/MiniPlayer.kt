package com.carne.podcast.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carne.podcast.playback.PlayerUiState
import com.carne.podcast.ui.theme.CarneTheme

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
            // Expressive wavy progress — the mini-player's signature flourish.
            // Decorative here: the full player exposes the scrubber to TalkBack,
            // so this duplicate progress is hidden from the accessibility tree.
            LinearWavyProgressIndicator(
                progress = { progress },
                color = CarneTheme.colors.brand,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CarneTheme.spacing.md, vertical = CarneTheme.spacing.xs)
                    .clearAndSetSemantics {},
            )
            Row(
                modifier = Modifier
                    .clickable(onClick = onClick, onClickLabel = "Open player")
                    .padding(horizontal = CarneTheme.spacing.sm, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PodcastArtwork(
                    url = state.artworkUri,
                    modifier = Modifier.size(44.dp),
                    shape = CarneTheme.shapes.artworkSmall,
                )
                Spacer(Modifier.width(CarneTheme.spacing.md))
                Box(Modifier.weight(1f)) {
                    Text(
                        text = state.title.ifEmpty { "Playing" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.semantics {
                        contentDescription = "Play or pause"
                        stateDescription = if (state.isPlaying) "Playing" else "Paused"
                    },
                ) {
                    PlayPauseIcon(
                        isPlaying = state.isPlaying,
                        playIcon = Icons.Rounded.PlayArrow,
                        pauseIcon = Icons.Rounded.Pause,
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
