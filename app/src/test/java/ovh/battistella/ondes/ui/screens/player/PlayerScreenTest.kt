package ovh.battistella.ondes.ui.screens.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ovh.battistella.ondes.data.local.OndesDatabase
import ovh.battistella.ondes.data.repository.PodcastRepository
import ovh.battistella.ondes.data.settings.OndesSettings
import ovh.battistella.ondes.data.settings.SettingsRepository
import ovh.battistella.ondes.playback.PlaybackConnection
import ovh.battistella.ondes.playback.PlayerUiState
import ovh.battistella.ondes.playback.SleepTimer
import ovh.battistella.ondes.testing.TestSupport
import ovh.battistella.ondes.ui.theme.OndesTheme

@RunWith(RobolectricTestRunner::class)
class PlayerScreenTest {

    @get:Rule val compose = createComposeRule()

    private lateinit var db: OndesDatabase
    private lateinit var repo: PodcastRepository
    private lateinit var connection: PlaybackConnection

    private fun build(): PlayerViewModel {
        db = TestSupport.inMemoryDb()
        repo = TestSupport.repository(db, Dispatchers.Unconfined)
        val playerFlow = MutableStateFlow(
            PlayerUiState(currentEpisodeId = "ep-1", title = "Now Playing Ep", durationMs = 60_000),
        )
        connection = TestSupport.mockConnection(playerFlow)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.settings } returns flowOf(OndesSettings())
        return PlayerViewModel(connection, SleepTimer(connection), repo, settingsRepository)
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun nowPlayingTitleRenders() {
        val vm = build()
        runBlocking { db.episodeDao().upsertAll(listOf(TestSupport.episode(id = "ep-1", title = "Now Playing Ep"))) }
        compose.setContent {
            OndesTheme(dynamicColor = false) {
                PlayerScreen(onClose = {}, viewModel = vm)
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Now Playing Ep").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Now Playing Ep").assertIsDisplayed()
    }
}
