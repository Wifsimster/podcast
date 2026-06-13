package ovh.battistella.ondes.ui.screens.downloads

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
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
import ovh.battistella.ondes.testing.TestSupport
import ovh.battistella.ondes.ui.theme.OndesTheme

@RunWith(RobolectricTestRunner::class)
class DownloadsScreenTest {

    @get:Rule val compose = createComposeRule()

    private val downloadManager = mockk<DownloadManager>(relaxed = true)
    private lateinit var db: OndesDatabase
    private lateinit var repo: PodcastRepository
    private lateinit var connection: PlaybackConnection

    private fun build(): DownloadsViewModel {
        db = TestSupport.inMemoryDb()
        repo = TestSupport.repository(db, Dispatchers.Unconfined)
        connection = TestSupport.mockConnection(MutableStateFlow(PlayerUiState()))
        return DownloadsViewModel(repo, connection, downloadManager)
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private fun setScreen(vm: DownloadsViewModel) = compose.setContent {
        OndesTheme(dynamicColor = false) {
            DownloadsScreen(onOpenPlayer = {}, contentPadding = PaddingValues(0.dp), viewModel = vm)
        }
    }

    @Test
    fun downloadedEpisodesAreListed() {
        val vm = build()
        runBlocking {
            db.episodeDao().upsertAll(
                listOf(
                    TestSupport.episode(id = "d1", title = "Downloaded Ep", downloadState = DownloadState.DOWNLOADED),
                ),
            )
        }
        setScreen(vm)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Downloaded Ep").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Downloaded Ep").assertIsDisplayed()
    }
}
