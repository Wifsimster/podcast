package com.carne.podcast.data.remote

/**
 * A browsable podcast theme, backed by an iTunes genre id so we can pull the
 * current top shows for it. Used by Discover to let people explore by topic
 * instead of having to know a show's name up front.
 */
data class PodcastTheme(
    /** iTunes podcast genre id (see https://podcasts.apple.com genre charts). */
    val genreId: Int,
    val label: String,
)

/** Curated shortlist of popular podcast genres surfaced on the Discover screen. */
object PodcastThemes {
    val all: List<PodcastTheme> = listOf(
        PodcastTheme(1318, "Technology"),
        PodcastTheme(1321, "Business"),
        PodcastTheme(1489, "News"),
        PodcastTheme(1488, "True Crime"),
        PodcastTheme(1303, "Comedy"),
        PodcastTheme(1533, "Science"),
        PodcastTheme(1512, "Health"),
        PodcastTheme(1487, "History"),
        PodcastTheme(1545, "Sports"),
        PodcastTheme(1304, "Education"),
        PodcastTheme(1310, "Music"),
        PodcastTheme(1324, "Society"),
        PodcastTheme(1301, "Arts"),
        PodcastTheme(1309, "TV & Film"),
    )
}
