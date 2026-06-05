package com.carne.podcast.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.carne.podcast.data.local.EpisodeEntity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerUiState(
    val isConnected: Boolean = false,
    val currentEpisodeId: String? = null,
    val title: String = "",
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
)

/**
 * App-wide bridge to [PlaybackService]. Connects a [MediaController] and
 * exposes a single [PlayerUiState] flow that any screen (mini-player, now
 * playing) can observe.
 */
@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PlayerUiState())
    val state = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    init {
        connect()
        startPositionTicker()
    }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            controller = future.get()
            controller?.addListener(playerListener)
            _state.value = _state.value.copy(isConnected = true)
            syncFromController()
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromController()
        }
    }

    private fun startPositionTicker() {
        scope.launch {
            while (true) {
                val c = controller
                if (c != null && c.isPlaying) {
                    _state.value = _state.value.copy(
                        positionMs = c.currentPosition.coerceAtLeast(0),
                        durationMs = c.duration.let { if (it == C.TIME_UNSET) 0L else it },
                    )
                }
                delay(500)
            }
        }
    }

    private fun syncFromController() {
        val c = controller ?: return
        val item = c.currentMediaItem
        _state.value = _state.value.copy(
            isConnected = true,
            currentEpisodeId = item?.mediaId?.takeIf { it.isNotBlank() },
            title = item?.mediaMetadata?.title?.toString() ?: "",
            artworkUri = item?.mediaMetadata?.artworkUri?.toString(),
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.let { if (it == C.TIME_UNSET) 0L else it },
            speed = c.playbackParameters.speed,
        )
    }

    /** Play an episode from the start position stored in the database. */
    fun play(episode: EpisodeEntity) {
        val c = controller ?: return
        // If this episode is already loaded, just resume.
        if (c.currentMediaItem?.mediaId == episode.id) {
            c.play()
            return
        }
        val uri: Uri = episode.localFilePath
            ?.let { path -> File(path).takeIf { it.exists() }?.let { Uri.fromFile(it) } }
            ?: episode.audioUrl.toUri()

        val metadata = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtworkUri(episode.imageUrl.takeIf { it.isNotBlank() }?.toUri())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()

        c.setMediaItem(mediaItem, /* startPositionMs = */ episode.positionMs.coerceAtLeast(0))
        c.prepare()
        c.play()
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun pause() { controller?.pause() }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun seekBack() { controller?.seekBack() }

    fun seekForward() { controller?.seekForward() }

    fun setSpeed(speed: Float) {
        controller?.playbackParameters = PlaybackParameters(speed)
    }

    fun stop() {
        controller?.run {
            pause()
            clearMediaItems()
        }
    }
}
