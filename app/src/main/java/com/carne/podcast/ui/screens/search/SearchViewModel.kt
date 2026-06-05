package com.carne.podcast.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carne.podcast.data.remote.PodcastSearchResult
import com.carne.podcast.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: PodcastRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

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
                error = if (results.isEmpty()) "No podcasts found" else null,
            )
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
                    error = "Subscribed",
                )
            } else {
                _state.value.copy(loading = false, error = "Couldn't load that feed")
            }
        }
    }
}
