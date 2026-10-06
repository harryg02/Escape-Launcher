package com.geecee.escapelauncher.core.domain.screentime

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * How a day is written in screen time keys: "yyyy-MM-dd" with ASCII digits whatever the device
 * language, so changing the language doesn't split or lose the user's history
 */
fun usageDate(day: LocalDate): String = day.format(DateTimeFormatter.ISO_LOCAL_DATE)
