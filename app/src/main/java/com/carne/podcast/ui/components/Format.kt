package com.carne.podcast.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** "1:02:03" or "12:34". */
fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = TimeUnit.SECONDS.toHours(totalSec)
    val m = TimeUnit.SECONDS.toMinutes(totalSec) % 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

/** Compact duration label, e.g. "1h 02m" or "34 min". */
fun formatDurationLabel(ms: Long): String {
    if (ms <= 0) return ""
    val totalMin = ms / 60000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${String.format(Locale.US, "%02d", m)}m" else "$m min"
}

fun formatDate(epochMs: Long): String {
    if (epochMs <= 0) return ""
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMs))
}

/** Strip HTML tags from feed descriptions for plain-text display. */
fun stripHtml(html: String): String =
    html.replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&#39;", "'")
        .replace("&quot;", "\"")
        .replace(Regex("\\s+"), " ")
        .trim()
