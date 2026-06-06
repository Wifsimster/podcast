package com.carne.podcast.ui.screens.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carne.podcast.R
import com.carne.podcast.data.remote.PodcastSearchResult
import com.carne.podcast.data.remote.PodcastTheme
import com.carne.podcast.data.remote.PodcastThemes
import com.carne.podcast.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<PodcastSearchResult> = emptyList(),
    val subscribedFeeds: Set<String> = emptySet(),
    val error: String? = null,
    // Browse-by-theme: a proposition of top shows for the picked theme.
    val themes: List<PodcastTheme> = PodcastThemes.all,
    val selectedTheme: PodcastTheme? = null,
    val themeLoading: Boolean = false,
    val themeResults: List<PodcastSearchResult> = emptyList(),
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PodcastRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    init {
        // Open Discover already proposing the top shows from the first theme.
        _state.value.themes.firstOrNull()?.let(::selectTheme)
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun search() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            // Allow pasting a raw feed URL directly.
            if (q.startsWith("http") && (q.contains("rss") || q.contains("feed") || q.contains(".xml"))) {
                subscribeByUrl(q)
                return@launch
            }
            val results = repository.search(q)
            _state.value = _state.value.copy(
                loading = false,
                results = results,
                error = if (results.isEmpty()) {
                    context.getString(R.string.search_error_none)
                } else null,
            )
        }
    }

    /** Pick a theme and load its top shows as a proposition. */
    fun selectTheme(theme: PodcastTheme) {
        if (_state.value.themeLoading && _state.value.selectedTheme == theme) return
        _state.value = _state.value.copy(
            selectedTheme = theme,
            themeLoading = true,
            themeResults = emptyList(),
        )
        viewModelScope.launch {
            val top = repository.topPodcasts(theme.genreId, limit = 3)
            // Ignore a stale response if the user has since tapped another chip.
            if (_state.value.selectedTheme != theme) return@launch
            _state.value = _state.value.copy(themeLoading = false, themeResults = top)
        }
    }

    fun subscribe(result: PodcastSearchResult) {
        viewModelScope.launch {
            repository.subscribe(result.feedUrl)
            _state.value = _state.value.copy(
                subscribedFeeds = _state.value.subscribedFeeds + result.feedUrl
            )
        }
    }

    private fun subscribeByUrl(url: String) {
        viewModelScope.launch {
            val outcome = repository.subscribe(url)
            _state.value = if (outcome.isSuccess) {
                _state.value.copy(
                    loading = false,
                    subscribedFeeds = _state.value.subscribedFeeds + url,
                    error = context.getString(R.string.search_subscribed),
                )
            } else {
                _state.value.copy(
                    loading = false,
                    error = context.getString(R.string.search_feed_error),
                )
            }
        }
    }
}
