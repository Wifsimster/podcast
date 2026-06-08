package com.ondes.podcast.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ondes.podcast.data.repository.PodcastRepository
import com.ondes.podcast.data.settings.SettingsRepository
import com.ondes.podcast.download.DownloadManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Periodically refreshes every subscribed feed in the background and posts a
 * notification for any newly published episodes.
 */
@HiltWorker
class FeedRefreshWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: PodcastRepository,
    private val settingsRepository: SettingsRepository,
    private val downloadManager: DownloadManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.backgroundRefresh) return Result.success()

        return try {
            val newEpisodes = repository.refreshSubscriptionsForNew()
            // Auto-download fresh episodes for subscriptions that opted in
            // (DownloadManager already honours the Wi-Fi-only constraint).
            newEpisodes.forEach { batch ->
                if (repository.getPodcastOnce(batch.feedUrl)?.autoDownload == true) {
                    batch.episodes.forEach { downloadManager.enqueue(it.id) }
                }
            }
            if (settings.newEpisodeNotifications) {
                NewEpisodeNotifier.notify(appContext, batches = newEpisodes)
            }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}
