package com.carne.podcast.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.carne.podcast.R
import com.carne.podcast.data.local.DownloadState
import com.carne.podcast.data.repository.PodcastRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@HiltWorker
class EpisodeDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: PodcastRepository,
    private val client: OkHttpClient,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val episodeId = inputData.getString(KEY_EPISODE_ID) ?: return Result.failure()
        val episode = repository.getEpisode(episodeId) ?: return Result.failure()

        // Run as a foreground/expedited worker so the OS is unlikely to kill us
        // mid-stream and leave the row stuck at DOWNLOADING. Promotion can be
        // refused (e.g. background-start limits); fall back to a normal worker.
        runCatching { setForeground(foregroundInfo()) }

        repository.updateDownload(episodeId, DownloadState.DOWNLOADING, 0, null)

        val dir = File(applicationContext.filesDir, "downloads").apply { mkdirs() }
        val target = File(dir, sanitize(episodeId) + ".audio")

        return try {
            val request = Request.Builder().url(episode.audioUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // 5xx / 429 are transient — let WorkManager back off and retry.
                    return if (response.isTransient() && runAttemptCount < MAX_ATTEMPTS) {
                        repository.updateDownload(episodeId, DownloadState.QUEUED, 0, null)
                        Result.retry()
                    } else {
                        repository.updateDownload(episodeId, DownloadState.FAILED, 0, null)
                        Result.failure()
                    }
                }
                val body = response.body ?: run {
                    repository.updateDownload(episodeId, DownloadState.FAILED, 0, null)
                    return Result.failure()
                }
                val total = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var lastReported = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                val pct = ((downloaded * 100) / total).toInt()
                                if (pct != lastReported && pct % 5 == 0) {
                                    lastReported = pct
                                    repository.updateDownload(
                                        episodeId, DownloadState.DOWNLOADING, pct, null
                                    )
                                }
                            }
                        }
                    }
                }
            }
            repository.updateDownload(
                episodeId, DownloadState.DOWNLOADED, 100, target.absolutePath
            )
            Result.success()
        } catch (io: IOException) {
            // A dropped connection is transient: retry with backoff instead of
            // failing permanently, until we exhaust the attempt budget.
            target.delete()
            if (runAttemptCount < MAX_ATTEMPTS) {
                repository.updateDownload(episodeId, DownloadState.QUEUED, 0, null)
                Result.retry()
            } else {
                repository.updateDownload(episodeId, DownloadState.FAILED, 0, null)
                Result.failure()
            }
        } catch (t: Throwable) {
            target.delete()
            repository.updateDownload(episodeId, DownloadState.FAILED, 0, null)
            Result.failure()
        }
    }

    private fun foregroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(
            applicationContext, DownloadNotifications.CHANNEL_ID,
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.downloading))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                DownloadNotifications.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(DownloadNotifications.NOTIFICATION_ID, notification)
        }
    }

    private fun okhttp3.Response.isTransient(): Boolean = code == 429 || code in 500..599

    private fun sanitize(id: String): String =
        id.hashCode().toString().replace("-", "n")

    companion object {
        const val KEY_EPISODE_ID = "episode_id"

        /** Total tries (initial + retries) before a download is marked FAILED. */
        private const val MAX_ATTEMPTS = 3
    }
}
