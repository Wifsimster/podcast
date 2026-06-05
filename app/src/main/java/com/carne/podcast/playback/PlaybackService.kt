package com.carne.podcast.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.carne.podcast.data.repository.PodcastRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground media service that owns the ExoPlayer + MediaSession. Provides
 * background playback, lock-screen / notification transport controls, and
 * persistence of the resume position back into the database.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var repository: PodcastRepository

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionSaver: Job? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(SEEK_BACK_MS)
            .setSeekForwardIncrementMs(SEEK_FORWARD_MS)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) startPositionSaver() else {
                    persistPosition()
                    positionSaver?.cancel()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Persist the outgoing item's position before moving on.
                persistPosition()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) markCurrentFinished()
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Keep playing in the background if media is active; otherwise stop.
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        persistPosition()
        positionSaver?.cancel()
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun startPositionSaver() {
        positionSaver?.cancel()
        positionSaver = scope.launch {
            while (isActive) {
                delay(POSITION_SAVE_INTERVAL_MS)
                persistPosition()
            }
        }
    }

    private fun persistPosition() {
        val item = player.currentMediaItem ?: return
        val id = item.mediaId
        if (id.isBlank()) return
        val position = player.currentPosition.coerceAtLeast(0)
        val duration = player.duration.let { if (it == C.TIME_UNSET) 0L else it }
        scope.launch(Dispatchers.IO) {
            repository.savePosition(id, position, duration)
        }
    }

    private fun markCurrentFinished() {
        val id = player.currentMediaItem?.mediaId ?: return
        if (id.isBlank()) return
        scope.launch(Dispatchers.IO) { repository.setPlayed(id, true) }
    }

    companion object {
        const val SEEK_BACK_MS = 10_000L
        const val SEEK_FORWARD_MS = 30_000L
        private const val POSITION_SAVE_INTERVAL_MS = 5_000L
    }
}
