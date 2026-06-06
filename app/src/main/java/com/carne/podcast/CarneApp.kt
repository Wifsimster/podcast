package com.carne.podcast

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.carne.podcast.data.settings.SettingsRepository
import com.carne.podcast.sync.FeedRefreshScheduler
import com.carne.podcast.sync.NewEpisodeNotifier
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CarneApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NewEpisodeNotifier.createChannel(this)
        scheduleBackgroundRefresh()
    }

    /** Honour the saved background-refresh preference on every launch. */
    private fun scheduleBackgroundRefresh() {
        appScope.launch {
            val settings = settingsRepository.settings.first()
            FeedRefreshScheduler.apply(this@CarneApp, settings.backgroundRefresh)
        }
    }
}
