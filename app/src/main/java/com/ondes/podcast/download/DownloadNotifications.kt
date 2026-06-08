package com.ondes.podcast.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.ondes.podcast.R

/**
 * The low-importance notification channel backing the foreground download
 * worker. A foreground/expedited [EpisodeDownloadWorker] must post an ongoing
 * notification while it streams, so the OS is far less likely to kill it
 * mid-download (which previously left rows stuck at DOWNLOADING).
 */
object DownloadNotifications {

    const val CHANNEL_ID = "downloads"
    const val NOTIFICATION_ID = 2001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.nav_downloads),
            NotificationManager.IMPORTANCE_LOW,
        )
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }
}
