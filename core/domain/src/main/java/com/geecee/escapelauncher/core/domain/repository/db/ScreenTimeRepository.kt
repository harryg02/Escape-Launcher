package com.geecee.escapelauncher.core.domain.repository.db

import com.geecee.escapelauncher.core.model.AppUsage
import kotlinx.coroutines.flow.Flow

interface ScreenTimeRepository {
    /**
     * Starts counting time for an app opened from the launcher. Ends the count for any app opened
     * before it that hadn't been closed yet.
     */
    suspend fun onAppOpened(packageName: String)
    suspend fun onAppClosed(packageName: String): Int
    fun hasActiveSession(): Boolean
    fun getActiveSessionPackageName(): String?
    suspend fun clearOldData()
    suspend fun getTotalUsageForDate(date: String): Long
    suspend fun getUsageForApp(packageName: String, date: String): Long
    suspend fun getScreenTimeListSorted(date: String): List<AppUsage>
    fun getScreenTimeListSortedFlow(date: String): Flow<List<AppUsage>>
    fun getTotalUsageForDateFlow(date: String): Flow<Long>

    /**
     * Usage per app for each of [dates], sorted by time. Every date is a key in the map, with an
     * empty list if nothing was used that day.
     */
    fun getUsageForDatesFlow(dates: List<String>): Flow<Map<String, List<AppUsage>>>
    val allUsageFlow: Flow<List<AppUsage>>
}
