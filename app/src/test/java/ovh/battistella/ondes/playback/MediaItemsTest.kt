package ovh.battistella.ondes.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards [MediaItems.resolveAudioSource], the single security-sensitive rule that
 * decides what an episode actually plays. A regression here either breaks offline
 * playback (ignoring a downloaded file) or, worse, lets a tampered feed point
 * playback at a non-http URL — so the precedence and the http(s)-only guard are
 * pinned by these tests.
 */
class MediaItemsTest {

    @Test
    fun existingLocalFileWins() {
        val file = File.createTempFile("ondes-episode", ".mp3").apply { deleteOnExit() }

        val source = MediaItems.resolveAudioSource(file.path, "https://cdn.example.com/ep.mp3")

        assertTrue(source is AudioSource.Local)
        assertEquals(file, (source as AudioSource.Local).file)
    }

    @Test
    fun fallsBackToRemoteWhenLocalFileMissing() {
        val missing = File.createTempFile("ondes-missing", ".mp3").apply { delete() }

        val source = MediaItems.resolveAudioSource(missing.path, "https://cdn.example.com/ep.mp3")

        assertEquals(AudioSource.Remote("https://cdn.example.com/ep.mp3"), source)
    }

    @Test
    fun usesRemoteWhenNoLocalPath() {
        val source = MediaItems.resolveAudioSource(null, "http://cdn.example.com/ep.mp3")

        assertEquals(AudioSource.Remote("http://cdn.example.com/ep.mp3"), source)
    }

    @Test
    fun rejectsNonHttpRemoteUrls() {
        // A hostile feed could try to redirect playback at a local file or
        // content provider; only http(s) is allowed through.
        assertNull(MediaItems.resolveAudioSource(null, "file:///data/data/secret"))
        assertNull(MediaItems.resolveAudioSource(null, "content://media/external/audio"))
        assertNull(MediaItems.resolveAudioSource(null, ""))
    }

    @Test
    fun missingLocalFileAndUnsafeRemoteYieldsNothing() {
        val missing = File.createTempFile("ondes-missing", ".mp3").apply { delete() }

        assertNull(MediaItems.resolveAudioSource(missing.path, "file:///etc/passwd"))
    }
}
