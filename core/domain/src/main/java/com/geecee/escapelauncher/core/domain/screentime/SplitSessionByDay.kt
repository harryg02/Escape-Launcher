package com.geecee.escapelauncher.core.domain.screentime

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Splits the time an app was open by day, so time after midnight counts towards the new day rather
 * than all of it going to the day the app was left
 *
 * @param startMillis When the app was opened
 * @param endMillis When it was left
 * @param zone The time zone whose midnights split the days
 * @return Milliseconds per day, in order. Empty if [endMillis] isn't after [startMillis]
 */
fun splitSessionByDay(startMillis: Long, endMillis: Long, zone: ZoneId): Map<LocalDate, Long> {
    val days = linkedMapOf<LocalDate, Long>()
    var from = startMillis
    var day = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()

    while (from < endMillis) {
        val nextMidnight = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val until = minOf(nextMidnight, endMillis)
        days[day] = until - from
        from = until
        day = day.plusDays(1)
    }
    return days
}
