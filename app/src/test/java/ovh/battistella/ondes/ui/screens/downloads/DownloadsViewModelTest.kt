package ovh.battistella.ondes.ui.screens.downloads

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ovh.battistella.ondes.data.local.DownloadState
import ovh.battistella.ondes.data.local.OndesDatabase
import ovh.battistella.ondes.data.repository.PodcastRepository
import ovh.battistella.ondes.download.DownloadManager
import ovh.battistella.ondes.playback.PlaybackConnection
import ovh.battistella.ondes.playback.PlayerUiState
import ovh.battistella.ondes.testing.MainDispatcherRule
import ovh.battistella.ondes.testing.TestSupport

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DownloadsViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val playerFlow = MutableStateFlow(PlayerUiState())
    private val downloadManager = mockk<DownloadManager>(relaxed = true)
    private lateinit var db: OndesDatabase
    private lateinit var repo: PodcastRepository
    private lateinit var connection: PlaybackConnection

    private fun build(): DownloadsViewModel {
        db = TestSupport.inMemoryDb()
        repo = TestSupport.repository(db, mainDispatcher.dispatcher)
        connection = TestSupport.mockConnection(playerFlow)
        return DownloadsViewModel(repo, connection, downloadManager)
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun `only downloaded episodes are listed`() = runTest(mainDispatcher.dispatcher) {
        val vm = build()
        db.episodeDao().upsertAll(
            listOf(
                TestSupport.episode(id = "done", downloadState = DownloadState.DOWNLOADED),
                TestSupport.episode(id = "none", downloadState = DownloadState.NONE),
            ),
        )
        backgroundScope.launch { vm.downloads.collect {} }
        advanceUntilIdle()

        assertEquals(listOf("done"), vm.downloads.value.map { it.id })
    }

    @Test
    fun `deleteDownload delegates to the manager`() {
        val vm = build()
        vm.deleteDownload(TestSupport.episode(id = "done", localFilePath = "/tmp/x.mp3"))
        verify { downloadManager.deleteDownload("done", "/tmp/x.mp3") }
    }

    @Test
    fun `addToQueue enqueues a downloaded episode`() = runTest(mainDispatcher.dispatcher) {
        val vm = build()
        db.episodeDao().upsertAll(listOf(TestSupport.episode(id = "done")))
        vm.addToQueue(TestSupport.episode(id = "done"))
        advanceUntilIdle()
        assertEquals(listOf("done"), db.queueDao().getOrderedIds())
    }
}
