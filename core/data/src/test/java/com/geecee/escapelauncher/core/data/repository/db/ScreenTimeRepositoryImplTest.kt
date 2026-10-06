package com.geecee.escapelauncher.core.data.repository.db

import com.geecee.escapelauncher.core.data.database.AppUsageDao
import com.geecee.escapelauncher.core.data.entity.AppUsageEntity
import com.geecee.escapelauncher.core.domain.screentime.usageDate
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ScreenTimeRepositoryImplTest {

    private val minute = 60_000L
    private val day = LocalDate.of(2026, 10, 5)
    private var now = day.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // Rows as the database would hold them
    private val rows = mutableMapOf<String, Long>()
    private val dao: AppUsageDao = mockk()
    private lateinit var repository: ScreenTimeRepositoryImpl

    private fun key(packageName: String, date: LocalDate = day) = usageKey(packageName, usageDate(date))

    @Before
    fun setup() {
        every { dao.getAllUsageFlow() } returns flowOf(emptyList())
        coEvery { dao.getAppUsage(any()) } answers {
            val key = firstArg<String>()
            rows[key]?.let { AppUsageEntity(key, it) }
        }
        coEvery { dao.insertOrUpdate(any()) } answers {
            val entity = firstArg<AppUsageEntity>()
            rows[entity.packageName] = entity.totalTime
        }
        repository = ScreenTimeRepositoryImpl(dao) { now }
    }

    @Test
    fun `closing the open app saves its time`() = runTest {
        repository.onAppOpened("com.example.chat")
        now += 5 * minute

        assertEquals(1, repository.onAppClosed("com.example.chat"))
        assertFalse(repository.hasActiveSession())
        assertEquals(mapOf(key("com.example.chat") to 5 * minute), rows)
    }

    @Test
    fun `opening another app ends the count for the one before`() = runTest {
        repository.onAppOpened("com.example.chat")
        now += 5 * minute
        repository.onAppOpened("com.example.video")
        now += 20 * minute
        repository.onAppClosed("com.example.video")

        assertEquals(
            mapOf(key("com.example.chat") to 5 * minute, key("com.example.video") to 20 * minute),
            rows
        )
    }

    @Test
    fun `time is added to what the app already had that day`() = runTest {
        rows[key("com.example.chat")] = 7 * minute

        repository.onAppOpened("com.example.chat")
        now += 3 * minute
        repository.onAppClosed("com.example.chat")

        assertEquals(10 * minute, rows[key("com.example.chat")])
    }

    @Test
    fun `time after midnight goes to the next day`() = runTest {
        now = day.atTime(23, 50).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        repository.onAppOpened("com.example.chat")
        now += 30 * minute
        repository.onAppClosed("com.example.chat")

        assertEquals(
            mapOf(key("com.example.chat") to 10 * minute, key("com.example.chat", day.plusDays(1)) to 20 * minute),
            rows
        )
    }

    @Test
    fun `closing an app that isn't the open one changes nothing`() = runTest {
        repository.onAppOpened("com.example.chat")
        now += 5 * minute

        assertEquals(0, repository.onAppClosed("com.example.video"))
        assertEquals("com.example.chat", repository.getActiveSessionPackageName())
        assertEquals(emptyMap<String, Long>(), rows)
    }

    @Test
    fun `an app is only counted once when it is closed twice`() = runTest {
        repository.onAppOpened("com.example.chat")
        now += 5 * minute
        repository.onAppClosed("com.example.chat")
        now += 5 * minute

        assertEquals(0, repository.onAppClosed("com.example.chat"))
        assertNull(repository.getActiveSessionPackageName())
        assertEquals(5 * minute, rows[key("com.example.chat")])
    }
}
