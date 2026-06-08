package com.ondes.podcast.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ondes.podcast.ui.theme.OndesTheme

/**
 * One shared, on-brand empty / zero state: brand-tinted icon, a headline, a
 * supporting line and an optional call-to-action. Used wherever a list can be
 * empty (library, downloads, search, home) so first-run and edge states feel
 * intentional and guide the user to the next step instead of showing a blank
 * screen or a stray line of grey text.
 */
@Composable
fun OndesEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(OndesTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OndesTheme.colors.brand,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(OndesTheme.spacing.lg))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(OndesTheme.spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(OndesTheme.spacing.xl))
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
