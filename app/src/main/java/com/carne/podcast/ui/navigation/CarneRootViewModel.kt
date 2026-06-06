package com.carne.podcast.ui.navigation

import androidx.lifecycle.ViewModel
import com.carne.podcast.common.SnackbarController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Exposes the singleton [SnackbarController] to the root composable. */
@HiltViewModel
class CarneRootViewModel @Inject constructor(
    val snackbar: SnackbarController,
) : ViewModel()
