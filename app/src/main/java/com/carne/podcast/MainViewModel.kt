package com.carne.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carne.podcast.data.settings.CarneSettings
import com.carne.podcast.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<CarneSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, CarneSettings())

    /** null while the preference is still loading, so we don't flash onboarding. */
    val showOnboarding: StateFlow<Boolean?> = settingsRepository.settings
        .map { !it.onboardingDone }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
