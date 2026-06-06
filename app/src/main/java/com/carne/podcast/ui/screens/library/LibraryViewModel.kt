package com.carne.podcast.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carne.podcast.data.local.PodcastWithCount
import com.carne.podcast.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** How the subscriptions grid is ordered. */
enum class LibrarySort { RECENT, ALPHABETICAL, UNPLAYED }

@HiltViewModel
class LibraryViewModel @Inject constructor(
    repository: PodcastRepository,
) : ViewModel() {

    private val _sort = MutableStateFlow(LibrarySort.RECENT)
    val sort: StateFlow<LibrarySort> = _sort.asStateFlow()

    val subscriptions: StateFlow<List<PodcastWithCount>> =
        combine(repository.observeSubscriptionsWithCounts(), _sort) { items, sort ->
            when (sort) {
                LibrarySort.ALPHABETICAL ->
                    items.sortedBy { it.podcast.title.lowercase() }
                LibrarySort.RECENT ->
                    items.sortedByDescending { it.podcast.lastUpdated }
                LibrarySort.UNPLAYED ->
                    items.sortedWith(
                        compareByDescending<PodcastWithCount> { it.unplayedCount }
                            .thenBy { it.podcast.title.lowercase() },
                    )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSort(sort: LibrarySort) {
        _sort.value = sort
    }
}
