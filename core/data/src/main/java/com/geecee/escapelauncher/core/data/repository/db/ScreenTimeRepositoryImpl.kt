package com.geecee.escapelauncher.core.data.repository.db

import android.util.Log
import com.geecee.escapelauncher.core.data.database.AppUsageDao
import com.geecee.escapelauncher.core.data.entity.AppUsageEntity
import com.geecee.escapelauncher.core.domain.repository.db.ScreenTimeRepository
import com.geecee.escapelauncher.core.domain.screentime.usageDate
import com.geecee.escapelauncher.core.model.AppUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenTimeRepositoryImpl @Inject constructor(
    private val appUsageDao: AppUsageDao
) : ScreenTimeRepository {
    private val appSessions = ConcurrentHashMap<String, Long>()

    override val allUsageFlow: Flow<List<AppUsage>> = appUsageDao.getAllUsageFlow().map { entities ->
        entities.map { usage ->
            AppUsage(
                packageName = usage.packageName.substringBeforeLast("-"),
                totalTime = usage.totalTime
            )
        }
    }

    override fun onAppOpened(packageName: String) {
        appSessions[packageName] = System.currentTimeMillis()
    }

    override fun hasActiveSession(): Boolean {
        return appSessions.isNotEmpty()
    }

    override fun getActiveSessionPackageName(): String? {
        return appSessions.keys().asSequence().firstOrNull()
    }

    override suspend fun onAppClosed(packageName: String): Int {
        // Remove first so a concurrent close (screen-off receiver + onResume) can't double count
        val openTime = appSessions.remove(packageName) ?: return 0
        val usageTime = System.currentTimeMillis() - openTime
        val appKey = usageKey(packageName, usageDate(LocalDate.now()))

        return try {
            val existingUsage = appUsageDao.getAppUsage(appKey)
            val updatedTime = (existingUsage?.totalTime ?: 0L) + usageTime

            appUsageDao.insertOrUpdate(
                AppUsageEntity(
                    packageName = appKey,
                    totalTime = updatedTime
                )
            )
            1
        } catch (e: Exception) {
            Log.e("ScreenTimeRepository", "Error saving app usage: ${e.message}")
            0
        }
    }

    /**
     * Keeps the last [DAYS_KEPT] days (today included), which is what the weekly view shows
     */
    override suspend fun clearOldData() {
        val today = LocalDate.now()
        val keepDates = (0 until DAYS_KEPT).map { daysAgo -> usageDate(today.minusDays(daysAgo.toLong())) }

        try {
            val keys = appUsageDao.getAllUsage().map { it.packageName }
            // Chunked to stay under SQLite's bound variable limit on older Android versions
            usageKeysToDelete(keys, keepDates).chunked(500).forEach { appUsageDao.deleteByKeys(it) }
        } catch (e: Exception) {
            Log.e("ScreenTimeRepository", "Error clearing old data: ${e.message}")
        }
    }

    override suspend fun getTotalUsageForDate(date: String): Long {
        return appUsageDao.getTotalUsageForDate("%-$date") ?: 0L
    }

    override fun getTotalUsageForDateFlow(date: String): Flow<Long> {
        return appUsageDao.getTotalUsageForDateFlow("%-$date").map { it ?: 0L }
    }

    override suspend fun getUsageForApp(packageName: String, date: String): Long {
        return appUsageDao.getAppUsage("$packageName-$date")?.totalTime ?: 0L
    }

    override suspend fun getScreenTimeListSorted(date: String): List<AppUsage> {
        val usageList = appUsageDao.getUsageListForDate("%-$date")
        return usageList.map { usage ->
            AppUsage(
                packageName = usage.packageName.substringBeforeLast("-$date"),
                totalTime = usage.totalTime
            )
        }.sortedByDescending { it.totalTime }
    }

    override fun getScreenTimeListSortedFlow(date: String): Flow<List<AppUsage>> {
        return appUsageDao.getUsageListForDateFlow("%-$date").map { usageList ->
            usageList.map { usage ->
                AppUsage(
                    packageName = usage.packageName.substringBeforeLast("-$date"),
                    totalTime = usage.totalTime
                )
            }.sortedByDescending { it.totalTime }
        }
    }

    override fun getUsageForDatesFlow(dates: List<String>): Flow<Map<String, List<AppUsage>>> {
        return appUsageDao.getAllUsageFlow().map { entities ->
            groupUsageByDate(entities.map { it.packageName to it.totalTime }, dates)
        }
    }

    private companion object {
        const val DAYS_KEPT = 7
    }
}
