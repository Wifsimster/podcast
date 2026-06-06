package com.carne.podcast.playback

import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaConstants
import androidx.media3.session.SessionResult
import com.carne.podcast.R
import com.carne.podcast.data.local.EpisodeEntity
import com.carne.podcast.data.repository.PodcastRepository
import com.carne.podcast.data.settings.CarneSettings
import com.carne.podcast.data.settings.SettingsRepository
import com.carne.podcast.download.DownloadManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject

/**
 * Foreground media service that owns the ExoPlayer + MediaSession. Provides
 * background playback, lock-screen / notification transport controls, and
 * persistence of the resume position back into the database.
 *
 * Implemented as a [MediaLibraryService] so the app exposes a browsable
 * content hierarchy (Continue listening / Subscriptions / Downloads) to
 * Android Auto, Wear OS and other `MediaBrowser` clients.
 */
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var repository: PodcastRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var downloadManager: DownloadManager

    private var mediaSession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionSaver: Job? = null

    /** Volume-boost effect, (re)bound to the player's current audio session. */
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var loudnessSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    /** Latest settings snapshot, kept current for auto-delete decisions. */
    @Volatile private var settings: CarneSettings = CarneSettings()

    /** The item currently playing, tracked so we can mark it finished on transition. */
    private var playingMediaId: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: media service starting")

        // Read the persisted settings once so the notification / Android Auto
        // skip buttons honour the user's chosen intervals from launch.
        settings = runBlocking { settingsRepository.settings.first() }
        scope.launch {
            settingsRepository.settings.collect {
                settings = it
                applyAudioEffects()
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(settings.skipBackMs)
            .setSeekForwardIncrementMs(settings.skipForwardMs)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) startPositionSaver() else {
                    persistPosition()
                    positionSaver?.cancel()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // An automatic transition means the previous episode played out
                // to the end — mark it finished before moving on.
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    playingMediaId?.let(::markFinished)
                }
                persistPosition()
                playingMediaId = mediaItem?.mediaId?.takeIf { it.isNotBlank() }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) markCurrentFinished()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                applyAudioEffects()
            }
        })

        applyAudioEffects()
        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback()).build()
    }

    /** Apply the user's skip-silence and volume-boost preferences to the player. */
    private fun applyAudioEffects() {
        if (!::player.isInitialized) return
        player.skipSilenceEnabled = settings.skipSilence

        val sessionId = player.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return
        try {
            if (loudnessEnhancer == null || loudnessSessionId != sessionId) {
                loudnessEnhancer?.release()
                loudnessEnhancer = LoudnessEnhancer(sessionId)
                loudnessSessionId = sessionId
            }
            loudnessEnhancer?.apply {
                setTargetGain(if (settings.boostVolume) BOOST_GAIN_MB else 0)
                setEnabled(settings.boostVolume)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "LoudnessEnhancer unavailable for session $sessionId", t)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Log.i(TAG, "onGetSession: controller='${controllerInfo.packageName}' uid=${controllerInfo.uid} session=${mediaSession != null}")
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Keep playing in the background if media is active; otherwise stop.
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: media service stopping")
        persistPosition()
        positionSaver?.cancel()
        scope.cancel()
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------------
    // Android Auto / MediaBrowser content tree
    // ---------------------------------------------------------------------

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
            Log.i(
                TAG,
                "onGetLibraryRoot: caller='${browser.packageName}' " +
                    "recent=${params?.isRecent} suggested=${params?.isSuggested} " +
                    "offline=${params?.isOffline}",
            )
            try {
                // Android Auto asks for a "recent" root to populate the resume /
                // now-playing card. Hand back a dedicated root whose single child is
                // the most recent in-progress episode so the car can offer "resume".
                val root = if (params?.isRecent == true) recentRootItem() else rootItem()
                LibraryResult.ofItem(root, rootParams())
            } catch (t: Throwable) {
                Log.e(TAG, "onGetLibraryRoot failed", t)
                LibraryResult.ofError(SessionResult.RESULT_ERROR_UNKNOWN)
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
            Log.i(TAG, "onGetItem: mediaId='$mediaId' caller='${browser.packageName}'")
            try {
                val item = when (mediaId) {
                    ROOT_ID -> rootItem()
                    RECENT_ROOT_ID -> recentRootItem()
                    CONTINUE_ID -> folderItem(CONTINUE_ID, getString(R.string.continue_listening))
                    SUBSCRIPTIONS_ID ->
                        folderItem(SUBSCRIPTIONS_ID, getString(R.string.subscriptions_title))
                    DOWNLOADS_ID -> folderItem(DOWNLOADS_ID, getString(R.string.nav_downloads))
                    else -> when {
                        mediaId.startsWith(PODCAST_PREFIX) ->
                            repository.getSubscriptionsOnce()
                                .firstOrNull { PODCAST_PREFIX + it.feedUrl == mediaId }
                                ?.let { podcastItem(it.feedUrl, it.title, it.imageUrl) }
                        else -> repository.getEpisode(mediaId)?.let(::episodeBrowseItem)
                    }
                }
                if (item != null) LibraryResult.ofItem(item, null)
                else LibraryResult.ofError(SessionResult.RESULT_ERROR_BAD_VALUE)
            } catch (t: Throwable) {
                Log.e(TAG, "onGetItem failed for '$mediaId'", t)
                LibraryResult.ofError(SessionResult.RESULT_ERROR_UNKNOWN)
            }
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            Log.i(
                TAG,
                "onGetChildren: parent='$parentId' page=$page pageSize=$pageSize " +
                    "caller='${browser.packageName}'",
            )
            try {
                val children: List<MediaItem> = when (parentId) {
                    ROOT_ID -> listOf(
                        folderItem(CONTINUE_ID, getString(R.string.continue_listening)),
                        folderItem(SUBSCRIPTIONS_ID, getString(R.string.subscriptions_title)),
                        folderItem(DOWNLOADS_ID, getString(R.string.nav_downloads)),
                    )
                    // Resume card: the single most recent in-progress episode, or the
                    // newest episode if nothing is part-way through.
                    RECENT_ROOT_ID -> listOfNotNull(
                        (repository.getInProgressOnce().firstOrNull()
                            ?: repository.getLatestOnce().firstOrNull())
                            ?.let(::episodeBrowseItem),
                    )
                    CONTINUE_ID -> repository.getInProgressOnce().map(::episodeBrowseItem)
                    DOWNLOADS_ID -> repository.getDownloadedOnce().map(::episodeBrowseItem)
                    SUBSCRIPTIONS_ID -> repository.getSubscriptionsOnce()
                        .map { podcastItem(it.feedUrl, it.title, it.imageUrl) }
                    else -> if (parentId.startsWith(PODCAST_PREFIX)) {
                        val feedUrl = parentId.removePrefix(PODCAST_PREFIX)
                        repository.getEpisodesOnce(feedUrl).map(::episodeBrowseItem)
                    } else {
                        emptyList()
                    }
                }
                Log.i(TAG, "onGetChildren: parent='$parentId' -> ${children.size} items")
                LibraryResult.ofItemList(ImmutableList.copyOf(children), null)
            } catch (t: Throwable) {
                Log.e(TAG, "onGetChildren failed for '$parentId'", t)
                LibraryResult.ofItemList(ImmutableList.of<MediaItem>(), null)
            }
        }

        /**
         * Browser clients (Android Auto, notifications) hand us items that
         * carry only a `mediaId`. Resolve each one back to a fully-formed,
         * playable [MediaItem] with its audio URI and metadata.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = scope.future {
            resolve(mediaItems)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
            val resolved = resolve(mediaItems)
            if (resolved.isEmpty()) {
                return@future MediaItemsWithStartPosition(emptyList(), 0, 0L)
            }
            val index = (if (startIndex == C.INDEX_UNSET) 0 else startIndex)
                .coerceIn(0, resolved.lastIndex)
            // If the caller didn't pin a position (e.g. tapping an episode in
            // the car), resume from the saved position in the database.
            val position = if (startPositionMs != C.TIME_UNSET) {
                startPositionMs
            } else {
                repository.getEpisode(resolved[index].mediaId)
                    ?.positionMs?.coerceAtLeast(0) ?: 0L
            }
            MediaItemsWithStartPosition(resolved, index, position)
        }
    }

    private suspend fun resolve(items: List<MediaItem>): List<MediaItem> {
        val resolved = items.mapNotNull { item ->
            // Already complete (has a URI) — keep as-is.
            if (item.localConfiguration != null) return@mapNotNull item
            repository.getEpisode(item.mediaId)?.let(::playableItem)
        }
        Log.i(TAG, "resolve: ${items.size} requested -> ${resolved.size} playable")
        return resolved
    }

    // ---------------------------------------------------------------------
    // MediaItem builders
    // ---------------------------------------------------------------------

    private fun rootItem(): MediaItem = folderItem(ROOT_ID, getString(R.string.app_name))

    /** Root requested by Android Auto for the resume / now-playing card. */
    private fun recentRootItem(): MediaItem = folderItem(RECENT_ROOT_ID, getString(R.string.app_name))

    private fun rootParams(): LibraryParams = LibraryParams.Builder()
        .setExtras(
            Bundle().apply {
                putInt(
                    MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                    MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
                )
                putInt(
                    MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                    MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
                )
            }
        )
        .build()

    private fun folderItem(id: String, title: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)
            .build()
        return MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata).build()
    }

    private fun podcastItem(feedUrl: String, title: String, imageUrl: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtworkUri(imageUrl.takeIf { it.isNotBlank() }?.toUri())
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST)
            .build()
        return MediaItem.Builder()
            .setMediaId(PODCAST_PREFIX + feedUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    /** A playable episode without a resolved URI — for browse results. */
    private fun episodeBrowseItem(episode: EpisodeEntity): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtworkUri(episode.imageUrl.takeIf { it.isNotBlank() }?.toUri())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .build()
        return MediaItem.Builder()
            .setMediaId(episode.id)
            .setMediaMetadata(metadata)
            .build()
    }

    /** A fully playable episode with its local/remote audio URI. */
    private fun playableItem(episode: EpisodeEntity): MediaItem {
        val uri: Uri = episode.localFilePath
            ?.let { path -> File(path).takeIf { it.exists() }?.let { Uri.fromFile(it) } }
            ?: episode.audioUrl.toUri()
        val metadata = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtworkUri(episode.imageUrl.takeIf { it.isNotBlank() }?.toUri())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .build()
        return MediaItem.Builder()
            .setMediaId(episode.id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    // ---------------------------------------------------------------------
    // Position persistence
    // ---------------------------------------------------------------------

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
        markFinished(id)
    }

    /** Mark an episode played and, if the user opted in, delete its download. */
    private fun markFinished(id: String) {
        if (id.isBlank()) return
        scope.launch(Dispatchers.IO) {
            repository.setPlayed(id, true)
            // A finished episode leaves the Up-Next queue so it stays current.
            repository.removeFromQueue(id)
            if (settings.autoDeleteFinished) {
                repository.getEpisode(id)?.let { episode ->
                    downloadManager.deleteDownload(episode.id, episode.localFilePath)
                }
            }
        }
    }

    companion object {
        /** Logcat tag for Android Auto / MediaBrowser diagnostics: `adb logcat -s CarneAuto`. */
        private const val TAG = "CarneAuto"

        private const val POSITION_SAVE_INTERVAL_MS = 5_000L

        /** Volume-boost gain in millibels (~10 dB) when "Boost volume" is on. */
        private const val BOOST_GAIN_MB = 1_000

        // Browse-tree node ids.
        private const val ROOT_ID = "[root]"
        private const val RECENT_ROOT_ID = "[recent]"
        private const val CONTINUE_ID = "[continue]"
        private const val SUBSCRIPTIONS_ID = "[subscriptions]"
        private const val DOWNLOADS_ID = "[downloads]"
        private const val PODCAST_PREFIX = "[podcast]"
    }
}
