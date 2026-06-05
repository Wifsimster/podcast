package com.carne.podcast.ui.screens.podcast

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carne.podcast.data.local.EpisodeEntity
import com.carne.podcast.data.local.PodcastEntity
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PodcastRepository,
    private val connection: PlaybackConnection,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    val feedUrl: String = Uri.decode(checkNotNull(savedStateHandle.get<String>("feedUrl")))

    val podcast: StateFlow<PodcastEntity?> = repository.observePodcast(feedUrl)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val episodes: StateFlow<List<EpisodeEntity>> = repository.observeEpisodes(feedUrl)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerState: StateFlow<PlayerUiState> = connection.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _refreshing.value = true
            runCatching { repository.refreshFeed(feedUrl) }
            _refreshing.value = false
        }
    }

    fun toggleSubscribe() {
        viewModelScope.launch {
            val current = podcast.value
            if (current?.subscribed == true) repository.unsubscribe(feedUrl)
            else repository.subscribe(feedUrl)
        }
    }

    fun playToggle(episode: EpisodeEntity) {
        val state = connection.state.value
        if (state.currentEpisodeId == episode.id && state.isPlaying) connection.pause()
        else connection.play(episode, episodes.value)
    }

    /** Load (and resume) an episode so the now-playing screen has something to show. */
    fun open(episode: EpisodeEntity) = connection.play(episode, episodes.value)

    fun download(episode: EpisodeEntity) = downloadManager.enqueue(episode.id)
    fun deleteDownload(episode: EpisodeEntity) =
        downloadManager.deleteDownload(episode.id, episode.localFilePath)

    fun markPlayed(episode: EpisodeEntity, played: Boolean) {
        viewModelScope.launch { repository.setPlayed(episode.id, played) }
    }
}
