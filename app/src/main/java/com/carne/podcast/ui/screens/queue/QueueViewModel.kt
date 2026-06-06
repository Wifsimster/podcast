package com.carne.podcast.ui.screens.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carne.podcast.data.local.EpisodeEntity
import com.carne.podcast.data.repository.PodcastRepository
import com.carne.podcast.playback.PlaybackConnection
import com.carne.podcast.playback.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repository: PodcastRepository,
    private val connection: PlaybackConnection,
) : ViewModel() {

    val queue: StateFlow<List<EpisodeEntity>> = repository.observeQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerState: StateFlow<PlayerUiState> = connection.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    /** Tapping a row plays the queue starting there; tapping the current one toggles. */
    fun playToggle(episode: EpisodeEntity) {
        val state = connection.state.value
        if (state.currentEpisodeId == episode.id && state.isPlaying) {
            connection.pause()
            return
        }
        val index = queue.value.indexOfFirst { it.id == episode.id }
        if (index >= 0) connection.playFromQueue(queue.value, index)
    }

    fun open(episode: EpisodeEntity) {
        val index = queue.value.indexOfFirst { it.id == episode.id }
        if (index >= 0) connection.playFromQueue(queue.value, index)
    }

    fun remove(episode: EpisodeEntity) {
        viewModelScope.launch { repository.removeFromQueue(episode.id) }
    }

    fun clear() {
        viewModelScope.launch { repository.clearQueue() }
    }

    fun moveUp(index: Int) = reorder(index, index - 1)
    fun moveDown(index: Int) = reorder(index, index + 1)

    private fun reorder(from: Int, to: Int) {
        val ids = queue.value.map { it.id }.toMutableList()
        if (from !in ids.indices || to !in ids.indices) return
        val moved = ids.removeAt(from)
        ids.add(to, moved)
        viewModelScope.launch { repository.setQueueOrder(ids) }
    }
}
