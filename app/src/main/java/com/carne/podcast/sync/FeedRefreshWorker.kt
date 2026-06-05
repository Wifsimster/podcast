package com.carne.podcast.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.carne.podcast.data.repository.PodcastRepository
import com.carne.podcast.data.settings.SettingsRepository
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
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        if (!settings.backgroundRefresh) return Result.success()

        return try {
            val newEpisodes = repository.refreshSubscriptionsForNew()
            if (settings.newEpisodeNotifications) {
                NewEpisodeNotifier.notify(appContext, newEpisodes)
            }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}
