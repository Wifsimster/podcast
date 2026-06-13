package ovh.battistella.ondes.playback

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import ovh.battistella.ondes.data.local.EpisodeEntity
import ovh.battistella.ondes.util.httpUrlOrEmpty
import ovh.battistella.ondes.util.isHttpUrl
import java.io.File

/**
 * The audio source chosen for an episode: an already-downloaded local file, or a
 * remote http(s) stream. Kept as a small sealed type so the decision rule below
 * stays pure (and unit-testable) — the [Uri] conversion happens separately, in
 * Android land.
 */
sealed interface AudioSource {
    data class Local(val file: File) : AudioSource
    data class Remote(val url: String) : AudioSource
}

/**
 * Single source of truth for turning an [EpisodeEntity] into a Media3
 * [MediaItem]. Both the in-app [PlaybackConnection] and the Android-Auto browse
 * tree ([MediaLibraryTree]) build items here, so the security-sensitive URI
 * rules live in exactly one place and can never drift apart.
 *
 * Audio source precedence: an existing local download wins; otherwise only a
 * remote `http(s)` source is allowed; everything else yields nothing. Artwork
 * URIs handed to SystemUI / Android Auto are likewise restricted to http(s) so a
 * tampered feed can't make a privileged client dereference a `file://` /
 * `content://` URI (see the threat model in `util/WebUrl.kt`).
 */
object MediaItems {

    /**
     * Pure decision rule for an episode's playable source. An existing local file
     * takes precedence (offline playback); failing that, only a safe http(s)
     * stream is accepted; a missing file or a non-http URL yields `null`.
     */
    fun resolveAudioSource(localFilePath: String?, audioUrl: String): AudioSource? {
        localFilePath?.let { path ->
            File(path).takeIf(File::exists)?.let { return AudioSource.Local(it) }
        }
        return audioUrl.takeIf { isHttpUrl(it) }?.let { AudioSource.Remote(it) }
    }

    /** The resolved playback [Uri] for an episode, or null if it has no safe source. */
    fun audioUri(episode: EpisodeEntity): Uri? =
        when (val source = resolveAudioSource(episode.localFilePath, episode.audioUrl)) {
            is AudioSource.Local -> Uri.fromFile(source.file)
            is AudioSource.Remote -> source.url.toUri()
            null -> null
        }

    /**
     * A fully playable episode item with its resolved audio [Uri], or null when
     * the episode has neither a downloaded file nor a safe http(s) source — such
     * an item must never reach the player.
     */
    fun playable(episode: EpisodeEntity): MediaItem? {
        val uri = audioUri(episode) ?: return null
        return MediaItem.Builder()
            .setMediaId(episode.id)
            .setUri(uri)
            .setMediaMetadata(episodeMetadata(episode))
            .build()
    }

    /**
     * A browsable episode item with no resolved URI — for Android Auto / Wear
     * listings, where the audio source is resolved later in `onAddMediaItems`.
     */
    fun browsable(episode: EpisodeEntity): MediaItem =
        MediaItem.Builder()
            .setMediaId(episode.id)
            .setMediaMetadata(episodeMetadata(episode))
            .build()

    private fun episodeMetadata(episode: EpisodeEntity): MediaMetadata =
        MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtworkUri(httpUrlOrEmpty(episode.imageUrl).takeIf { it.isNotBlank() }?.toUri())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .build()
}
