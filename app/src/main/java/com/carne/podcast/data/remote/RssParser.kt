package com.carne.podcast.data.remote

import android.util.Xml
import com.carne.podcast.util.httpUrlOrEmpty
import com.carne.podcast.util.isHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streaming RSS / Atom-ish parser for podcast feeds, built on the platform
 * [XmlPullParser] (no third-party dependency). Tolerant of the messy real
 * world: missing fields fall back to sensible defaults.
 */
@Singleton
class RssParser @Inject constructor(
    private val client: OkHttpClient,
) {
    fun fetchAndParse(feedUrl: String): ParsedFeed {
        val request = Request.Builder()
            .url(feedUrl)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code} fetching $feedUrl")
            val body = response.body ?: error("Empty body for $feedUrl")
            return parse(body.byteStream())
        }
    }

    fun parse(input: InputStream): ParsedFeed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var channelTitle = ""
        var channelAuthor = ""
        var channelDesc = ""
        var channelImage = ""
        var channelLink = ""
        val episodes = mutableListOf<ParsedEpisode>()

        var event = parser.eventType
        var insideItem = false
        var insideImage = false

        // Per-item accumulators
        var iTitle = ""
        var iDesc = ""
        var iGuid = ""
        var iAudio = ""
        var iImage = ""
        var iPub = 0L
        var iDuration = 0L
        var iChapters = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.lowercase(Locale.US)
                    when {
                        name == "item" || name == "entry" -> {
                            insideItem = true
                            iTitle = ""; iDesc = ""; iGuid = ""
                            iAudio = ""; iImage = ""; iPub = 0L; iDuration = 0L
                            iChapters = ""
                        }
                        name == "image" && !insideItem -> insideImage = true
                        insideItem -> readItemTag(parser, name).let { r ->
                            if (r != null) when (r.first) {
                                "title" -> iTitle = r.second
                                "description" -> if (iDesc.isEmpty()) iDesc = r.second
                                "content:encoded" -> iDesc = r.second
                                "guid" -> iGuid = r.second
                                "pubdate" -> iPub = parseDate(r.second)
                                "duration" -> iDuration = parseDuration(r.second)
                                "audio" -> iAudio = r.second
                                "image" -> iImage = r.second
                                "chaptersurl" -> iChapters = r.second
                            }
                        }
                        insideImage -> {
                            if (name == "url") channelImage = parser.nextText().trim()
                        }
                        else -> when (name) {
                            "title" -> channelTitle = parser.nextText().trim()
                            "description", "subtitle" ->
                                if (channelDesc.isEmpty()) channelDesc = parser.nextText().trim()
                            "author" -> channelAuthor = parser.nextText().trim()
                            "itunes:author" -> if (channelAuthor.isEmpty())
                                channelAuthor = parser.nextText().trim()
                            "link" -> if (channelLink.isEmpty())
                                channelLink = parser.nextText().trim()
                            "itunes:image" -> if (channelImage.isEmpty())
                                channelImage = parser.getAttributeValue(null, "href")?.trim().orEmpty()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name.lowercase(Locale.US)
                    when (name) {
                        "item", "entry" -> {
                            // Only http(s) enclosures are playable; a feed pointing
                            // the audio at a local file:// / content:// URI is dropped.
                            if (isHttpUrl(iAudio)) {
                                episodes += ParsedEpisode(
                                    guid = iGuid.ifEmpty { iAudio },
                                    title = iTitle.ifEmpty { "Untitled" },
                                    description = iDesc,
                                    audioUrl = iAudio.trim(),
                                    imageUrl = httpUrlOrEmpty(iImage),
                                    pubDate = iPub,
                                    durationMs = iDuration,
                                    chaptersUrl = httpUrlOrEmpty(iChapters),
                                )
                            }
                            insideItem = false
                        }
                        "image" -> insideImage = false
                    }
                }
            }
            event = parser.next()
        }

        return ParsedFeed(
            title = channelTitle.ifEmpty { "Podcast" },
            author = channelAuthor,
            description = channelDesc,
            imageUrl = httpUrlOrEmpty(channelImage),
            link = channelLink,
            episodes = episodes,
        )
    }

    /** Returns a (key,value) for recognized item-level tags, or null. */
    private fun readItemTag(parser: XmlPullParser, name: String): Pair<String, String>? = when (name) {
        "title" -> "title" to parser.nextText().trim()
        "description", "summary", "itunes:summary" -> "description" to parser.nextText().trim()
        "content:encoded" -> "content:encoded" to parser.nextText().trim()
        "guid", "id" -> "guid" to parser.nextText().trim()
        "pubdate", "published", "updated" -> "pubdate" to parser.nextText().trim()
        "itunes:duration" -> "duration" to parser.nextText().trim()
        "enclosure" -> {
            val url = parser.getAttributeValue(null, "url")?.trim().orEmpty()
            if (url.isNotEmpty()) "audio" to url else null
        }
        "itunes:image" -> {
            val href = parser.getAttributeValue(null, "href")?.trim().orEmpty()
            if (href.isNotEmpty()) "image" to href else null
        }
        "podcast:chapters" -> {
            val url = parser.getAttributeValue(null, "url")?.trim().orEmpty()
            if (url.isNotEmpty()) "chaptersurl" to url else null
        }
        else -> null
    }

    private fun parseDuration(raw: String): Long {
        val t = raw.trim()
        if (t.isEmpty()) return 0L
        // Either seconds ("3600") or HH:MM:SS / MM:SS
        return if (t.contains(":")) {
            val parts = t.split(":").mapNotNull { it.trim().toLongOrNull() }
            when (parts.size) {
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
                2 -> (parts[0] * 60 + parts[1]) * 1000
                else -> 0L
            }
        } else {
            (t.toDoubleOrNull()?.toLong() ?: 0L) * 1000
        }
    }

    private fun parseDate(raw: String): Long {
        val t = raw.trim()
        if (t.isEmpty()) return 0L
        for (fmt in DATE_FORMATS) {
            runCatching {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                return sdf.parse(t)?.time ?: 0L
            }
        }
        return 0L
    }

    companion object {
        private const val USER_AGENT = "Ondes/1.0 (Android podcast app)"
        private val DATE_FORMATS = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm Z",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
        )
    }
}
