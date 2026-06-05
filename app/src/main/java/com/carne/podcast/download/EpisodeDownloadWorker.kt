package com.carne.podcast.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.carne.podcast.data.local.DownloadState
import com.carne.podcast.data.repository.PodcastRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

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

        repository.updateDownload(episodeId, DownloadState.DOWNLOADING, 0, null)

        val dir = File(applicationContext.filesDir, "downloads").apply { mkdirs() }
        val target = File(dir, sanitize(episodeId) + ".audio")

        return try {
            val request = Request.Builder().url(episode.audioUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    repository.updateDownload(episodeId, DownloadState.FAILED, 0, null)
                    return Result.failure()
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
        } catch (t: Throwable) {
            target.delete()
            repository.updateDownload(episodeId, DownloadState.FAILED, 0, null)
            Result.failure()
        }
    }

    private fun sanitize(id: String): String =
        id.hashCode().toString().replace("-", "n")

    companion object {
        const val KEY_EPISODE_ID = "episode_id"
    }
}
