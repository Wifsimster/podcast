package com.carne.podcast.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Upsert
    suspend fun upsert(podcast: PodcastEntity)

    @Query("SELECT * FROM podcasts WHERE subscribed = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeSubscribed(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl")
    fun observePodcast(feedUrl: String): Flow<PodcastEntity?>

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl")
    suspend fun getPodcast(feedUrl: String): PodcastEntity?

    @Query("UPDATE podcasts SET subscribed = :subscribed WHERE feedUrl = :feedUrl")
    suspend fun setSubscribed(feedUrl: String, subscribed: Boolean)

    @Query("DELETE FROM podcasts WHERE feedUrl = :feedUrl")
    suspend fun delete(feedUrl: String)
}

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNew(episodes: List<EpisodeEntity>): List<Long>

    @Update
    suspend fun update(episode: EpisodeEntity)

    @Query("SELECT * FROM episodes WHERE feedUrl = :feedUrl ORDER BY pubDate DESC")
    fun observeForFeed(feedUrl: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    fun observeEpisode(id: String): Flow<EpisodeEntity?>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getEpisode(id: String): EpisodeEntity?

    @Query("UPDATE episodes SET positionMs = :positionMs, durationMs = CASE WHEN :durationMs > 0 THEN :durationMs ELSE durationMs END WHERE id = :id")
    suspend fun updatePosition(id: String, positionMs: Long, durationMs: Long)

    @Query("UPDATE episodes SET isPlayed = :played, isFinished = :played, positionMs = CASE WHEN :played THEN 0 ELSE positionMs END WHERE id = :id")
    suspend fun setPlayed(id: String, played: Boolean)

    @Query("UPDATE episodes SET downloadState = :state, downloadProgress = :progress, localFilePath = :path WHERE id = :id")
    suspend fun updateDownload(id: String, state: DownloadState, progress: Int, path: String?)

    /** Continue listening: started-but-not-finished, most recently touched first. */
    @Query(
        """
        SELECT * FROM episodes
        WHERE positionMs > 0 AND isFinished = 0
        ORDER BY pubDate DESC LIMIT 20
        """
    )
    fun observeInProgress(): Flow<List<EpisodeEntity>>

    /** Newest episodes across all subscriptions, for the home feed. */
    @Query(
        """
        SELECT * FROM episodes
        WHERE feedUrl IN (SELECT feedUrl FROM podcasts WHERE subscribed = 1)
        ORDER BY pubDate DESC LIMIT 50
        """
    )
    fun observeLatest(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE downloadState = 'DOWNLOADED' ORDER BY pubDate DESC")
    fun observeDownloaded(): Flow<List<EpisodeEntity>>
}
