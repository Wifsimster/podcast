package ovh.battistella.ondes.ui.screens.queue

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ovh.battistella.ondes.common.SnackbarController
import ovh.battistella.ondes.data.local.OndesDatabase
import ovh.battistella.ondes.data.local.QueueItemEntity
import ovh.battistella.ondes.data.repository.PodcastRepository
import ovh.battistella.ondes.playback.PlaybackConnection
import ovh.battistella.ondes.playback.PlayerUiState
import ovh.battistella.ondes.testing.TestSupport
import ovh.battistella.ondes.ui.theme.OndesTheme

@RunWith(RobolectricTestRunner::class)
class QueueScreenTest {

    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val snackbar = SnackbarController()
    private lateinit var db: OndesDatabase
    private lateinit var repo: PodcastRepository
    private lateinit var connection: PlaybackConnection

    private fun build(): QueueViewModel {
        db = TestSupport.inMemoryDb()
        repo = TestSupport.repository(db, Dispatchers.Unconfined)
        connection = TestSupport.mockConnection(MutableStateFlow(PlayerUiState()))
        return QueueViewModel(context, repo, connection, snackbar)
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private fun setScreen(vm: QueueViewModel) = compose.setContent {
        OndesTheme(dynamicColor = false) {
            QueueScreen(onOpenPlayer = {}, onBrowse = {}, contentPadding = PaddingValues(0.dp), viewModel = vm)
        }
    }

    @Test
    fun queuedEpisodesAreListed() {
        val vm = build()
        runBlocking {
            db.episodeDao().upsertAll(listOf(TestSupport.episode(id = "q1", title = "Queued Ep")))
            db.queueDao().upsert(QueueItemEntity("q1", 0))
        }
        setScreen(vm)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Queued Ep").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Queued Ep").assertIsDisplayed()
    }
}
