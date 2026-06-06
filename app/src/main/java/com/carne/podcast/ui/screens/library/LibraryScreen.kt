package com.carne.podcast.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.BuildConfig
import com.carne.podcast.R
import com.carne.podcast.data.local.PodcastEntity
import com.carne.podcast.ui.components.CarneEmptyState
import com.carne.podcast.ui.components.CarneTopAppBar
import com.carne.podcast.ui.components.PodcastArtwork
import com.carne.podcast.ui.screens.downloads.DownloadsScreen
import com.carne.podcast.ui.theme.CarneTheme

/**
 * The Library is the user's "owned content" hub. It hosts two tabs so everything
 * they've kept lives in one place: the shows they subscribe to and the episodes
 * they've saved offline — the latter folded in here (it used to be its own
 * bottom-bar tab) to keep the bottom bar to four core destinations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenPodcast: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onBrowse: () -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(R.string.library_tab_shows, R.string.library_tab_downloads)

    Scaffold(
        topBar = {
            Column {
                CarneTopAppBar(
                    title = stringResource(R.string.nav_library),
                    onOpenSettings = onOpenSettings,
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, labelRes ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(stringResource(labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val innerPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        )
        when (selectedTab) {
            0 -> SubscriptionsTab(
                subscriptions = subscriptions,
                onOpenPodcast = onOpenPodcast,
                onBrowse = onBrowse,
                contentPadding = innerPadding,
            )

            else -> DownloadsScreen(
                onOpenPlayer = onOpenPlayer,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun SubscriptionsTab(
    subscriptions: List<PodcastEntity>,
    onOpenPodcast: (String) -> Unit,
    onBrowse: () -> Unit,
    contentPadding: PaddingValues,
) {
    if (subscriptions.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            CarneEmptyState(
                icon = Icons.Rounded.LibraryMusic,
                title = stringResource(R.string.library_empty_title),
                message = stringResource(R.string.library_empty_message),
                actionLabel = stringResource(R.string.browse_podcasts),
                onAction = onBrowse,
            )
        }
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

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "🌶️ Carne v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
