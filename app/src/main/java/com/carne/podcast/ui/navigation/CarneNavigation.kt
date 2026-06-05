package com.carne.podcast.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carne.podcast.ui.components.MiniPlayer
import com.carne.podcast.ui.screens.downloads.DownloadsScreen
import com.carne.podcast.ui.screens.home.HomeScreen
import com.carne.podcast.ui.screens.library.LibraryScreen
import com.carne.podcast.ui.screens.player.PlayerScreen
import com.carne.podcast.ui.screens.player.PlayerViewModel
import com.carne.podcast.ui.screens.podcast.PodcastScreen
import com.carne.podcast.ui.screens.search.SearchScreen
import com.carne.podcast.ui.screens.settings.SettingsScreen

@Composable
fun CarneRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isPlayer = currentRoute == Routes.PLAYER
    val isTopLevel = TopLevelDestination.entries.any { it.route == currentRoute }

    // A single PlayerViewModel scoped to the root drives the mini-player.
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = !isPlayer) {
                Column {
                    MiniPlayer(
                        state = playerState,
                        onPlayPause = playerViewModel::playPause,
                        onForward = playerViewModel::seekForward,
                        onClick = { navController.navigate(Routes.PLAYER) },
                    )
                    if (isTopLevel) {
                        BottomBar(navController, currentRoute)
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                    contentPadding = innerPadding,
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onOpenPodcast = { navController.navigate(Routes.podcast(it)) },
                    contentPadding = innerPadding,
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(contentPadding = innerPadding)
            }
            composable(Routes.DOWNLOADS) {
                DownloadsScreen(
                    onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                    contentPadding = innerPadding,
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(contentPadding = innerPadding)
            }
            composable(Routes.PODCAST) {
                PodcastScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                )
            }
            composable(Routes.PLAYER) {
                PlayerScreen(onClose = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomBar(
    navController: androidx.navigation.NavHostController,
    currentRoute: String?,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
            )
        }
    }
}
