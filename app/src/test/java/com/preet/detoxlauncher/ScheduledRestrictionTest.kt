package com.preet.detoxlauncher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduledRestrictionTest {

    private fun isWithinSchedule(
        currentMinuteOfDay: Int,
        startHour: Int,
        startMin: Int,
        endHour: Int,
        endMin: Int
    ): Boolean {
        val startTotal = startHour * 60 + startMin
        val endTotal = endHour * 60 + endMin

        return if (startTotal <= endTotal) {
            currentMinuteOfDay in startTotal..endTotal
        } else {
            // Crosses midnight (e.g., 22:00 to 07:00)
            currentMinuteOfDay >= startTotal || currentMinuteOfDay <= endTotal
        }
    }

    @Test
    fun testDaytimeScheduleWindow() {
        val startHour = 9
        val startMin = 0
        val endHour = 17
        val endMin = 0

        val timeAt12PM = 12 * 60
        val timeAt8AM = 8 * 60
        val timeAt6PM = 18 * 60

        assertTrue("12:00 PM is inside 9:00 - 17:00", isWithinSchedule(timeAt12PM, startHour, startMin, endHour, endMin))
        assertFalse("8:00 AM is outside 9:00 - 17:00", isWithinSchedule(timeAt8AM, startHour, startMin, endHour, endMin))
        assertFalse("6:00 PM is outside 9:00 - 17:00", isWithinSchedule(timeAt6PM, startHour, startMin, endHour, endMin))
    }

    @Test
    fun testOvernightScheduleWindowCrossingMidnight() {
        val startHour = 22 // 10:00 PM
        val startMin = 0
        val endHour = 7    // 07:00 AM
        val endMin = 0

        val timeAt11PM = 23 * 60
        val timeAt3AM = 3 * 60
        val timeAt12PM = 12 * 60

        assertTrue("23:00 is inside overnight window 22:00 - 07:00", isWithinSchedule(timeAt11PM, startHour, startMin, endHour, endMin))
        assertTrue("03:00 is inside overnight window 22:00 - 07:00", isWithinSchedule(timeAt3AM, startHour, startMin, endHour, endMin))
        assertFalse("12:00 is outside overnight window 22:00 - 07:00", isWithinSchedule(timeAt12PM, startHour, startMin, endHour, endMin))
    }
}
