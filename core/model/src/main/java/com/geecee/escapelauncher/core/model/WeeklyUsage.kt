package com.geecee.escapelauncher.core.model

/**
 * Screen time over several days, for reflecting on where the time went
 *
 * @param days One entry per day, in the order they were asked for
 * @param apps Each app's time added up across all [days], most time first
 */
data class WeeklyUsage(
    val days: List<DayUsage> = emptyList(),
    val apps: List<AppUsageUiModel> = emptyList()
)

/**
 * @param date The day, formatted as it is stored ("yyyy-MM-dd")
 * @param apps Time per app that day, most time first
 */
data class DayUsage(
    val date: String,
    val totalTime: Long,
    val apps: List<AppUsageUiModel>
)
