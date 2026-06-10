package ovh.battistella.ondes.data.remote

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
    /** Podcasting 2.0 `<podcast:chapters>` JSON URL, if the feed provides one. */
    val chaptersUrl: String = "",
)

/** A podcast returned by the search service (iTunes). */
data class PodcastSearchResult(
    val feedUrl: String,
    val title: String,
    val author: String,
    val imageUrl: String,
)
