package com.geecee.escapelauncher.feature.screentime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geecee.escapelauncher.core.domain.repository.db.ScreenTimeRepository
import com.geecee.escapelauncher.core.domain.screentime.GetAppUsageUiListUseCase
import com.geecee.escapelauncher.core.domain.screentime.GetWeeklyUsageUseCase
import com.geecee.escapelauncher.core.model.AppUsageUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val DAYS_IN_WEEK = 7

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val screenTimeRepository: ScreenTimeRepository,
    private val getAppUsageUiListUseCase: GetAppUsageUiListUseCase,
    private val getWeeklyUsageUseCase: GetWeeklyUsageUseCase
) : ViewModel() {

    /**
     * The last seven days, oldest first
     *
     * @param dates As stored in the database ("yyyy-MM-dd")
     * @param labels Short day names to show
     */
    private data class WeekDates(val dates: List<String>, val labels: List<String>) {
        val today: String get() = dates.last()
    }

    private val datesFlow: Flow<WeekDates> = flow {
        while (true) {
            val now = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val labelFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val days = (DAYS_IN_WEEK - 1 downTo 0).map { daysAgo ->
                Calendar.getInstance().apply {
                    time = now
                    add(Calendar.DAY_OF_YEAR, -daysAgo)
                }.time
            }

            emit(WeekDates(days.map { dateFormat.format(it) }, days.map { labelFormat.format(it) }))

            // Calculate delay until next midnight
            val nextMidnight = Calendar.getInstance().apply {
                time = now
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delayMs = nextMidnight.timeInMillis - System.currentTimeMillis()
            delay((delayMs + 1000).milliseconds) // 1 second buffer to ensure we've crossed into the next day
        }
    }.distinctUntilChanged()

    val totalUsage: StateFlow<Long> = datesFlow.flatMapLatest { week ->
        screenTimeRepository.getTotalUsageForDateFlow(week.today)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val appUsageUiList: StateFlow<List<AppUsageUiModel>> = datesFlow.flatMapLatest { week ->
        getAppUsageUiListUseCase(week.today)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val weekUsage: StateFlow<WeekUi> = datesFlow.flatMapLatest { week ->
        getWeeklyUsageUseCase(week.dates).map { usage ->
            WeekUi(
                days = usage.days.mapIndexed { index, day ->
                    WeekDayUi(label = week.labels[index], totalTime = day.totalTime, apps = day.apps)
                },
                apps = usage.apps
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeekUi()
    )

    fun onAppOpened(packageName: String) {
        screenTimeRepository.onAppOpened(packageName)
    }

    suspend fun onAppClosed(packageName: String) {
        screenTimeRepository.onAppClosed(packageName)
    }

    fun hasActiveSession(): Boolean {
        return screenTimeRepository.hasActiveSession()
    }

    fun getActiveSessionPackageName(): String? {
        return screenTimeRepository.getActiveSessionPackageName()
    }

    fun getScreenTime(packageName: String): Long {
        return appUsageUiList.value.find { it.packageName == packageName }?.totalTime ?: 0L
    }
}
