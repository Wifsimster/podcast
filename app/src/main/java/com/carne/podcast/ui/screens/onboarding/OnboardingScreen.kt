package com.carne.podcast.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.R
import com.carne.podcast.data.remote.PodcastSearchResult
import com.carne.podcast.ui.components.CarneEmptyState
import com.carne.podcast.ui.components.PodcastArtwork
import com.carne.podcast.ui.theme.CarneTheme

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // System back from the proposals step returns to the theme picker rather than
    // exiting onboarding entirely.
    BackHandler(enabled = state.step == OnboardingStep.PROPOSALS) { viewModel.back() }

    when (state.step) {
        OnboardingStep.THEMES -> ThemeStep(
            state = state,
            themes = viewModel.themes,
            onToggle = viewModel::toggle,
            onContinue = viewModel::proceedToProposals,
            onSkip = viewModel::skip,
        )
        OnboardingStep.PROPOSALS -> ProposalStep(
            state = state,
            onToggle = viewModel::toggleProposal,
            onBack = viewModel::back,
            onRetry = viewModel::retryProposals,
            onSubscribe = viewModel::subscribeSelectedAndFinish,
            onSkip = viewModel::skip,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeStep(
    state: OnboardingUiState,
    themes: List<com.carne.podcast.data.remote.PodcastTheme>,
    onToggle: (com.carne.podcast.data.remote.PodcastTheme) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(CarneTheme.spacing.xl),
    ) {
        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(CarneTheme.spacing.sm))
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(CarneTheme.spacing.xl))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CarneTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(CarneTheme.spacing.xs),
        ) {
            themes.forEach { theme ->
                FilterChip(
                    selected = theme.genreId in state.selectedThemes,
                    onClick = { onToggle(theme) },
                    label = { Text(theme.label) },
                )
            }
        }

        Spacer(Modifier.height(CarneTheme.spacing.xxl))
        Button(
            onClick = onContinue,
            enabled = state.selectedThemes.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_continue))
        }
        Spacer(Modifier.height(CarneTheme.spacing.sm))
        TextButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.skip))
        }
    }
}

@Composable
private fun ProposalStep(
    state: OnboardingUiState,
    onToggle: (PodcastSearchResult) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSubscribe: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(CarneTheme.spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Spacer(Modifier.width(CarneTheme.spacing.sm))
            Text(
                text = stringResource(R.string.onboarding_proposals_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(CarneTheme.spacing.xs))
        Text(
            text = stringResource(R.string.onboarding_proposals_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(CarneTheme.spacing.md))

        when {
            state.proposalsLoading -> Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }

            state.proposalsError || state.proposals.isEmpty() -> Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                CarneEmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = stringResource(R.string.nothing_to_show_title),
                    message = stringResource(R.string.onboarding_proposals_error),
                    actionLabel = stringResource(R.string.try_again),
                    onAction = onRetry,
                )
            }

            else -> LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(state.proposals, key = { it.feedUrl }) { result ->
                    SelectableProposalRow(
                        result = result,
                        checked = result.feedUrl in state.chosenFeeds,
                        onToggle = { onToggle(result) },
                    )
                }
            }
        }

        Spacer(Modifier.height(CarneTheme.spacing.sm))
        if (state.subscribing) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = onSubscribe,
                enabled = state.chosenFeeds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_subscribe_selected))
            }
            Spacer(Modifier.height(CarneTheme.spacing.sm))
            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.skip))
            }
        }
    }
}

@Composable
private fun SelectableProposalRow(
    result: PodcastSearchResult,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cd = stringResource(R.string.subscribe_to, result.title)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics { contentDescription = cd }
            .padding(vertical = CarneTheme.spacing.sm),
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
        // Null callback: the whole row is the toggle target (above).
        Checkbox(checked = checked, onCheckedChange = null)
    }
}
