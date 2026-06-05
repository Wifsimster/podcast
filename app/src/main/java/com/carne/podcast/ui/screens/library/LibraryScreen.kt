package com.carne.podcast.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.ui.components.CarneEmptyState
import com.carne.podcast.ui.components.PodcastArtwork
import com.carne.podcast.ui.theme.CarneTheme

@Composable
fun LibraryScreen(
    onOpenPodcast: (String) -> Unit,
    onBrowse: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()

    if (subscriptions.isEmpty()) {
        CarneEmptyState(
            icon = Icons.Rounded.LibraryMusic,
            title = "Your library's empty",
            message = "Subscribe to shows and they'll live here, ready when you are. Let's get cooking. 🌶️",
            actionLabel = "Browse podcasts",
            onAction = onBrowse,
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = CarneTheme.spacing.lg, end = CarneTheme.spacing.lg,
            top = CarneTheme.spacing.lg + contentPadding.calculateTopPadding(),
            bottom = CarneTheme.spacing.lg + contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(CarneTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(CarneTheme.spacing.lg),
    ) {
        items(subscriptions, key = { it.feedUrl }) { podcast ->
            Column(
                modifier = Modifier
                    .animateItem()
                    .clickable { onOpenPodcast(podcast.feedUrl) },
            ) {
                PodcastArtwork(
                    url = podcast.imageUrl,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    shape = CarneTheme.shapes.artworkMedium,
                )
                Spacer(Modifier.height(CarneTheme.spacing.sm))
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = podcast.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
