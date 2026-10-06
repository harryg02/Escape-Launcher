package com.geecee.escapelauncher.core.domain.rhythms

import com.geecee.escapelauncher.core.model.ClosedPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

class ClosedScheduleTest {

    private val weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
    private val everyDay = DayOfWeek.values().toSet()

    // A known Monday, so tests don't depend on what weekday a literal date is
    private val monday: LocalDate = LocalDate.of(2026, 1, 1).with(TemporalAdjusters.nextOrSame(MONDAY))

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): LocalDateTime =
        day.atTime(hour, minute)

    private fun minutes(hour: Int, minute: Int = 0) = hour * 60 + minute

    private val weeknights = ClosedPeriod(weekdays, minutes(22), minutes(7))

    @Test
    fun `no periods means open`() {
        assertEquals(ClosedState.Open, ClosedSchedule.closedState(emptyList(), at(monday, 23)))
    }

    @Test
    fun `inside an overnight period reopens the next morning`() {
        assertEquals(
            ClosedState.Closed(at(monday.plusDays(1), 7)),
            ClosedSchedule.closedState(listOf(weeknights), at(monday, 23))
        )
    }

    @Test
    fun `early morning is covered by the period that started the night before`() {
        val tuesday = monday.plusDays(1)
        assertEquals(
            ClosedState.Closed(at(tuesday, 7)),
            ClosedSchedule.closedState(listOf(weeknights), at(tuesday, 6, 59))
        )
    }

    @Test
    fun `friday night period runs into saturday morning`() {
        val saturday = monday.plusDays(5)
        assertEquals(
            ClosedState.Closed(at(saturday, 7)),
            ClosedSchedule.closedState(listOf(weeknights), at(saturday, 6))
        )
    }

    @Test
    fun `days not chosen are open`() {
        val sunday = monday.plusDays(6)
        assertEquals(ClosedState.Open, ClosedSchedule.closedState(listOf(weeknights), at(sunday, 23)))
        // Monday morning was not preceded by a Sunday night period
        assertEquals(ClosedState.Open, ClosedSchedule.closedState(listOf(weeknights), at(monday, 6)))
    }

    @Test
    fun `start is closed and end is open`() {
        val workHours = ClosedPeriod(setOf(MONDAY), minutes(9), minutes(17))

        assertEquals(ClosedState.Open, ClosedSchedule.closedState(listOf(workHours), at(monday, 8, 59)))
        assertEquals(
            ClosedState.Closed(at(monday, 17)),
            ClosedSchedule.closedState(listOf(workHours), at(monday, 9))
        )
        assertEquals(ClosedState.Open, ClosedSchedule.closedState(listOf(workHours), at(monday, 17)))
    }

    @Test
    fun `touching periods are treated as one`() {
        val lateEvening = ClosedPeriod(everyDay, minutes(22), 0)
        val night = ClosedPeriod(everyDay, 0, minutes(7))

        assertEquals(
            ClosedState.Closed(at(monday.plusDays(1), 7)),
            ClosedSchedule.closedState(listOf(lateEvening, night), at(monday, 23))
        )
    }

    @Test
    fun `overlapping periods use the later end`() {
        val first = ClosedPeriod(setOf(MONDAY), minutes(9), minutes(12))
        val second = ClosedPeriod(setOf(MONDAY), minutes(11), minutes(14))

        assertEquals(
            ClosedState.Closed(at(monday, 14)),
            ClosedSchedule.closedState(listOf(first, second), at(monday, 10))
        )
    }

    @Test
    fun `equal start and end lasts the whole day`() {
        val sunday = monday.plusDays(6)
        val allSunday = ClosedPeriod(setOf(SUNDAY), 0, 0)

        assertEquals(
            ClosedState.Closed(at(sunday.plusDays(1), 0)),
            ClosedSchedule.closedState(listOf(allSunday), at(sunday, 12))
        )
        assertEquals(ClosedState.Open, ClosedSchedule.closedState(listOf(allSunday), at(monday, 12)))
    }

    @Test
    fun `always closed has no reopening time`() {
        val always = ClosedPeriod(everyDay, 0, 0)

        assertEquals(ClosedState.Closed(null), ClosedSchedule.closedState(listOf(always), at(monday, 12)))
    }

    @Test
    fun `periods without days are ignored`() {
        val noDays = ClosedPeriod(emptySet(), 0, 0)

        assertEquals(ClosedState.Open, ClosedSchedule.closedState(listOf(noDays), at(monday, 12)))
    }

    @Test
    fun `encode and decode round trip`() {
        val periods = listOf(weeknights, ClosedPeriod(setOf(SATURDAY, SUNDAY), 0, 0))

        assertEquals(periods, ClosedSchedule.decode(ClosedSchedule.encode(periods)))
    }

    @Test
    fun `encode uses iso day numbers`() {
        assertEquals("1,2,3,4,5|1320|420", ClosedSchedule.encode(listOf(weeknights)))
    }

    @Test
    fun `decode skips malformed entries`() {
        val decoded = ClosedSchedule.decode("1,2|60|120;nonsense;8|0|0;3|abc|10;4|0|5000;;")

        assertEquals(listOf(ClosedPeriod(setOf(MONDAY, TUESDAY), 60, 120)), decoded)
    }

    @Test
    fun `decode of empty text is empty`() {
        assertTrue(ClosedSchedule.decode("").isEmpty())
    }
}
