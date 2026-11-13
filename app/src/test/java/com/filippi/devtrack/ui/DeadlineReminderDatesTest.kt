package com.filippi.devtrack.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DeadlineReminderDatesTest {
    private val today = LocalDate.of(2026, 11, 13)

    @Test fun sendsOnlyAtTheThreeRequestedOffsets() {
        for (days in 0..8) {
            val deadline = today.plusDays(days.toLong()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            if (days in listOf(1, 5, 7)) assertEquals(days, reminderOffset(deadline, today))
            else assertNull(reminderOffset(deadline, today))
        }
    }

    @Test fun usesTheStoredUtcCalendarDate() {
        val deadline = LocalDate.of(2026, 11, 14).atStartOfDay()
            .toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(1, reminderOffset(deadline, today))
    }
}
