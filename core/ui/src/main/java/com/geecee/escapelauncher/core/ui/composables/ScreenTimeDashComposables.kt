package com.geecee.escapelauncher.core.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shows how long one app was used for
 */
@Composable
fun AppUsage(appName: String, time: String, modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(5.dp, 5.dp)
    ) {
        Text(
            text = if (appName.length > 12) appName.take(12) + "..." else appName,
            modifier = Modifier.align(Alignment.CenterStart),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterEnd),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Box with [AppUsage]s in it
 */
@Composable
fun AppUsages(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(48.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            Modifier.padding(20.dp)
        ) {
            content()
        }
    }
}
