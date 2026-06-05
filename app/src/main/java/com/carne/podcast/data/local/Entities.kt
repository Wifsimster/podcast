package com.carne.podcast.data.local

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
)

/** Episode joined with its parent podcast, for list rows. */
data class EpisodeWithPodcast(
    val episode: EpisodeEntity,
    val podcastTitle: String,
    val podcastImageUrl: String,
)
