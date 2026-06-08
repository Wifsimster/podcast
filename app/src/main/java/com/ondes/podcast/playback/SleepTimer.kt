package com.ondes.podcast.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Counts down and pauses playback when it reaches zero. */
@Singleton
class SleepTimer @Inject constructor(
    private val playbackConnection: PlaybackConnection,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private val _remainingMs = MutableStateFlow(0L)
    /** Milliseconds left, or 0 when inactive. */
    val remainingMs = _remainingMs.asStateFlow()

    private val _endOfEpisodeArmed = MutableStateFlow(false)
    /** True when the timer is set to stop at the end of the current episode. */
    val endOfEpisodeArmed = _endOfEpisodeArmed.asStateFlow()

    fun start(durationMs: Long) {
        cancel()
        _remainingMs.value = durationMs
        job = scope.launch {
            while (isActive && _remainingMs.value > 0) {
                delay(1_000)
                _remainingMs.value = (_remainingMs.value - 1_000).coerceAtLeast(0)
            }
            if (_remainingMs.value <= 0) {
                playbackConnection.pause()
            }
        }
    }

    /** Pause playback when the episode that is current right now finishes. */
    fun startEndOfEpisode() {
        cancel()
        val targetId = playbackConnection.state.value.currentEpisodeId ?: return
        _endOfEpisodeArmed.value = true
        job = scope.launch {
            playbackConnection.state.collect { state ->
                val current = state.currentEpisodeId
                val nearEnd = current == targetId &&
                    state.durationMs > 0 &&
                    state.positionMs >= state.durationMs - END_THRESHOLD_MS
                // Either the episode played out / auto-advanced, or it is about to.
                if (current != targetId || nearEnd) {
                    playbackConnection.pause()
                    cancel()
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = 0
        _endOfEpisodeArmed.value = false
    }

    private companion object {
        const val END_THRESHOLD_MS = 1_500L
    }
}
