package com.carne.podcast.ui.screens.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.data.remote.PodcastSearchResult
import com.carne.podcast.data.remote.PodcastTheme
import com.carne.podcast.ui.components.CarneEmptyState
import com.carne.podcast.ui.components.PodcastArtwork
import com.carne.podcast.ui.theme.CarneTheme

@Composable
fun SearchScreen(
    contentPadding: PaddingValues,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(CarneTheme.spacing.lg),
            placeholder = { Text("Search podcasts or paste a feed URL") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
        )

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            state.results.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.results, key = { it.feedUrl }) { result ->
                    PodcastResultRow(
                        result = result,
                        subscribed = result.feedUrl in state.subscribedFeeds,
                        onSubscribe = { viewModel.subscribe(result) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            state.error != null -> CarneEmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "No luck there",
                message = state.error.orEmpty(),
                actionLabel = "Try again",
                onAction = viewModel::search,
            )

            // No active search → propose top shows the user can browse by theme.
            state.query.isBlank() -> BrowseByTheme(
                state = state,
                onSelectTheme = viewModel::selectTheme,
                onSubscribe = viewModel::subscribe,
            )

            else -> CarneEmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "No matches",
                message = "Spice up the spelling, or try another show.",
            )
        }
    }
}

/**
 * The Discover landing content: a strip of theme chips and, below it, the top
 * podcasts for the picked theme — a ready-made proposition so people can find a
 * great show without knowing its name.
 */
@Composable
private fun BrowseByTheme(
    state: SearchUiState,
    onSelectTheme: (PodcastTheme) -> Unit,
    onSubscribe: (PodcastSearchResult) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Browse by theme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(
                start = CarneTheme.spacing.lg,
                end = CarneTheme.spacing.lg,
                bottom = CarneTheme.spacing.sm,
            ),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = CarneTheme.spacing.lg),
        ) {
            items(state.themes, key = { it.genreId }) { theme ->
                FilterChip(
                    selected = theme == state.selectedTheme,
                    onClick = { onSelectTheme(theme) },
                    label = { Text(theme.label) },
                    modifier = Modifier.padding(end = CarneTheme.spacing.sm),
                )
            }
        }

        Spacer(Modifier.height(CarneTheme.spacing.md))

        when {
            state.themeLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            state.themeResults.isEmpty() -> CarneEmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "Nothing to show",
                message = "Couldn't load top shows for ${state.selectedTheme?.label.orEmpty()}. " +
                    "Check your connection and try another theme. 🌶️",
            )

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.themeResults, key = { it.feedUrl }) { result ->
                    PodcastResultRow(
                        result = result,
                        subscribed = result.feedUrl in state.subscribedFeeds,
                        onSubscribe = { onSubscribe(result) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/** A single podcast result row with artwork, title/author and a subscribe action. */
@Composable
private fun PodcastResultRow(
    result: PodcastSearchResult,
    subscribed: Boolean,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = CarneTheme.spacing.lg,
                vertical = CarneTheme.spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PodcastArtwork(
            url = result.imageUrl,
            modifier = Modifier.size(56.dp),
            shape = CarneTheme.shapes.artworkSmall,
        )
        Spacer(Modifier.width(CarneTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                result.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(CarneTheme.spacing.sm))
        Button(
            onClick = onSubscribe,
            enabled = !subscribed,
            modifier = Modifier.semantics {
                contentDescription =
                    if (subscribed) "Subscribed to ${result.title}"
                    else "Subscribe to ${result.title}"
            },
        ) {
            Text(if (subscribed) "Added" else "Subscribe")
        }
    }
}
