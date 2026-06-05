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
import com.carne.podcast.data.settings.CarneSettings
import com.carne.podcast.data.settings.SettingsRepository
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
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PlayerUiState())
    val state = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** Latest settings snapshot, kept current so playback honours live changes. */
    @Volatile private var settings: CarneSettings = CarneSettings()

    init {
        scope.launch { settingsRepository.settings.collect { settings = it } }
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

    /**
     * Play [episode], resuming from its saved position. When auto-advance is on
     * and a [queue] is supplied, the episodes after it are loaded too so
     * playback flows continuously into the next one.
     */
    fun play(episode: EpisodeEntity, queue: List<EpisodeEntity> = emptyList()) {
        val c = controller ?: return
        // If this episode is already loaded, just resume.
        if (c.currentMediaItem?.mediaId == episode.id) {
            c.play()
            return
        }

        val items = if (settings.autoAdvance && queue.isNotEmpty()) {
            val tail = queue.dropWhile { it.id != episode.id }
            (if (tail.isEmpty()) listOf(episode) else tail).map(::mediaItemFor)
        } else {
            listOf(mediaItemFor(episode))
        }

        c.setMediaItems(items, /* startIndex = */ 0, episode.positionMs.coerceAtLeast(0))
        c.playbackParameters = PlaybackParameters(settings.defaultSpeed)
        c.prepare()
        c.play()
    }

    private fun mediaItemFor(episode: EpisodeEntity): MediaItem {
        val uri: Uri = episode.localFilePath
            ?.let { path -> File(path).takeIf { it.exists() }?.let { Uri.fromFile(it) } }
            ?: episode.audioUrl.toUri()

        val metadata = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtworkUri(episode.imageUrl.takeIf { it.isNotBlank() }?.toUri())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId(episode.id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun pause() { controller?.pause() }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    /** Skip backward by the user-configured interval. */
    fun seekBack() {
        val c = controller ?: return
        c.seekTo((c.currentPosition - settings.skipBackMs).coerceAtLeast(0))
    }

    /** Skip forward by the user-configured interval. */
    fun seekForward() {
        val c = controller ?: return
        val target = c.currentPosition + settings.skipForwardMs
        val duration = c.duration
        c.seekTo(if (duration > 0) target.coerceAtMost(duration) else target)
    }

    fun setSpeed(speed: Float) {
        controller?.playbackParameters = PlaybackParameters(speed)
        scope.launch { settingsRepository.setDefaultSpeed(speed) }
    }

    fun stop() {
        controller?.run {
            pause()
            clearMediaItems()
        }
    }
}
