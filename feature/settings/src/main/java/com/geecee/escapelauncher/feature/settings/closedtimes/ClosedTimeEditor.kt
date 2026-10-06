package com.geecee.escapelauncher.feature.settings.closedtimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.geecee.escapelauncher.core.model.ClosedPeriod
import com.geecee.escapelauncher.core.ui.R
import java.time.DayOfWeek

private const val DEFAULT_START_MINUTE = 22 * 60
private const val DEFAULT_END_MINUTE = 7 * 60

/**
 * Dialog for adding or changing one closed time: which days it starts on, and from when until when
 *
 * @param initial The period being edited, or null when adding a new one
 * @param onSave Called with the finished period
 * @param onDismiss Called when the dialog is closed without saving
 */
@Composable
fun ClosedTimeEditor(
    initial: ClosedPeriod?,
    onSave: (ClosedPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Days are kept as a bit mask (bit 0 is Monday) so they survive being saved
    var dayMask by rememberSaveable {
        mutableIntStateOf(daysToMask(initial?.days ?: DayOfWeek.values().toSet()))
    }
    var startMinute by rememberSaveable { mutableIntStateOf(initial?.startMinute ?: DEFAULT_START_MINUTE) }
    var endMinute by rememberSaveable { mutableIntStateOf(initial?.endMinute ?: DEFAULT_END_MINUTE) }
    // null when no time is being picked, true for the start time, false for the end time
    var pickingStart by rememberSaveable { mutableStateOf<Boolean?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(ClosedPeriod(maskToDays(dayMask), startMinute, endMinute)) },
                enabled = dayMask != 0
            ) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Text(stringResource(R.string.closed_time), style = MaterialTheme.typography.bodyLarge)
        },
        text = {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    orderedDays().forEach { day ->
                        DayToggle(
                            label = shortDayName(day),
                            selected = dayMask and dayBit(day) != 0,
                            onToggle = { dayMask = dayMask xor dayBit(day) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = { pickingStart = true }) {
                    Text(
                        stringResource(R.string.closed_time_from, formatMinuteOfDay(context, startMinute)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = { pickingStart = false }) {
                    Text(
                        stringResource(R.string.closed_time_until, formatMinuteOfDay(context, endMinute)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (endMinute <= startMinute) {
                    Text(
                        text = stringResource(
                            if (endMinute == startMinute) R.string.closed_time_whole_day else R.string.closed_time_next_day
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )

    pickingStart?.let { isStart ->
        TimeDialog(
            initialMinuteOfDay = if (isStart) startMinute else endMinute,
            onConfirm = { minuteOfDay ->
                if (isStart) startMinute = minuteOfDay else endMinute = minuteOfDay
                pickingStart = null
            },
            onDismiss = { pickingStart = null }
        )
    }
}

@Composable
private fun DayToggle(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    initialMinuteOfDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = { TimeInput(state = state) }
    )
}

private fun dayBit(day: DayOfWeek): Int = 1 shl (day.value - 1)

private fun daysToMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or dayBit(day) }

private fun maskToDays(mask: Int): Set<DayOfWeek> =
    DayOfWeek.values().filter { mask and dayBit(it) != 0 }.toSet()
