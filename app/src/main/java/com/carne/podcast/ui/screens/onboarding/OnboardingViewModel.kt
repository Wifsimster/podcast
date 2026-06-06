package com.carne.podcast.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carne.podcast.data.remote.PodcastTheme
import com.carne.podcast.data.remote.PodcastThemes
import com.carne.podcast.data.repository.PodcastRepository
import com.carne.podcast.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * First-run interest picker: the user taps a few themes and we subscribe them to
 * the top shows for each, so the app is useful immediately instead of empty.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: PodcastRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val themes: List<PodcastTheme> = PodcastThemes.all

    private val _selected = MutableStateFlow<Set<Int>>(emptySet())
    val selected = _selected.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    fun toggle(theme: PodcastTheme) {
        _selected.value = _selected.value.toMutableSet().apply {
            if (!add(theme.genreId)) remove(theme.genreId)
        }
    }

    /** Subscribe to the top shows for the chosen themes, then leave onboarding. */
    fun finish() {
        viewModelScope.launch {
            _busy.value = true
            themes.filter { it.genreId in _selected.value }.forEach { theme ->
                repository.topPodcasts(theme.genreId, limit = 3).forEach { result ->
                    runCatching { repository.subscribe(result.feedUrl) }
                }
            }
            settingsRepository.setOnboardingDone(true)
            _busy.value = false
        }
    }

    fun skip() {
        viewModelScope.launch { settingsRepository.setOnboardingDone(true) }
    }
}
