package com.carne.podcast.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carne.podcast.data.settings.CarneSettings
import com.carne.podcast.data.settings.SettingsRepository
import com.carne.podcast.data.settings.ThemeMode
import com.carne.podcast.sync.FeedRefreshScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<CarneSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CarneSettings())

    fun setSkipBack(ms: Long) = update { settingsRepository.setSkipBackMs(ms) }
    fun setSkipForward(ms: Long) = update { settingsRepository.setSkipForwardMs(ms) }
    fun setAutoAdvance(value: Boolean) = update { settingsRepository.setAutoAdvance(value) }
    fun setWifiOnlyDownloads(value: Boolean) =
        update { settingsRepository.setWifiOnlyDownloads(value) }
    fun setAutoDeleteFinished(value: Boolean) =
        update { settingsRepository.setAutoDeleteFinished(value) }
    fun setNewEpisodeNotifications(value: Boolean) =
        update { settingsRepository.setNewEpisodeNotifications(value) }
    fun setThemeMode(value: ThemeMode) = update { settingsRepository.setThemeMode(value) }
    fun setDynamicColor(value: Boolean) = update { settingsRepository.setDynamicColor(value) }

    fun setBackgroundRefresh(value: Boolean) = update {
        settingsRepository.setBackgroundRefresh(value)
        FeedRefreshScheduler.apply(context, value)
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
