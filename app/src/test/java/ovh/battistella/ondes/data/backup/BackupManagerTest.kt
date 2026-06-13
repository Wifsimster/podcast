package ovh.battistella.ondes.data.backup

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ovh.battistella.ondes.data.local.OndesDatabase
import ovh.battistella.ondes.data.local.QueueItemEntity
import ovh.battistella.ondes.data.settings.OndesSettings
import ovh.battistella.ondes.data.settings.SettingsRepository
import ovh.battistella.ondes.testing.TestSupport
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Round-trips a backup: export from one database, import into a fresh one, and
 * confirm subscriptions, listening progress and queue survive the migration —
 * while a non-http (tampered) feed is rejected on import.
 */
@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {

    private val settings = mockk<SettingsRepository>(relaxed = true)
    private val dbs = mutableListOf<OndesDatabase>()

    private fun newManager(): Pair<OndesDatabase, BackupManager> {
        val db = TestSupport.inMemoryDb().also(dbs::add)
        coEvery { settings.snapshot() } returns OndesSettings()
        return db to BackupManager(db.podcastDao(), db.episodeDao(), db.queueDao(), settings)
    }

    @After fun tearDown() {
        dbs.forEach { it.close() }
    }

    @Test
    fun `subscriptions, progress and queue survive an export-import round trip`() = runBlocking {
        val (source, exporter) = newManager()
        source.podcastDao().upsert(TestSupport.podcast(feedUrl = "https://a.example/feed", title = "Show A"))
        source.episodeDao().upsertAll(
            listOf(TestSupport.episode(id = "e1", feedUrl = "https://a.example/feed", positionMs = 12_345)),
        )
        source.queueDao().upsert(QueueItemEntity("e1", 0))

        val bytes = ByteArrayOutputStream().also { exporter.export(it) }.toByteArray()

        val (restored, importer) = newManager()
        importer.import(ByteArrayInputStream(bytes))

        assertEquals("Show A", restored.podcastDao().getPodcast("https://a.example/feed")?.title)
        assertEquals(12_345L, restored.episodeDao().getEpisode("e1")?.positionMs)
        assertEquals(listOf("e1"), restored.queueDao().getOrderedIds())
    }

    @Test
    fun `import rejects a tampered non-http feed`() = runBlocking {
        val (target, importer) = newManager()
        val doc = """
            { "version": 1,
              "podcasts": [ { "feedUrl": "file:///etc/passwd", "title": "Evil" } ],
              "episodes": [], "queue": [] }
        """.trimIndent()

        importer.import(ByteArrayInputStream(doc.toByteArray()))

        assertNull(target.podcastDao().getPodcast("file:///etc/passwd"))
    }
}
