package ovh.battistella.ondes.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import ovh.battistella.ondes.R

object Routes {
    const val HOME = "home"
    const val QUEUE = "queue"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val PODCAST = "podcast/{feedUrl}"

    fun podcast(feedUrl: String) = "podcast/${Uri.encode(feedUrl)}"
}

/**
 * The bottom-navigation destinations. Material 3 specs a navigation bar for 3–5
 * destinations: Downloads lives inside Library (a filtered view) and Settings is
 * reached from the Home top-bar gear, keeping this to a focused four.
 */
enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Rounded.Home),
    LIBRARY(Routes.LIBRARY, R.string.nav_library, Icons.Rounded.LibraryMusic),
    SEARCH(Routes.SEARCH, R.string.nav_search, Icons.Rounded.Search),
    QUEUE(Routes.QUEUE, R.string.nav_queue, Icons.Rounded.QueueMusic),
}
