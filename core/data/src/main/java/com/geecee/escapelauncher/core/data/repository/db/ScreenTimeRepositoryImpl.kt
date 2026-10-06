package com.geecee.escapelauncher.core.data.repository.db

import android.util.Log
import com.geecee.escapelauncher.core.data.database.AppUsageDao
import com.geecee.escapelauncher.core.data.entity.AppUsageEntity
import com.geecee.escapelauncher.core.domain.repository.db.ScreenTimeRepository
import com.geecee.escapelauncher.core.domain.screentime.splitSessionByDay
import com.geecee.escapelauncher.core.domain.screentime.usageDate
import com.geecee.escapelauncher.core.model.AppUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenTimeRepositoryImpl internal constructor(
    private val appUsageDao: AppUsageDao,
    private val currentTimeMillis: () -> Long
) : ScreenTimeRepository {
    @Inject
    constructor(appUsageDao: AppUsageDao) : this(appUsageDao, System::currentTimeMillis)

    private data class Session(val packageName: String, val startMillis: Long)

    // The app last opened from the launcher, until the user is back home or the screen turns off
    private val activeSession = AtomicReference<Session?>(null)

    // Saving reads and then writes a row, so two saves at once could lose one of them
    private val saveLock = Mutex()

    override val allUsageFlow: Flow<List<AppUsage>> = appUsageDao.getAllUsageFlow().map { entities ->
        entities.map { usage ->
            AppUsage(
                packageName = usage.packageName.substringBeforeLast("-"),
                totalTime = usage.totalTime
            )
        }
    }

    override suspend fun onAppOpened(packageName: String) {
        val now = currentTimeMillis()
        // The launcher only opens one app at a time, so if an earlier one was still counting
        // (e.g. the launcher stayed visible in split screen) the user has moved on from it
        activeSession.getAndSet(Session(packageName, now))?.let { save(it, now) }
    }

    override fun hasActiveSession(): Boolean {
        return activeSession.get() != null
    }

    override fun getActiveSessionPackageName(): String? {
        return activeSession.get()?.packageName
    }

    override suspend fun onAppClosed(packageName: String): Int {
        val session = activeSession.get() ?: return 0
        // Clear first so a concurrent close (screen-off receiver + onResume) can't double count
        if (session.packageName != packageName || !activeSession.compareAndSet(session, null)) return 0
        return save(session, currentTimeMillis())
    }

    /**
     * Adds a session's time to its app, split at midnight so each day gets the time spent in it
     */
    private suspend fun save(session: Session, endMillis: Long): Int = saveLock.withLock {
        try {
            splitSessionByDay(session.startMillis, endMillis, ZoneId.systemDefault()).forEach { (day, time) ->
                val appKey = usageKey(session.packageName, usageDate(day))
                val existingTime = appUsageDao.getAppUsage(appKey)?.totalTime ?: 0L
                appUsageDao.insertOrUpdate(AppUsageEntity(packageName = appKey, totalTime = existingTime + time))
            }
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
