package com.carne.podcast.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
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
                        val subscribed = result.feedUrl in state.subscribedFeeds
                        Button(
                            onClick = { viewModel.subscribe(result) },
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
            }

            state.error != null -> CarneEmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "No luck there",
                message = state.error.orEmpty(),
                actionLabel = "Try again",
                onAction = viewModel::search,
            )

            state.query.isBlank() -> CarneEmptyState(
                icon = Icons.Rounded.Search,
                title = "Discover podcasts",
                message = "Search by name — or paste an RSS feed URL to add any show directly. 🌶️",
            )

            else -> CarneEmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "No matches",
                message = "Spice up the spelling, or try another show.",
            )
        }
    }
}
