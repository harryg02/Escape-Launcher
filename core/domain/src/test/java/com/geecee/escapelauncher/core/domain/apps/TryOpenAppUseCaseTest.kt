package com.geecee.escapelauncher.core.domain.apps

import com.geecee.escapelauncher.core.domain.repository.db.ModifiedAppsRepository
import com.geecee.escapelauncher.core.domain.repository.settings.AppPauseSettingsRepository
import com.geecee.escapelauncher.core.model.ClosedPeriod
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class TryOpenAppUseCaseTest {

    private val modifiedAppsRepository: ModifiedAppsRepository = mockk()
    private val appPauseSettingsRepository: AppPauseSettingsRepository = mockk()
    private lateinit var useCase: TryOpenAppUseCase

    private val monday = LocalDate.of(2026, 1, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))

    // Mondays 9:00 to 17:00
    private val workHours = ClosedPeriod(setOf(DayOfWeek.MONDAY), 9 * 60, 17 * 60)

    @Before
    fun setup() {
        coEvery { modifiedAppsRepository.isChallenge("flagged") } returns true
        coEvery { modifiedAppsRepository.isChallenge("normal") } returns false
        every { appPauseSettingsRepository.closedPeriods } returns flowOf(listOf(workHours))
        useCase = TryOpenAppUseCase(modifiedAppsRepository, appPauseSettingsRepository)
    }

    @Test
    fun `apps without the countdown always launch`() = runTest {
        assertEquals(TryOpenAppResult.Launch, useCase("normal", now = monday.atTime(10, 0)))
    }

    @Test
    fun `flagged app outside closed times shows the pause`() = runTest {
        assertEquals(TryOpenAppResult.ShowChallenge, useCase("flagged", now = monday.atTime(18, 0)))
    }

    @Test
    fun `flagged app during closed times is declined with the reopening time`() = runTest {
        assertEquals(
            TryOpenAppResult.Closed(monday.atTime(17, 0)),
            useCase("flagged", now = monday.atTime(10, 0))
        )
    }

    @Test
    fun `bypassing launches even during closed times`() = runTest {
        assertEquals(
            TryOpenAppResult.Launch,
            useCase("flagged", bypassChallenge = true, now = monday.atTime(10, 0))
        )
    }

    @Test
    fun `no closed times shows the pause`() = runTest {
        every { appPauseSettingsRepository.closedPeriods } returns flowOf(emptyList())

        assertEquals(TryOpenAppResult.ShowChallenge, useCase("flagged", now = monday.atTime(10, 0)))
    }
}
