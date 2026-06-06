package com.carne.podcast.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.carne.podcast.R

/**
 * The one top app bar every primary screen wears, so titles, the status-bar
 * scrim and the actions on the trailing edge stay identical from tab to tab.
 *
 * It also centralises two recurring affordances: an optional leading **back**
 * arrow (for pushed screens like Settings) and an optional trailing **settings**
 * gear — the single, always-in-the-same-spot way into preferences now that
 * Settings has left the bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarneTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        actions = {
            actions()
            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
