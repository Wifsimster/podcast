package com.carne.podcast.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carne.podcast.data.local.Chapter
import com.carne.podcast.data.local.EpisodeEntity
import com.carne.podcast.data.repository.PodcastRepository
import com.carne.podcast.playback.PlaybackConnection
import com.carne.podcast.playback.PlayerUiState
import com.carne.podcast.playback.SleepTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val connection: PlaybackConnection,
    private val sleepTimer: SleepTimer,
    private val repository: PodcastRepository,
) : ViewModel() {

    val playerState: StateFlow<PlayerUiState> = connection.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    /** Full episode entity for the currently loaded item (rich now-playing UI). */
    val currentEpisode: StateFlow<EpisodeEntity?> = connection.state
        .map { it.currentEpisodeId }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.observeEpisode(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Chapters for the loaded episode (fetched once per episode, off the main thread). */
    val chapters: StateFlow<List<Chapter>> = currentEpisode
        .distinctUntilChangedBy { it?.id }
        .flatMapLatest { episode ->
            if (episode == null || episode.chaptersUrl.isNullOrBlank()) flowOf(emptyList())
            else flow { emit(repository.chaptersFor(episode)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepRemainingMs: StateFlow<Long> = sleepTimer.remainingMs
    val sleepEndOfEpisode: StateFlow<Boolean> = sleepTimer.endOfEpisodeArmed

    fun playPause() = connection.playPause()
    fun seekTo(ms: Long) = connection.seekTo(ms)
    fun seekBack() = connection.seekBack()
    fun seekForward() = connection.seekForward()
    fun setSpeed(speed: Float) = connection.setSpeed(speed)

    fun startSleepTimer(minutes: Int) = sleepTimer.start(minutes * 60_000L)
    fun startSleepAtEpisodeEnd() = sleepTimer.startEndOfEpisode()
    fun cancelSleepTimer() = sleepTimer.cancel()
}
