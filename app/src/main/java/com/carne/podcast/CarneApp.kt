package com.carne.podcast

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.carne.podcast.data.local.PodcastDao
import com.carne.podcast.data.local.PodcastEntity
import com.carne.podcast.data.repository.PodcastRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CarneApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var podcastDao: PodcastDao
    @Inject lateinit var repository: PodcastRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        bootstrapDefaultSubscription()
    }

    /**
     * Seed the user's favorite — Silicon Carne — on first launch so the app is
     * useful immediately, then fetch its episodes.
     */
    private fun bootstrapDefaultSubscription() {
        appScope.launch {
            val existing = podcastDao.getPodcast(SILICON_CARNE_FEED)
            if (existing == null) {
                podcastDao.upsert(
                    PodcastEntity(
                        feedUrl = SILICON_CARNE_FEED,
                        title = "Silicon Carne",
                        author = "Carlos Diaz",
                        description = "Un peu de piquant dans un monde de Tech 🌶️",
                        imageUrl = "",
                        link = "https://siliconcarne.substack.com/",
                        subscribed = true,
                        lastUpdated = 0L,
                    )
                )
            }
            runCatching { repository.refreshFeed(SILICON_CARNE_FEED, markSubscribed = true) }
        }
    }

    companion object {
        const val SILICON_CARNE_FEED = "https://feed.ausha.co/vVW80F6lQwAm"
    }
}
