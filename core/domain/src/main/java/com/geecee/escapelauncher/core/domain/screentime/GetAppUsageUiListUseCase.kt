package com.geecee.escapelauncher.core.domain.screentime

import com.geecee.escapelauncher.core.domain.repository.android.AppsRepository
import com.geecee.escapelauncher.core.domain.repository.db.ScreenTimeRepository
import com.geecee.escapelauncher.core.model.AppUsageUiModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Use case to process and format app usage data for display in the UI.
 * Joins one day's usage stats with app names.
 */
class GetAppUsageUiListUseCase @Inject constructor(
    private val screenTimeRepository: ScreenTimeRepository,
    private val appsRepository: AppsRepository
) {
    operator fun invoke(date: String): Flow<List<AppUsageUiModel>> {
        return combine(
            screenTimeRepository.getScreenTimeListSortedFlow(date),
            appsRepository.installedApps
        ) { usage, apps ->
            usage.withAppNames(apps.associate { it.packageName to it.displayName })
        }
    }
}
