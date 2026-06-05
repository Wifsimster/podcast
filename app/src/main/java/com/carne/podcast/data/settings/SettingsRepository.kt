package com.carne.podcast.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Which color scheme to render, independent of the system setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** A snapshot of every user-configurable preference in the app. */
data class CarneSettings(
    val skipBackMs: Long = 10_000L,
    val skipForwardMs: Long = 30_000L,
    val defaultSpeed: Float = 1f,
    val autoAdvance: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val autoDeleteFinished: Boolean = false,
    val backgroundRefresh: Boolean = true,
    val newEpisodeNotifications: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "carne_settings")

/**
 * Single source of truth for user preferences, backed by Preferences DataStore.
 * Exposes the whole bundle as a [Flow] so any layer (UI, playback, downloads,
 * sync) can react to changes, plus suspend setters for the settings screen.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val SKIP_BACK = longPreferencesKey("skip_back_ms")
        val SKIP_FORWARD = longPreferencesKey("skip_forward_ms")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val AUTO_ADVANCE = booleanPreferencesKey("auto_advance")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only_downloads")
        val AUTO_DELETE = booleanPreferencesKey("auto_delete_finished")
        val BACKGROUND_REFRESH = booleanPreferencesKey("background_refresh")
        val NEW_EPISODE_NOTIFS = booleanPreferencesKey("new_episode_notifications")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val settings: Flow<CarneSettings> = context.dataStore.data.map { p ->
        val defaults = CarneSettings()
        CarneSettings(
            skipBackMs = p[Keys.SKIP_BACK] ?: defaults.skipBackMs,
            skipForwardMs = p[Keys.SKIP_FORWARD] ?: defaults.skipForwardMs,
            defaultSpeed = p[Keys.DEFAULT_SPEED] ?: defaults.defaultSpeed,
            autoAdvance = p[Keys.AUTO_ADVANCE] ?: defaults.autoAdvance,
            wifiOnlyDownloads = p[Keys.WIFI_ONLY] ?: defaults.wifiOnlyDownloads,
            autoDeleteFinished = p[Keys.AUTO_DELETE] ?: defaults.autoDeleteFinished,
            backgroundRefresh = p[Keys.BACKGROUND_REFRESH] ?: defaults.backgroundRefresh,
            newEpisodeNotifications = p[Keys.NEW_EPISODE_NOTIFS] ?: defaults.newEpisodeNotifications,
            themeMode = p[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: defaults.themeMode,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
        )
    }

    suspend fun setSkipBackMs(value: Long) = edit { it[Keys.SKIP_BACK] = value }
    suspend fun setSkipForwardMs(value: Long) = edit { it[Keys.SKIP_FORWARD] = value }
    suspend fun setDefaultSpeed(value: Float) = edit { it[Keys.DEFAULT_SPEED] = value }
    suspend fun setAutoAdvance(value: Boolean) = edit { it[Keys.AUTO_ADVANCE] = value }
    suspend fun setWifiOnlyDownloads(value: Boolean) = edit { it[Keys.WIFI_ONLY] = value }
    suspend fun setAutoDeleteFinished(value: Boolean) = edit { it[Keys.AUTO_DELETE] = value }
    suspend fun setBackgroundRefresh(value: Boolean) = edit { it[Keys.BACKGROUND_REFRESH] = value }
    suspend fun setNewEpisodeNotifications(value: Boolean) =
        edit { it[Keys.NEW_EPISODE_NOTIFS] = value }
    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.THEME_MODE] = value.name }
    suspend fun setDynamicColor(value: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = value }

    private suspend fun edit(
        block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit,
    ) {
        context.dataStore.edit(block)
    }
}
