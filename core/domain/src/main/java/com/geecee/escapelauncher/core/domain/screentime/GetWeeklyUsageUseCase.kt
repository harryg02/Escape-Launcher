package com.geecee.escapelauncher.core.domain.screentime

import com.geecee.escapelauncher.core.domain.repository.android.AppsRepository
import com.geecee.escapelauncher.core.domain.repository.db.ScreenTimeRepository
import com.geecee.escapelauncher.core.model.AppUsage
import com.geecee.escapelauncher.core.model.AppUsageUiModel
import com.geecee.escapelauncher.core.model.DayUsage
import com.geecee.escapelauncher.core.model.WeeklyUsage
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * What the time went to over several days: a total and app list per day, and the apps across
 * all of them. Usage of apps that are no longer installed is left out, as on the daily list.
 */
class GetWeeklyUsageUseCase @Inject constructor(
    private val screenTimeRepository: ScreenTimeRepository,
    private val appsRepository: AppsRepository
) {
    /**
     * @param dates The days to include, in the order they should be shown
     */
    operator fun invoke(dates: List<String>): Flow<WeeklyUsage> {
        return combine(
            screenTimeRepository.getUsageForDatesFlow(dates),
            appsRepository.installedApps
        ) { usageByDate, apps ->
            summariseWeek(dates, usageByDate, apps.associate { it.packageName to it.displayName })
        }
    }

    companion object {
        fun summariseWeek(
            dates: List<String>,
            usageByDate: Map<String, List<AppUsage>>,
            appNames: Map<String, String>
        ): WeeklyUsage {
            val days = dates.map { date ->
                val apps = usageByDate[date].orEmpty().withAppNames(appNames)
                DayUsage(date = date, totalTime = apps.sumOf { it.totalTime }, apps = apps)
            }

            val weekApps = days.flatMap { it.apps }
                .groupBy { it.packageName }
                .map { (_, entries) -> entries.first().copy(totalTime = entries.sumOf { it.totalTime }) }
                .sortedByDescending { it.totalTime }

            return WeeklyUsage(days = days, apps = weekApps)
        }
    }
}

/**
 * Adds display names, dropping apps that aren't installed, sorted with the most time first
 */
internal fun List<AppUsage>.withAppNames(appNames: Map<String, String>): List<AppUsageUiModel> =
    mapNotNull { usage ->
        val appName = appNames[usage.packageName] ?: return@mapNotNull null
        AppUsageUiModel(packageName = usage.packageName, appName = appName, totalTime = usage.totalTime)
    }.sortedByDescending { it.totalTime }
