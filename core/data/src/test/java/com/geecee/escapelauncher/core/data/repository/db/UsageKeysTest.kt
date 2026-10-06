package com.geecee.escapelauncher.core.data.repository.db

import com.geecee.escapelauncher.core.model.AppUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageKeysTest {

    private val rows = listOf(
        "com.example.chat-2026-10-05" to 120_000L,
        "com.example.video-player-2026-10-05" to 600_000L,
        "com.example.chat-2026-10-04" to 30_000L,
        "com.example.old-2026-09-01" to 5_000L
    )

    @Test
    fun `groups rows by date and keeps dashes in package names`() {
        val grouped = groupUsageByDate(rows, listOf("2026-10-04", "2026-10-05"))

        assertEquals(
            listOf(
                AppUsage("com.example.video-player", 600_000L),
                AppUsage("com.example.chat", 120_000L)
            ),
            grouped["2026-10-05"]
        )
        assertEquals(listOf(AppUsage("com.example.chat", 30_000L)), grouped["2026-10-04"])
    }

    @Test
    fun `dates without usage are present and empty`() {
        val grouped = groupUsageByDate(rows, listOf("2026-10-03"))

        assertEquals(mapOf("2026-10-03" to emptyList<AppUsage>()), grouped)
    }

    @Test
    fun `keys outside the kept dates are deleted`() {
        val toDelete = usageKeysToDelete(rows.map { it.first }, listOf("2026-10-04", "2026-10-05"))

        assertEquals(listOf("com.example.old-2026-09-01"), toDelete)
    }

    @Test
    fun `usage key joins package and date`() {
        assertEquals("com.example.chat-2026-10-05", usageKey("com.example.chat", "2026-10-05"))
    }
}
