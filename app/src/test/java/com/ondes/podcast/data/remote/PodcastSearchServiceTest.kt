package com.ondes.podcast.data.remote

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards [PodcastSearchService.distinctByFeedUrl], which keeps the result lists
 * feed-unique. The Search/Discover lists key Compose rows by feedUrl, and a
 * duplicate key crashes the LazyColumn — reproduced by searching "the room" on
 * the French iTunes storefront, where one show is returned under two collection
 * ids sharing a single feed.
 */
class PodcastSearchServiceTest {

    private val service = PodcastSearchService(OkHttpClient())

    private fun result(feed: String, title: String = feed) =
        PodcastSearchResult(feedUrl = feed, title = title, author = "", imageUrl = "")

    @Test
    fun dropsDuplicateFeedsKeepingFirstAndOrder() {
        val input = listOf(
            result("https://feeds.simplecast.com/aPz9Qkq6", "The Room A"),
            result("https://example.com/other"),
            result("https://feeds.simplecast.com/aPz9Qkq6", "The Room B"),
        )

        val out = service.distinctByFeedUrl(input)

        assertEquals(
            listOf("https://feeds.simplecast.com/aPz9Qkq6", "https://example.com/other"),
            out.map { it.feedUrl },
        )
        // The first occurrence wins, so ranking/relevance order is preserved.
        assertEquals("The Room A", out.first().title)
    }

    @Test
    fun leavesAlreadyUniqueListUntouched() {
        val input = listOf(result("https://a.example/feed"), result("https://b.example/feed"))
        assertEquals(input, service.distinctByFeedUrl(input))
    }
}
