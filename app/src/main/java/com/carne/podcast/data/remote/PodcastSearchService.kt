package com.carne.podcast.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Podcast discovery via the public iTunes Search API. No key required, returns
 * a usable RSS [feedUrl] for each match so we can subscribe directly.
 */
@Singleton
class PodcastSearchService @Inject constructor(
    private val client: OkHttpClient,
) {
    fun search(term: String): List<PodcastSearchResult> {
        if (term.isBlank()) return emptyList()
        val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("media", "podcast")
            .addQueryParameter("entity", "podcast")
            .addQueryParameter("limit", "30")
            .addQueryParameter("term", term)
            .build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
            val out = ArrayList<PodcastSearchResult>(results.length())
            for (i in 0 until results.length()) {
                val o = results.optJSONObject(i) ?: continue
                val feed = o.optString("feedUrl")
                if (feed.isNullOrBlank()) continue
                out += PodcastSearchResult(
                    feedUrl = feed,
                    title = o.optString("collectionName"),
                    author = o.optString("artistName"),
                    imageUrl = o.optString("artworkUrl600")
                        .ifEmpty { o.optString("artworkUrl100") },
                )
            }
            return out
        }
    }
}
