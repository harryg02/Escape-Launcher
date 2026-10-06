package com.geecee.escapelauncher.feature.screentime

import com.geecee.escapelauncher.core.model.AppUsageUiModel

/**
 * The last seven days for the screen time page, oldest first, so today is the last day
 *
 * @param apps Each app's time across the whole week, most time first
 */
data class WeekUi(
    val days: List<WeekDayUi> = emptyList(),
    val apps: List<AppUsageUiModel> = emptyList()
)

/**
 * @param label Short day name, such as "Mon"
 * @param apps Time per app that day, most time first
 */
data class WeekDayUi(
    val label: String,
    val totalTime: Long,
    val apps: List<AppUsageUiModel>
)
