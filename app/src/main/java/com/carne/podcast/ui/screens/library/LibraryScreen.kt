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
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.BuildConfig
import com.carne.podcast.R
import com.carne.podcast.data.local.PodcastWithCount
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
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(LibraryTab.PODCASTS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_library)) },
                actions = {
                    if (tab == LibraryTab.PODCASTS && subscriptions.isNotEmpty()) {
                        SortMenu(current = sort, onSelect = viewModel::setSort)
                    }
                },
            )
        },
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
                        items(subscriptions, key = { it.podcast.feedUrl }) { item ->
                            PodcastTile(
                                item = item,
                                onClick = { onOpenPodcast(item.podcast.feedUrl) },
                                modifier = Modifier.animateItem(),
                            )
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

@Composable
private fun PodcastTile(
    item: PodcastWithCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unplayedCd = if (item.unplayedCount > 0) {
        pluralStringResource(
            R.plurals.unplayed_count, item.unplayedCount, item.unplayedCount,
        )
    } else ""
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            PodcastArtwork(
                url = item.podcast.imageUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                shape = CarneTheme.shapes.artworkMedium,
            )
            if (item.unplayedCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(CarneTheme.spacing.sm)
                        .semantics { contentDescription = unplayedCd },
                ) {
                    Text(if (item.unplayedCount > 99) "99+" else "${item.unplayedCount}")
                }
            }
        }
        Spacer(Modifier.height(CarneTheme.spacing.sm))
        Text(
            text = item.podcast.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.podcast.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SortMenu(current: LibrarySort, onSelect: (LibrarySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val labelFor = mapOf(
        LibrarySort.RECENT to R.string.sort_recent,
        LibrarySort.ALPHABETICAL to R.string.sort_alphabetical,
        LibrarySort.UNPLAYED to R.string.sort_unplayed,
    )
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.AutoMirrored.Rounded.Sort,
                contentDescription = stringResource(R.string.sort_library),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LibrarySort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelFor.getValue(option))) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    trailingIcon = if (option == current) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else null,
                )
            }
        }
    }
}
