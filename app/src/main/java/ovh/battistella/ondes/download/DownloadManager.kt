package ovh.battistella.ondes.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import ovh.battistella.ondes.R
import ovh.battistella.ondes.common.SnackbarController
import ovh.battistella.ondes.data.local.DownloadState
import ovh.battistella.ondes.data.repository.PodcastRepository
import ovh.battistella.ondes.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PodcastRepository,
    private val snackbar: SnackbarController,
    settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var wifiOnly: Boolean = false

    init {
        scope.launch { settingsRepository.settings.collect { wifiOnly = it.wifiOnlyDownloads } }
    }

    fun enqueue(episodeId: String) {
        scope.launch { repository.updateDownload(episodeId, DownloadState.QUEUED, 0, null) }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
            .setInputData(workDataOf(EpisodeDownloadWorker.KEY_EPISODE_ID to episodeId))
            .setConstraints(constraints)
            // Expedited so a user-triggered download starts promptly and runs as a
            // foreground worker (less likely to be OS-killed mid-stream); falls back
            // to a normal request when the app is out of expedited quota.
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(episodeId), ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(episodeId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(episodeId))
        scope.launch { repository.updateDownload(episodeId, DownloadState.NONE, 0, null) }
    }

    /**
     * Delete the local file for an episode. [showUndo] offers a re-download via a
     * snackbar for user-initiated deletes; automatic delete-when-finished passes
     * false so it stays silent.
     */
    fun deleteDownload(episodeId: String, localPath: String?, showUndo: Boolean = true) {
        localPath?.let { runCatching { File(it).delete() } }
        scope.launch { repository.updateDownload(episodeId, DownloadState.NONE, 0, null) }
        if (showUndo) {
            // Deleting only removes the local file; "Undo" simply re-downloads it.
            snackbar.show(
                text = context.getString(R.string.download_deleted),
                actionLabel = context.getString(R.string.undo),
                onAction = { enqueue(episodeId) },
            )
        }
    }

    private fun workName(episodeId: String) = "download_$episodeId"
}
