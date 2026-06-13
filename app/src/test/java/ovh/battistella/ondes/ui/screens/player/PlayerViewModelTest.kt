package ovh.battistella.ondes.ui.screens.player

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
import ovh.battistella.ondes.testing.MainDispatcherRule
import ovh.battistella.ondes.testing.TestSupport

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlayerViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val playerFlow = MutableStateFlow(PlayerUiState())
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private lateinit var db: OndesDatabase
    private lateinit var repo: PodcastRepository
    private lateinit var connection: PlaybackConnection
    private lateinit var sleepTimer: SleepTimer

    private fun build(settings: OndesSettings = OndesSettings()): PlayerViewModel {
        db = TestSupport.inMemoryDb()
        repo = TestSupport.repository(db, mainDispatcher.dispatcher)
        connection = TestSupport.mockConnection(playerFlow)
        sleepTimer = SleepTimer(connection)
        every { settingsRepository.settings } returns flowOf(settings)
        return PlayerViewModel(connection, sleepTimer, repo, settingsRepository)
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun `skip intervals are surfaced in seconds from settings`() = runTest(mainDispatcher.dispatcher) {
        val vm = build(OndesSettings(skipBackMs = 15_000, skipForwardMs = 45_000))
        backgroundScope.launch { vm.skipBackSeconds.collect {} }
        backgroundScope.launch { vm.skipForwardSeconds.collect {} }
        advanceUntilIdle()

        assertEquals(15, vm.skipBackSeconds.value)
        assertEquals(45, vm.skipForwardSeconds.value)
    }

    @Test
    fun `currentEpisode resolves the loaded media id to its entity`() = runTest(mainDispatcher.dispatcher) {
        val vm = build()
        db.episodeDao().upsertAll(listOf(TestSupport.episode(id = "ep-1", title = "Now Playing")))
        backgroundScope.launch { vm.currentEpisode.collect {} }

        playerFlow.value = PlayerUiState(currentEpisodeId = "ep-1")
        advanceUntilIdle()

        assertEquals("Now Playing", vm.currentEpisode.value?.title)
    }

    @Test
    fun `transport controls delegate to the connection`() {
        val vm = build()
        vm.playPause(); verify { connection.playPause() }
        vm.seekForward(); verify { connection.seekForward() }
        vm.seekBack(); verify { connection.seekBack() }
        vm.next(); verify { connection.next() }
        vm.previous(); verify { connection.previous() }
        vm.stop(); verify { connection.stop() }
    }
}
