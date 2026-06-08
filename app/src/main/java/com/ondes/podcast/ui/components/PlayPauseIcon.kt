package com.ondes.podcast.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The app's most-tapped control. Cross-fades + scales between the play and pause
 * glyphs so the tap has a satisfying pop — picking up the spring specs from the
 * [androidx.compose.material3.MaterialExpressiveTheme] already in place — instead
 * of swapping instantly.
 *
 * The icon itself carries no [Icon.contentDescription]; the enclosing toggle
 * button is expected to own the name + `stateDescription` ("Playing"/"Paused")
 * so TalkBack announces a single, stateful control.
 */
@Composable
fun PlayPauseIcon(
    isPlaying: Boolean,
    playIcon: ImageVector,
    pauseIcon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut()
        },
        label = "play-pause",
    ) { playing ->
        Icon(
            imageVector = if (playing) pauseIcon else playIcon,
            contentDescription = null,
            tint = tint,
            modifier = modifier,
        )
    }
}
