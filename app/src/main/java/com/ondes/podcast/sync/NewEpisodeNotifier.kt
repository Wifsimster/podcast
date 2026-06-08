package com.ondes.podcast.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ondes.podcast.MainActivity
import com.ondes.podcast.R
import com.ondes.podcast.data.repository.NewEpisodeBatch

/**
 * Builds and posts "new episodes available" notifications — one per podcast,
 * bundled under a group summary, each deep-linking to the show it belongs to.
 */
object NewEpisodeNotifier {

    const val CHANNEL_ID = "new_episodes"

    /** Intent extra carrying the feed URL to open when a notification is tapped. */
    const val EXTRA_OPEN_FEED_URL = "com.ondes.podcast.extra.OPEN_FEED_URL"

    private const val GROUP_KEY = "com.ondes.podcast.NEW_EPISODES"
    private const val SUMMARY_ID = 1001
    private const val MAX_LINES = 5

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun notify(context: Context, batches: List<NewEpisodeBatch>) {
        val withEpisodes = batches.filter { it.episodes.isNotEmpty() }
        if (withEpisodes.isEmpty()) return
        if (!hasPermission(context)) return

        val manager = NotificationManagerCompat.from(context)
        withEpisodes.forEach { batch ->
            manager.notify(notificationId(batch.feedUrl), buildPodcastNotification(context, batch))
        }
        // A group summary so the system bundles the per-podcast notifications.
        manager.notify(SUMMARY_ID, buildSummary(context, withEpisodes))
    }

    private fun buildPodcastNotification(context: Context, batch: NewEpisodeBatch): Notification {
        val episodes = batch.episodes
        val title = batch.podcastTitle.ifBlank { context.getString(R.string.notif_channel_name) }
        val text = if (episodes.size == 1) {
            episodes.first().title
        } else {
            context.resources.getQuantityString(
                R.plurals.new_episodes_count, episodes.size, episodes.size,
            )
        }
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        episodes.take(MAX_LINES).forEach { style.addLine(it.title) }
        if (episodes.size > MAX_LINES) {
            style.setSummaryText(context.getString(R.string.notif_more, episodes.size - MAX_LINES))
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(style)
            .setContentIntent(contentIntent(context, batch.feedUrl))
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .build()
    }

    private fun buildSummary(context: Context, batches: List<NewEpisodeBatch>): Notification {
        val total = batches.sumOf { it.episodes.size }
        val title = context.resources.getQuantityString(
            R.plurals.new_episodes_count, total, total,
        )
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        batches.forEach { batch ->
            val detail = if (batch.episodes.size == 1) {
                batch.episodes.first().title
            } else {
                context.resources.getQuantityString(
                    R.plurals.new_episodes_count, batch.episodes.size, batch.episodes.size,
                )
            }
            style.addLine(
                context.getString(R.string.notif_podcast_line, batch.podcastTitle, detail)
            )
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setStyle(style)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setContentIntent(contentIntent(context, feedUrl = null))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .build()
    }

    private fun contentIntent(context: Context, feedUrl: String?): PendingIntent {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (feedUrl != null) putExtra(EXTRA_OPEN_FEED_URL, feedUrl)
        }
        // Distinct request code per podcast so each PendingIntent keeps its own extras.
        val requestCode = feedUrl?.hashCode() ?: 0
        return PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** A stable id per feed, kept clear of the group-summary id. */
    private fun notificationId(feedUrl: String): Int {
        val hash = feedUrl.hashCode()
        return if (hash == SUMMARY_ID) SUMMARY_ID + 1 else hash
    }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
