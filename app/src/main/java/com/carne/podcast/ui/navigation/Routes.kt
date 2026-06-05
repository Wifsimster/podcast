package com.carne.podcast.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val DOWNLOADS = "downloads"
    const val PLAYER = "player"
    const val PODCAST = "podcast/{feedUrl}"

    fun podcast(feedUrl: String) = "podcast/${Uri.encode(feedUrl)}"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "Home", Icons.Rounded.Home),
    LIBRARY(Routes.LIBRARY, "Library", Icons.Rounded.LibraryMusic),
    SEARCH(Routes.SEARCH, "Search", Icons.Rounded.Search),
    DOWNLOADS(Routes.DOWNLOADS, "Downloads", Icons.Rounded.Download),
}
