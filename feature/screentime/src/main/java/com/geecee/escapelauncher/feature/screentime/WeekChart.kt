package com.geecee.escapelauncher.feature.screentime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geecee.escapelauncher.core.common.formatScreenTime
import com.geecee.escapelauncher.core.theme.EscapeThemePreview

/**
 * One column per day showing how much the phone was used. Columns share one colour; the chosen
 * day is full strength and the rest are faded. Tapping a day chooses it. There are no numbers on
 * the columns, the chosen day's time is shown by the caller.
 *
 * @param days The days to show, oldest first
 * @param selectedIndex Index into [days] of the chosen day
 */
@Composable
fun WeekChart(
    days: List<WeekDayUi>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val longestDay = days.maxOfOrNull { it.totalTime }?.takeIf { it > 0 } ?: 1L

    Row(
        modifier
            .fillMaxWidth()
            .height(160.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, day ->
            DayColumn(
                day = day,
                fraction = (day.totalTime.toFloat() / longestDay).coerceIn(0f, 1f),
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayColumn(
    day: WeekDayUi,
    fraction: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = day.label + ", " + formatScreenTime(day.totalTime)

    Column(
        modifier
            .fillMaxHeight()
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Rounded at the top only so every column stands on the same baseline
            Box(
                Modifier
                    .width(20.dp)
                    .heightIn(min = 2.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 1f else 0.35f)
                    )
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = day.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

@Preview
@Composable
fun PrevWeekChart() {
    EscapeThemePreview {
        WeekChart(
            days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").mapIndexed { index, label ->
                WeekDayUi(label = label, totalTime = (index + 1) * 900_000L, apps = emptyList())
            },
            selectedIndex = 6,
            onSelect = {}
        )
    }
}
