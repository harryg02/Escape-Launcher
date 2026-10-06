package com.geecee.escapelauncher.core.domain.screentime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SplitSessionByDayTest {

    private val utc = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 10, 5)

    private fun millis(time: LocalDateTime, zone: ZoneId = utc) = time.atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `a session within one day all goes to that day`() {
        val split = splitSessionByDay(millis(day.atTime(10, 0)), millis(day.atTime(10, 30)), utc)

        assertEquals(mapOf(day to 30 * 60_000L), split)
    }

    @Test
    fun `a session past midnight is split between the two days`() {
        val split = splitSessionByDay(millis(day.atTime(23, 50)), millis(day.plusDays(1).atTime(0, 20)), utc)

        assertEquals(mapOf(day to 10 * 60_000L, day.plusDays(1) to 20 * 60_000L), split)
    }

    @Test
    fun `a session over several days gives each day its share`() {
        val split = splitSessionByDay(millis(day.atTime(12, 0)), millis(day.plusDays(2).atTime(1, 0)), utc)

        assertEquals(
            mapOf(
                day to 12 * 3_600_000L,
                day.plusDays(1) to 24 * 3_600_000L,
                day.plusDays(2) to 3_600_000L
            ),
            split
        )
    }

    @Test
    fun `nothing is counted if the end is not after the start`() {
        val start = millis(day.atTime(10, 0))

        assertEquals(emptyMap<LocalDate, Long>(), splitSessionByDay(start, start, utc))
        assertEquals(emptyMap<LocalDate, Long>(), splitSessionByDay(start, start - 1, utc))
    }

    @Test
    fun `days follow the given time zone`() {
        // 23:30 to 00:30 in Berlin is 21:30 to 22:30 UTC (summer time), all one day in UTC
        val berlin = ZoneId.of("Europe/Berlin")
        val start = millis(day.atTime(23, 30), berlin)
        val end = millis(day.plusDays(1).atTime(0, 30), berlin)

        assertEquals(mapOf(day to 30 * 60_000L, day.plusDays(1) to 30 * 60_000L), splitSessionByDay(start, end, berlin))
        assertEquals(mapOf(day to 60 * 60_000L), splitSessionByDay(start, end, utc))
    }

    @Test
    fun `a day that is shorter because of a clock change is measured in real time`() {
        // Europe/Berlin moves its clocks forward at 02:00 on 2026-03-29, so that day has 23 hours
        val berlin = ZoneId.of("Europe/Berlin")
        val changeDay = LocalDate.of(2026, 3, 29)
        val start = millis(changeDay.atStartOfDay(), berlin)
        val end = millis(changeDay.plusDays(1).atStartOfDay(), berlin)

        assertEquals(mapOf(changeDay to 23 * 3_600_000L), splitSessionByDay(start, end, berlin))
    }
}
