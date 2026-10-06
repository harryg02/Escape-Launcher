package com.geecee.escapelauncher.feature.settings.closedtimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.geecee.escapelauncher.core.ui.R
import com.geecee.escapelauncher.core.ui.composables.EscapeHeader
import com.geecee.escapelauncher.core.ui.composables.SettingsButton
import com.geecee.escapelauncher.core.ui.composables.SettingsSpacer
import com.geecee.escapelauncher.core.ui.composables.SettingsSwipeableButton

/**
 * Lists the user's closed times, during which apps with the countdown stay closed. Tap one to
 * change it, swipe it away to remove it.
 *
 * @param goBack When back button is pressed
 */
@Composable
fun ClosedTimesOptions(
    goBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClosedTimesViewModel = hiltViewModel()
) {
    val periods by viewModel.closedPeriods.collectAsState()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    // Index of the period being edited, or null when adding a new one
    var editingIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            item(key = "header") { EscapeHeader(goBack, stringResource(R.string.closed_times)) }

            item(key = "description") {
                Text(
                    text = stringResource(R.string.closed_times_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
            }

            itemsIndexed(periods, key = { index, period -> "$index-$period" }) { index, period ->
                SettingsSwipeableButton(
                    label = describePeriod(period),
                    onClick = {
                        editingIndex = index
                        showEditor = true
                    },
                    onDeleteClick = { viewModel.removePeriod(index) },
                    isTopOfGroup = index == 0,
                    deleteIconContentDescription = stringResource(R.string.remove)
                )
            }

            item(key = "add") {
                SettingsButton(
                    label = stringResource(R.string.add_closed_time),
                    isTopOfGroup = periods.isEmpty(),
                    isBottomOfGroup = true,
                    onClick = {
                        editingIndex = null
                        showEditor = true
                    }
                )
            }

            item(key = "spacer1") { SettingsSpacer() }
            item(key = "spacer2") { SettingsSpacer() }
        }
    }

    if (showEditor) {
        ClosedTimeEditor(
            initial = editingIndex?.let { periods.getOrNull(it) },
            onSave = { period ->
                viewModel.savePeriod(editingIndex, period)
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }
}
