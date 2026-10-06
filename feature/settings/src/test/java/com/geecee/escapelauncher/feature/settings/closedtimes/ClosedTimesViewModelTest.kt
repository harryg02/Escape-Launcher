package com.geecee.escapelauncher.feature.settings.closedtimes

import com.geecee.escapelauncher.core.domain.repository.settings.AppPauseSettingsRepository
import com.geecee.escapelauncher.core.model.ClosedPeriod
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class ClosedTimesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val appPauseSettingsRepository: AppPauseSettingsRepository = mockk(relaxUnitFun = true)
    private lateinit var viewModel: ClosedTimesViewModel

    private val nights = ClosedPeriod(setOf(DayOfWeek.MONDAY), 22 * 60, 7 * 60)
    private val mornings = ClosedPeriod(setOf(DayOfWeek.SATURDAY), 6 * 60, 9 * 60)
    private val evenings = ClosedPeriod(setOf(DayOfWeek.SUNDAY), 18 * 60, 21 * 60)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { appPauseSettingsRepository.closedPeriods } returns flowOf(listOf(nights, mornings))
        viewModel = ClosedTimesViewModel(appPauseSettingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saving a new period adds it to the end`() {
        viewModel.savePeriod(null, evenings)

        coVerify { appPauseSettingsRepository.setClosedPeriods(listOf(nights, mornings, evenings)) }
    }

    @Test
    fun `saving an edited period replaces it in place`() {
        viewModel.savePeriod(0, evenings)

        coVerify { appPauseSettingsRepository.setClosedPeriods(listOf(evenings, mornings)) }
    }

    @Test
    fun `removing a period keeps the others`() {
        viewModel.removePeriod(0)

        coVerify { appPauseSettingsRepository.setClosedPeriods(listOf(mornings)) }
    }

    @Test
    fun `removing an index that no longer exists changes nothing`() {
        viewModel.removePeriod(5)

        coVerify(exactly = 0) { appPauseSettingsRepository.setClosedPeriods(any()) }
    }
}
