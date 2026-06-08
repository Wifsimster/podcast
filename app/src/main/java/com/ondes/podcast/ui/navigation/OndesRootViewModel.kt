package com.ondes.podcast.ui.navigation

import androidx.lifecycle.ViewModel
import com.ondes.podcast.common.SnackbarController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Exposes the singleton [SnackbarController] to the root composable. */
@HiltViewModel
class OndesRootViewModel @Inject constructor(
    val snackbar: SnackbarController,
) : ViewModel()
