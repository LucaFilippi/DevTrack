package com.filippi.devtrack.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

internal fun reminderOffset(deadlineAt: Long, today: LocalDate): Int? {
    val deadline = Instant.ofEpochMilli(deadlineAt).atOffset(ZoneOffset.UTC).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, deadline).toInt()
    return days.takeIf { it == 7 || it == 5 || it == 1 }
}
