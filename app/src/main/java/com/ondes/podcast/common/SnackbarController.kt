package com.ondes.podcast.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide channel for transient snackbars, including reversible "undo" actions.
 * Any ViewModel or manager can [show] a message; the root scaffold collects
 * [messages] and renders them on a single shared [androidx.compose.material3.SnackbarHost].
 */
@Singleton
class SnackbarController @Inject constructor() {

    data class Message(
        val text: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
    )

    // A small buffer so emissions from non-suspending callers (tryEmit) aren't dropped
    // if a couple land back-to-back before the collector resumes.
    private val _messages = MutableSharedFlow<Message>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    fun show(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        _messages.tryEmit(Message(text, actionLabel, onAction))
    }
}
