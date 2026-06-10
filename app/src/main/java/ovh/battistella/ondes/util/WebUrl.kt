package ovh.battistella.ondes.util

/**
 * Scheme allowlist for URLs that originate from untrusted feed content (RSS
 * enclosures, artwork, chapters), OPML imports and restored backups.
 *
 * These URLs are eventually handed to ExoPlayer, Coil and the system media
 * session's artwork loader — all of which also resolve `file://`, `content://`,
 * `asset://` and `data://`. A hostile feed could therefore point them at local
 * files or content providers (artwork URIs are dereferenced by SystemUI /
 * Android Auto with elevated privileges). We only ever want remote http(s), so
 * everything else is rejected at the boundary.
 */

/** True only for a non-blank, absolute `http`/`https` URL. */
fun isHttpUrl(url: String?): Boolean {
    val u = url?.trim()?.lowercase() ?: return false
    return u.startsWith("http://") || u.startsWith("https://")
}

/** The trimmed URL when it is http(s), otherwise an empty string. */
fun httpUrlOrEmpty(url: String?): String =
    url?.trim()?.takeIf { isHttpUrl(it) } ?: ""
