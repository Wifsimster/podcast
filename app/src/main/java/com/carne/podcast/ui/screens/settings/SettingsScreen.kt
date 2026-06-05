package com.carne.podcast.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carne.podcast.data.settings.ThemeMode

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        SectionHeader("Playback")
        ChoiceRow(
            title = "Skip back",
            current = secondsLabel(settings.skipBackMs),
            options = SKIP_BACK_OPTIONS.map { secondsLabel(it) },
            onSelect = { viewModel.setSkipBack(SKIP_BACK_OPTIONS[it]) },
        )
        ChoiceRow(
            title = "Skip forward",
            current = secondsLabel(settings.skipForwardMs),
            options = SKIP_FORWARD_OPTIONS.map { secondsLabel(it) },
            onSelect = { viewModel.setSkipForward(SKIP_FORWARD_OPTIONS[it]) },
        )
        SwitchRow(
            title = "Auto-play next episode",
            subtitle = "Continue to the next episode when one finishes",
            checked = settings.autoAdvance,
            onCheckedChange = viewModel::setAutoAdvance,
        )

        SectionHeader("Downloads")
        SwitchRow(
            title = "Download over Wi-Fi only",
            subtitle = "Never use mobile data for downloads",
            checked = settings.wifiOnlyDownloads,
            onCheckedChange = viewModel::setWifiOnlyDownloads,
        )
        SwitchRow(
            title = "Delete when finished",
            subtitle = "Remove the download once you finish an episode",
            checked = settings.autoDeleteFinished,
            onCheckedChange = viewModel::setAutoDeleteFinished,
        )

        SectionHeader("Updates")
        SwitchRow(
            title = "Background refresh",
            subtitle = "Check subscriptions for new episodes periodically",
            checked = settings.backgroundRefresh,
            onCheckedChange = viewModel::setBackgroundRefresh,
        )
        SwitchRow(
            title = "New episode notifications",
            subtitle = "Notify me when subscriptions publish new episodes",
            checked = settings.newEpisodeNotifications,
            onCheckedChange = viewModel::setNewEpisodeNotifications,
        )

        SectionHeader("Appearance")
        ChoiceRow(
            title = "Theme",
            current = themeLabel(settings.themeMode),
            options = ThemeMode.entries.map { themeLabel(it) },
            onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) },
        )
        SwitchRow(
            title = "Dynamic color",
            subtitle = "Tint the app from your wallpaper (Android 12+)",
            checked = settings.dynamicColor,
            onCheckedChange = viewModel::setDynamicColor,
        )

        Spacer(Modifier.padding(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(Modifier.padding(start = 16.dp))
}

@Composable
private fun ChoiceRow(
    title: String,
    current: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Box {
            TextButton(onClick = { open = true }) { Text(current) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEachIndexed { index, label ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onSelect(index); open = false },
                    )
                }
            }
        }
    }
    HorizontalDivider(Modifier.padding(start = 16.dp))
}

private val SKIP_BACK_OPTIONS = listOf(5_000L, 10_000L, 15_000L, 30_000L)
private val SKIP_FORWARD_OPTIONS = listOf(10_000L, 15_000L, 30_000L, 45_000L, 60_000L)

private fun secondsLabel(ms: Long): String = "${ms / 1000}s"

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
