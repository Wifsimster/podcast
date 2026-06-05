package com.carne.podcast.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carne.podcast.data.local.DownloadState
import com.carne.podcast.data.local.EpisodeEntity
import com.carne.podcast.data.repository.PodcastRepository
import com.carne.podcast.download.DownloadManager
import com.carne.podcast.playback.PlaybackConnection
import com.carne.podcast.playback.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PodcastRepository,
    private val connection: PlaybackConnection,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    val inProgress: StateFlow<List<EpisodeEntity>> = repository.observeInProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latest: StateFlow<List<EpisodeEntity>> = repository.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerState: StateFlow<PlayerUiState> = connection.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _refreshing.value = true
            val feeds = repository.observeSubscriptions().first().map { it.feedUrl }
            repository.refreshAllSubscriptions(feeds)
            _refreshing.value = false
        }
    }

    fun playToggle(episode: EpisodeEntity) {
        val state = connection.state.value
        if (state.currentEpisodeId == episode.id && state.isPlaying) connection.pause()
        else connection.play(episode)
    }

    fun download(episode: EpisodeEntity) = downloadManager.enqueue(episode.id)
    fun deleteDownload(episode: EpisodeEntity) =
        downloadManager.deleteDownload(episode.id, episode.localFilePath)

    fun markPlayed(episode: EpisodeEntity, played: Boolean) {
        viewModelScope.launch { repository.setPlayed(episode.id, played) }
    }
}
