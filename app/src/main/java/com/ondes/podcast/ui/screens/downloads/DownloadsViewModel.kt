package com.ondes.podcast.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ondes.podcast.data.local.EpisodeEntity
import com.ondes.podcast.data.repository.PodcastRepository
import com.ondes.podcast.download.DownloadManager
import com.ondes.podcast.playback.PlaybackConnection
import com.ondes.podcast.playback.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: PodcastRepository,
    private val connection: PlaybackConnection,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    val downloads: StateFlow<List<EpisodeEntity>> = repository.observeDownloaded()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerState: StateFlow<PlayerUiState> = connection.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    fun playToggle(episode: EpisodeEntity) {
        val state = connection.state.value
        if (state.currentEpisodeId == episode.id && state.isPlaying) connection.pause()
        else connection.play(episode, downloads.value)
    }

    /** Load (and resume) an episode so the now-playing screen has something to show. */
    fun open(episode: EpisodeEntity) = connection.play(episode, downloads.value)

    fun deleteDownload(episode: EpisodeEntity) =
        downloadManager.deleteDownload(episode.id, episode.localFilePath)

    fun markPlayed(episode: EpisodeEntity, played: Boolean) {
        viewModelScope.launch { repository.setPlayed(episode.id, played) }
    }

    fun playNext(episode: EpisodeEntity) {
        viewModelScope.launch { repository.playNextInQueue(episode.id) }
    }

    fun addToQueue(episode: EpisodeEntity) {
        viewModelScope.launch { repository.addToQueueEnd(episode.id) }
    }
}
