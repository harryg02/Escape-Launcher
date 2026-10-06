package com.geecee.escapelauncher.feature.screentime

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.geecee.escapelauncher.core.common.formatScreenTime
import com.geecee.escapelauncher.core.model.AppUsageUiModel
import com.geecee.escapelauncher.core.ui.R
import com.geecee.escapelauncher.core.ui.composables.AppUsage
import com.geecee.escapelauncher.core.ui.composables.AppUsages
import com.geecee.escapelauncher.core.ui.composables.SettingsSpacer

// Long lists make the page a scoreboard; the rest of the apps add little to the picture
private const val MAX_APPS_SHOWN = 10

/**
 * Screen time page. Shows where the time went rather than judging it: a column per day for the
 * last week, the chosen day's apps, and the apps across the whole week.
 */
@Composable
fun ScreenTimeDashboard(
    screenTimeViewModel: ScreenTimeViewModel = hiltViewModel(LocalActivity.current as ComponentActivity)
) {
    val week by screenTimeViewModel.weekUsage.collectAsState()
    // -1 means today, so the page follows the date when it changes at midnight
    var chosenDay by rememberSaveable { mutableIntStateOf(-1) }
    val selectedIndex = if (chosenDay in week.days.indices) chosenDay else week.days.lastIndex
    val selectedDay = week.days.getOrNull(selectedIndex)
    val isToday = selectedIndex == week.days.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .padding(15.dp, 0.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(120.dp))

        Text(
            text = if (isToday || selectedDay == null) stringResource(R.string.today) else selectedDay.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = formatScreenTime(selectedDay?.totalTime ?: 0L),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(15.dp))

        if (week.days.isNotEmpty()) {
            WeekChart(
                days = week.days,
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    chosenDay = if (index == week.days.lastIndex) -1 else index
                },
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .clip(RoundedCornerShape(48.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(20.dp)
            )
        }

        Spacer(Modifier.height(30.dp))

        UsageSection(
            title = stringResource(R.string.where_the_time_went),
            apps = selectedDay?.apps.orEmpty()
        )

        Spacer(Modifier.height(30.dp))

        UsageSection(
            title = stringResource(R.string.last_seven_days),
            apps = week.apps
        )

        Spacer(Modifier.height(15.dp))

        // What is and isn't counted, so the numbers aren't read as more exact than they are
        Text(
            text = stringResource(R.string.screen_time_counting_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        Spacer(Modifier.height(15.dp))

        SettingsSpacer()
        SettingsSpacer()
    }
}

@Composable
private fun UsageSection(
    title: String,
    apps: List<AppUsageUiModel>,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 10.dp, bottom = 8.dp)
        )

        AppUsages(Modifier) {
            if (apps.isNotEmpty()) {
                apps.take(MAX_APPS_SHOWN).forEach { app ->
                    AppUsage(
                        app.appName,
                        if (app.totalTime > 60000) formatScreenTime(app.totalTime) else "<1m",
                        Modifier
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.no_apps_used),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
