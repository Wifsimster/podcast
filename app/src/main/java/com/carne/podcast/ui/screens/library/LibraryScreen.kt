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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.BuildConfig
import com.carne.podcast.R
import com.carne.podcast.ui.components.CarneEmptyState
import com.carne.podcast.ui.components.PodcastArtwork
import com.carne.podcast.ui.screens.downloads.DownloadsScreen
import com.carne.podcast.ui.theme.CarneTheme

/** The two views the Library hosts: subscribed shows and offline downloads. */
private enum class LibraryTab(val labelRes: Int) {
    PODCASTS(R.string.library_tab_podcasts),
    DOWNLOADS(R.string.nav_downloads),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenPodcast: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onBrowse: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(LibraryTab.PODCASTS) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_library)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CarneTheme.spacing.lg, vertical = CarneTheme.spacing.sm),
            ) {
                LibraryTab.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        shape = SegmentedButtonDefaults.itemShape(index, LibraryTab.entries.size),
                    ) {
                        Text(stringResource(entry.labelRes))
                    }
                }
            }

            when (tab) {
                LibraryTab.PODCASTS -> if (subscriptions.isEmpty()) {
                    CarneEmptyState(
                        icon = Icons.Rounded.LibraryMusic,
                        title = stringResource(R.string.library_empty_title),
                        message = stringResource(R.string.library_empty_message),
                        actionLabel = stringResource(R.string.browse_podcasts),
                        onAction = onBrowse,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = CarneTheme.spacing.lg, end = CarneTheme.spacing.lg,
                            top = CarneTheme.spacing.lg,
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
                                text = stringResource(R.string.library_version, BuildConfig.VERSION_NAME),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = CarneTheme.spacing.sm),
                            )
                        }
                    }
                }

                LibraryTab.DOWNLOADS -> DownloadsScreen(
                    onOpenPlayer = onOpenPlayer,
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                )
            }
        }
    }
}
