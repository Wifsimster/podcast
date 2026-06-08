package com.ondes.podcast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.ondes.podcast.ui.theme.OndesTheme

/**
 * Show / episode artwork.
 *
 * The corner [shape] defaults to the shared [OndesTheme.shapes] vocabulary so
 * every artwork across the app reads with one consistent radius instead of a
 * per-call-site literal. Pass a [contentDescription] when the artwork is the
 * primary content of a screen (player, podcast header); leave it null in rows
 * where adjacent text already names the show, so TalkBack isn't redundant.
 */
@Composable
fun PodcastArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = OndesTheme.shapes.artworkMedium,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            Placeholder()
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { Placeholder() },
                error = { Placeholder() },
            )
        }
    }
}

@Composable
private fun Placeholder() {
    Icon(
        imageVector = Icons.Rounded.Podcasts,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(28.dp),
    )
}
