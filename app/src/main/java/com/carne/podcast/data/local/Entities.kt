package com.carne.podcast.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadState { NONE, QUEUED, DOWNLOADING, DOWNLOADED, FAILED }

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val feedUrl: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String,
    val link: String,
    val subscribed: Boolean = true,
    val lastUpdated: Long = 0L,
    /** Per-podcast playback speed override; null = use the global default. */
    val overrideSpeed: Float? = null,
    /** Auto-download newly published episodes for this subscription. */
    @ColumnInfo(defaultValue = "0") val autoDownload: Boolean = false,
)

@Entity(
    tableName = "episodes",
    indices = [Index("feedUrl"), Index("pubDate")],
)
data class EpisodeEntity(
    @PrimaryKey val id: String,            // guid (or audioUrl fallback)
    val feedUrl: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val pubDate: Long,                     // epoch millis
    val durationMs: Long,                  // 0 if unknown
    val positionMs: Long = 0L,             // resume position
    val isPlayed: Boolean = false,
    val isFinished: Boolean = false,
    val downloadState: DownloadState = DownloadState.NONE,
    val localFilePath: String? = null,
    val downloadProgress: Int = 0,         // 0..100
    val chaptersUrl: String? = null,       // Podcasting 2.0 chapters JSON URL
)

/** A single chapter marker within an episode. */
data class Chapter(
    val startMs: Long,
    val title: String,
    val imageUrl: String? = null,
)

/** Episode joined with its parent podcast, for list rows. */
data class EpisodeWithPodcast(
    val episode: EpisodeEntity,
    val podcastTitle: String,
    val podcastImageUrl: String,
)

/**
 * One entry in the user-curated "Up Next" queue. [sortIndex] orders the queue
 * (smaller plays first); gaps are left between entries so "Play next" can splice
 * an item in front without renumbering the whole list.
 */
@Entity(tableName = "queue")
data class QueueItemEntity(
    @PrimaryKey val episodeId: String,
    val sortIndex: Long,
)
