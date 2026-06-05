package com.carne.podcast.data.repository

import com.carne.podcast.data.local.DownloadState
import com.carne.podcast.data.local.EpisodeDao
import com.carne.podcast.data.local.EpisodeEntity
import com.carne.podcast.data.local.PodcastDao
import com.carne.podcast.data.local.PodcastEntity
import com.carne.podcast.data.remote.ParsedFeed
import com.carne.podcast.data.remote.PodcastSearchResult
import com.carne.podcast.data.remote.PodcastSearchService
import com.carne.podcast.data.remote.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepository @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val rssParser: RssParser,
    private val searchService: PodcastSearchService,
) {
    fun observeSubscriptions(): Flow<List<PodcastEntity>> = podcastDao.observeSubscribed()
    fun observePodcast(feedUrl: String): Flow<PodcastEntity?> = podcastDao.observePodcast(feedUrl)
    fun observeEpisodes(feedUrl: String): Flow<List<EpisodeEntity>> = episodeDao.observeForFeed(feedUrl)
    fun observeEpisode(id: String): Flow<EpisodeEntity?> = episodeDao.observeEpisode(id)
    fun observeLatest(): Flow<List<EpisodeEntity>> = episodeDao.observeLatest()
    fun observeInProgress(): Flow<List<EpisodeEntity>> = episodeDao.observeInProgress()
    fun observeDownloaded(): Flow<List<EpisodeEntity>> = episodeDao.observeDownloaded()

    suspend fun getEpisode(id: String): EpisodeEntity? = episodeDao.getEpisode(id)

    // --- one-shot snapshots, used to build the Android Auto browse tree ---
    suspend fun getSubscriptionsOnce(): List<PodcastEntity> = observeSubscriptions().first()
    suspend fun getEpisodesOnce(feedUrl: String): List<EpisodeEntity> = observeEpisodes(feedUrl).first()
    suspend fun getInProgressOnce(): List<EpisodeEntity> = observeInProgress().first()
    suspend fun getDownloadedOnce(): List<EpisodeEntity> = observeDownloaded().first()

    /** Subscribe to a feed by URL, fetching its content. Returns the feed URL. */
    suspend fun subscribe(feedUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            refreshFeed(feedUrl, markSubscribed = true)
            feedUrl
        }
    }

    suspend fun unsubscribe(feedUrl: String) = withContext(Dispatchers.IO) {
        podcastDao.setSubscribed(feedUrl, false)
    }

    /**
     * (Re)fetch a feed and reconcile podcast + episodes into the database.
     * Returns the episodes that were genuinely new this refresh (so callers can
     * surface "new episode" notifications); known episodes keep their state.
     */
    suspend fun refreshFeed(feedUrl: String, markSubscribed: Boolean = false): List<EpisodeEntity> =
        withContext(Dispatchers.IO) {
            val parsed: ParsedFeed = rssParser.fetchAndParse(feedUrl)
            val existing = podcastDao.getPodcast(feedUrl)
            podcastDao.upsert(
                PodcastEntity(
                    feedUrl = feedUrl,
                    title = parsed.title,
                    author = parsed.author,
                    description = parsed.description,
                    imageUrl = parsed.imageUrl.ifEmpty { existing?.imageUrl.orEmpty() },
                    link = parsed.link,
                    subscribed = markSubscribed || (existing?.subscribed ?: true),
                    lastUpdated = System.currentTimeMillis(),
                )
            )
            val fallbackImage = parsed.imageUrl
            val rows = parsed.episodes.map { e ->
                EpisodeEntity(
                    id = e.guid,
                    feedUrl = feedUrl,
                    title = e.title,
                    description = e.description,
                    audioUrl = e.audioUrl,
                    imageUrl = e.imageUrl.ifEmpty { fallbackImage },
                    pubDate = e.pubDate,
                    durationMs = e.durationMs,
                )
            }
            // INSERT IGNORE keeps existing playback state for known episodes; a
            // rowId of -1 marks a row that already existed and was skipped.
            val rowIds = episodeDao.insertNew(rows)
            rows.filterIndexed { index, _ -> rowIds.getOrElse(index) { -1L } != -1L }
        }

    suspend fun refreshAllSubscriptions(feedUrls: List<String>) = withContext(Dispatchers.IO) {
        feedUrls.forEach { url -> runCatching { refreshFeed(url) } }
    }

    /**
     * Refresh every subscribed feed and return the newly published episodes,
     * skipping a podcast's first-ever fetch so an initial subscribe doesn't
     * spam a notification for the whole back catalogue.
     */
    suspend fun refreshSubscriptionsForNew(): List<EpisodeEntity> = withContext(Dispatchers.IO) {
        val subscriptions = getSubscriptionsOnce()
        subscriptions.flatMap { podcast ->
            val firstFetch = podcast.lastUpdated == 0L
            runCatching { refreshFeed(podcast.feedUrl) }
                .getOrDefault(emptyList())
                .takeUnless { firstFetch }
                ?: emptyList()
        }
    }

    suspend fun search(term: String): List<PodcastSearchResult> = withContext(Dispatchers.IO) {
        runCatching { searchService.search(term) }.getOrDefault(emptyList())
    }

    /** Top shows for a theme (iTunes genre id), to propose on the Discover screen. */
    suspend fun topPodcasts(genreId: Int, limit: Int = 3): List<PodcastSearchResult> =
        withContext(Dispatchers.IO) {
            runCatching { searchService.topPodcasts(genreId, limit) }.getOrDefault(emptyList())
        }

    // --- playback / state mutations ---

    suspend fun savePosition(episodeId: String, positionMs: Long, durationMs: Long) =
        episodeDao.updatePosition(episodeId, positionMs, durationMs)

    suspend fun setPlayed(episodeId: String, played: Boolean) =
        episodeDao.setPlayed(episodeId, played)

    suspend fun updateDownload(
        episodeId: String,
        state: DownloadState,
        progress: Int,
        path: String?,
    ) = episodeDao.updateDownload(episodeId, state, progress, path)
}
