package com.geecee.escapelauncher.core.domain.rhythms

import com.geecee.escapelauncher.core.model.ClosedPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Whether apps with the open countdown are closed at a given moment
 */
sealed interface ClosedState {
    data object Open : ClosedState

    /**
     * @param reopensAt When apps open again, or null if that is more than a week away
     */
    data class Closed(val reopensAt: LocalDateTime?) : ClosedState
}

/**
 * Works out closed times from the user's [ClosedPeriod]s. Pure logic, no Android calls.
 */
object ClosedSchedule {
    private const val MINUTES_IN_DAY = 24 * 60
    private const val MAX_LOOKAHEAD_DAYS = 7L
    private const val MAX_STEPS = 100

    /**
     * Finds whether [now] is inside a closed period and, if so, when apps open again. Periods
     * that touch or overlap are treated as one closed stretch.
     */
    fun closedState(periods: List<ClosedPeriod>, now: LocalDateTime): ClosedState {
        var reopensAt = latestEndContaining(periods, now) ?: return ClosedState.Open
        val limit = now.plusDays(MAX_LOOKAHEAD_DAYS)

        repeat(MAX_STEPS) {
            if (reopensAt.isAfter(limit)) return ClosedState.Closed(null)
            reopensAt = latestEndContaining(periods, reopensAt) ?: return ClosedState.Closed(reopensAt)
        }
        return ClosedState.Closed(null)
    }

    /**
     * The latest end of any period occurrence that contains [time], or null if none does.
     * Occurrences last at most a day, so only ones starting today or yesterday can contain it.
     */
    private fun latestEndContaining(periods: List<ClosedPeriod>, time: LocalDateTime): LocalDateTime? {
        val today = time.toLocalDate()
        return periods
            .filter { isValid(it) }
            .flatMap { period ->
                listOf(today.minusDays(1), today).mapNotNull { day -> occurrence(period, day) }
            }
            .filter { (start, end) -> !time.isBefore(start) && time.isBefore(end) }
            .maxOfOrNull { (_, end) -> end }
    }

    private fun occurrence(period: ClosedPeriod, day: LocalDate): Pair<LocalDateTime, LocalDateTime>? {
        if (day.dayOfWeek !in period.days) return null
        val start = day.atStartOfDay().plusMinutes(period.startMinute.toLong())
        val length = if (period.endMinute > period.startMinute) {
            period.endMinute - period.startMinute
        } else {
            MINUTES_IN_DAY - period.startMinute + period.endMinute
        }
        return start to start.plusMinutes(length.toLong())
    }

    fun isValid(period: ClosedPeriod): Boolean =
        period.days.isNotEmpty() &&
                period.startMinute in 0 until MINUTES_IN_DAY &&
                period.endMinute in 0 until MINUTES_IN_DAY

    /**
     * Stores periods as text, e.g. "1,2,3,4,5|1320|420;6,7|0|0". Days use ISO numbers (Monday = 1).
     */
    fun encode(periods: List<ClosedPeriod>): String =
        periods.filter { isValid(it) }.joinToString(";") { period ->
            val days = period.days.map { it.value }.sorted().joinToString(",")
            "$days|${period.startMinute}|${period.endMinute}"
        }

    /**
     * Reads text written by [encode]. Anything malformed is skipped rather than failing.
     */
    fun decode(text: String): List<ClosedPeriod> =
        text.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size != 3) return@mapNotNull null
            val days = parts[0].split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .map { DayOfWeek.of(it) }
                .toSet()
            val start = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
            val end = parts[2].trim().toIntOrNull() ?: return@mapNotNull null
            ClosedPeriod(days, start, end).takeIf { isValid(it) }
        }
}
