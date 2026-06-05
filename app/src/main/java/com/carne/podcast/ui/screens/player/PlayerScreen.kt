package com.carne.podcast.ui.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.ui.components.PodcastArtwork
import com.carne.podcast.ui.components.formatTime
import com.carne.podcast.ui.components.stripHtml

@Composable
fun PlayerScreen(
    onClose: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val episode by viewModel.currentEpisode.collectAsStateWithLifecycle()
    val sleepRemaining by viewModel.sleepRemainingMs.collectAsStateWithLifecycle()

    // Local scrub state so the thumb tracks the finger without fighting ticks.
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableStateOf(0f) }

    val duration = state.durationMs.coerceAtLeast(0)
    val maxValue = if (duration > 0) duration.toFloat() else 1f
    val sliderPosition = (if (scrubbing) scrubValue
    else if (duration > 0) state.positionMs.toFloat() else 0f).coerceIn(0f, maxValue)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close")
            }
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
        PodcastArtwork(
            url = state.artworkUri ?: episode?.imageUrl,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            cornerRadius = 20.dp,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = state.title.ifEmpty { episode?.title.orEmpty() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(20.dp))
        Slider(
            value = sliderPosition,
            onValueChange = { scrubbing = true; scrubValue = it },
            onValueChangeFinished = {
                viewModel.seekTo(scrubValue.toLong())
                scrubbing = false
            },
            valueRange = 0f..maxValue,
            enabled = duration > 0,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(sliderPosition.toLong()), style = MaterialTheme.typography.bodySmall)
            Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::seekBack, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Replay10, "Back 10 seconds", Modifier.size(38.dp))
            }
            IconButton(
                onClick = viewModel::playPause,
                modifier = Modifier.size(80.dp),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause
                    else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                )
            }
            IconButton(onClick = viewModel::seekForward, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Forward30, "Forward 30 seconds", Modifier.size(38.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SpeedControl(current = state.speed, onSelect = viewModel::setSpeed)
            SleepControl(
                remainingMs = sleepRemaining,
                onSelect = { viewModel.startSleepTimer(it) },
                onCancel = viewModel::cancelSleepTimer,
            )
        }

        episode?.description?.takeIf { it.isNotBlank() }?.let { desc ->
            Spacer(Modifier.height(24.dp))
            Text("Episode notes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stripHtml(desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SpeedControl(current: Float, onSelect: (Float) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val speeds = listOf(0.8f, 1.0f, 1.2f, 1.5f, 1.8f, 2.0f, 3.0f)
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(6.dp))
            Text("${trimSpeed(current)}×")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            speeds.forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${trimSpeed(speed)}×") },
                    onClick = { onSelect(speed); open = false },
                )
            }
        }
    }
}

@Composable
private fun SleepControl(remainingMs: Long, onSelect: (Int) -> Unit, onCancel: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val options = listOf(5, 15, 30, 45, 60)
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(6.dp))
            Text(if (remainingMs > 0) formatTime(remainingMs) else "Sleep")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text("$minutes min") },
                    onClick = { onSelect(minutes); open = false },
                )
            }
            if (remainingMs > 0) {
                DropdownMenuItem(
                    text = { Text("Cancel timer") },
                    onClick = { onCancel(); open = false },
                )
            }
        }
    }
}

private fun trimSpeed(speed: Float): String =
    if (speed == speed.toLong().toFloat()) speed.toLong().toString()
    else speed.toString()
