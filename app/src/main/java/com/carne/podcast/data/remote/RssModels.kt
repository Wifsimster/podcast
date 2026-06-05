package com.carne.podcast.data.remote

data class ParsedFeed(
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String,
    val link: String,
    val episodes: List<ParsedEpisode>,
)

data class ParsedEpisode(
    val guid: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val pubDate: Long,
    val durationMs: Long,
)

/** A podcast returned by the search service (iTunes). */
data class PodcastSearchResult(
    val feedUrl: String,
    val title: String,
    val author: String,
    val imageUrl: String,
)
