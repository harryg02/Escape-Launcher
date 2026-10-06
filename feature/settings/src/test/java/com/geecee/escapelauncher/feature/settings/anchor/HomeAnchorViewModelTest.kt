package com.geecee.escapelauncher.feature.settings.anchor

import com.geecee.escapelauncher.core.domain.repository.settings.HomeAnchorRepository
import com.geecee.escapelauncher.core.model.HomeAnchor
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeAnchorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val homeAnchorRepository: HomeAnchorRepository = mockk(relaxUnitFun = true)
    private lateinit var viewModel: HomeAnchorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeAnchorViewModel(homeAnchorRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saving blank text removes the anchor`() {
        viewModel.saveHomeAnchor("   ", "0123 456")

        coVerify { homeAnchorRepository.clearHomeAnchor() }
        coVerify(exactly = 0) { homeAnchorRepository.setHomeAnchor(any()) }
    }

    @Test
    fun `saving text stores the text and number`() {
        viewModel.saveHomeAnchor("Call Sam", "0123 456")

        coVerify { homeAnchorRepository.setHomeAnchor(HomeAnchor("Call Sam", "0123 456")) }
    }

    @Test
    fun `remove clears the anchor`() {
        viewModel.clearHomeAnchor()

        coVerify { homeAnchorRepository.clearHomeAnchor() }
    }

    @Test
    fun `loads the saved anchor`() = runTest {
        every { homeAnchorRepository.homeAnchor } returns flowOf(HomeAnchor("Evening walk", ""))

        assertEquals(HomeAnchor("Evening walk", ""), viewModel.loadHomeAnchor())
    }
}
