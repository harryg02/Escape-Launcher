package com.geecee.escapelauncher.core.domain.screentime

import com.geecee.escapelauncher.core.domain.repository.android.AppsRepository
import com.geecee.escapelauncher.core.domain.repository.db.ScreenTimeRepository
import com.geecee.escapelauncher.core.model.AppUsage
import com.geecee.escapelauncher.core.model.AppUsageUiModel
import com.geecee.escapelauncher.core.model.InstalledApp
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetWeeklyUsageUseCaseTest {

    private val dates = listOf("2026-10-04", "2026-10-05")
    private val names = mapOf("chat" to "Chat", "video" to "Video")

    private val usage = mapOf(
        "2026-10-04" to listOf(AppUsage("chat", 1_000L), AppUsage("removed", 9_000L)),
        "2026-10-05" to listOf(AppUsage("video", 3_000L), AppUsage("chat", 500L))
    )

    @Test
    fun `days keep the requested order with their totals`() {
        val week = GetWeeklyUsageUseCase.summariseWeek(dates, usage, names)

        assertEquals(dates, week.days.map { it.date })
        assertEquals(listOf(1_000L, 3_500L), week.days.map { it.totalTime })
    }

    @Test
    fun `uninstalled apps are left out of totals and lists`() {
        val week = GetWeeklyUsageUseCase.summariseWeek(dates, usage, names)

        assertTrue(week.apps.none { it.packageName == "removed" })
        assertEquals(listOf(AppUsageUiModel("chat", "Chat", 1_000L)), week.days[0].apps)
    }

    @Test
    fun `week apps are added up across days, most time first`() {
        val week = GetWeeklyUsageUseCase.summariseWeek(dates, usage, names)

        assertEquals(
            listOf(
                AppUsageUiModel("video", "Video", 3_000L),
                AppUsageUiModel("chat", "Chat", 1_500L)
            ),
            week.apps
        )
    }

    @Test
    fun `days without data are empty`() {
        val week = GetWeeklyUsageUseCase.summariseWeek(listOf("2026-10-01"), emptyMap(), names)

        assertEquals(0L, week.days.single().totalTime)
        assertTrue(week.days.single().apps.isEmpty())
        assertTrue(week.apps.isEmpty())
    }

    @Test
    fun `invoke joins repository usage with installed app names`() = runTest {
        val screenTimeRepository: ScreenTimeRepository = mockk()
        val appsRepository: AppsRepository = mockk()
        val chat: InstalledApp = mockk()
        every { chat.packageName } returns "chat"
        every { chat.displayName } returns "Chat"
        every { screenTimeRepository.getUsageForDatesFlow(dates) } returns flowOf(usage)
        every { appsRepository.installedApps } returns MutableStateFlow(listOf(chat))

        val week = GetWeeklyUsageUseCase(screenTimeRepository, appsRepository)(dates).first()

        assertEquals(listOf(AppUsageUiModel("chat", "Chat", 1_500L)), week.apps)
    }
}
