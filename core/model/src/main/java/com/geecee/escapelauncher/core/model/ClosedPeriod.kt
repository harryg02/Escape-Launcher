package com.geecee.escapelauncher.core.model

import java.time.DayOfWeek

/**
 * A recurring time during which apps with the open countdown stay closed
 *
 * @param days The days this period starts on
 * @param startMinute Minutes after midnight when it starts
 * @param endMinute Minutes after midnight when it ends. If this is not after [startMinute], the
 * period runs past midnight into the next day; if it equals [startMinute] it lasts a whole day
 */
data class ClosedPeriod(
    val days: Set<DayOfWeek>,
    val startMinute: Int,
    val endMinute: Int
)
