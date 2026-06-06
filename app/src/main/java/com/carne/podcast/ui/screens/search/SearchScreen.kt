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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.R
import com.carne.podcast.data.remote.PodcastSearchResult
import com.carne.podcast.data.remote.PodcastTheme
import com.carne.podcast.ui.components.CarneEmptyState
import com.carne.podcast.ui.components.CarneTopAppBar
import com.carne.podcast.ui.components.PodcastArtwork
import com.carne.podcast.ui.theme.CarneTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CarneTopAppBar(
                title = stringResource(R.string.nav_search),
                onOpenSettings = onOpenSettings,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
        ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(CarneTheme.spacing.lg),
            placeholder = {
                Text(
                    stringResource(R.string.search_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
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
                title = stringResource(R.string.search_no_luck_title),
                message = state.error.orEmpty(),
                actionLabel = stringResource(R.string.try_again),
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
                title = stringResource(R.string.search_no_matches_title),
                message = stringResource(R.string.search_no_matches_message),
            )
        }
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
            text = stringResource(R.string.browse_by_theme),
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
                title = stringResource(R.string.nothing_to_show_title),
                message = stringResource(
                    R.string.theme_load_error,
                    state.selectedTheme?.label.orEmpty(),
                ),
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
        val subscribedCd = stringResource(R.string.subscribed_to, result.title)
        val subscribeCd = stringResource(R.string.subscribe_to, result.title)
        Button(
            onClick = onSubscribe,
            enabled = !subscribed,
            modifier = Modifier.semantics {
                contentDescription = if (subscribed) subscribedCd else subscribeCd
            },
        ) {
            Text(
                if (subscribed) stringResource(R.string.subscribed_added)
                else stringResource(R.string.subscribe)
            )
        }
    }
}
