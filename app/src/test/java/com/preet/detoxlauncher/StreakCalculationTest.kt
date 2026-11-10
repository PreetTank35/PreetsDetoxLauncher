package com.preet.detoxlauncher

import com.preet.detoxlauncher.data.local.entity.DetoxStreakEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculationTest {

    @Test
    fun testConsecutiveStreaksCalculation() {
        // Simulating consecutive day results
        val records = listOf(
            DetoxStreakEntity("2026-11-08", goalMinutes = 120, actualMinutes = 95, goalMet = true),
            DetoxStreakEntity("2026-11-09", goalMinutes = 120, actualMinutes = 110, goalMet = true),
            DetoxStreakEntity("2026-11-10", goalMinutes = 120, actualMinutes = 80, goalMet = true)
        )

        var streak = 0
        for (record in records.reversed()) {
            if (record.goalMet) {
                streak++
            } else {
                break
            }
        }

        assertEquals(3, streak)
    }

    @Test
    fun testStreakResetsWhenGoalExceeded() {
        val records = listOf(
            DetoxStreakEntity("2026-11-08", goalMinutes = 120, actualMinutes = 90, goalMet = true),
            DetoxStreakEntity("2026-11-09", goalMinutes = 120, actualMinutes = 145, goalMet = false), // Broken
            DetoxStreakEntity("2026-11-10", goalMinutes = 120, actualMinutes = 70, goalMet = true)
        )

        var currentStreak = 0
        for (record in records.reversed()) {
            if (record.goalMet) {
                currentStreak++
            } else {
                break
            }
        }

        assertEquals("Current streak should only count consecutive days up to the broken day", 1, currentStreak)
    }

    @Test
    fun testLongestStreakCalculation() {
        val records = listOf(
            DetoxStreakEntity("2026-11-01", 120, 100, true),
            DetoxStreakEntity("2026-11-02", 120, 90, true),
            DetoxStreakEntity("2026-11-03", 120, 80, true),
            DetoxStreakEntity("2026-11-04", 120, 150, false), // Break
            DetoxStreakEntity("2026-11-05", 120, 100, true),
            DetoxStreakEntity("2026-11-06", 120, 95, true)
        )

        var longest = 0
        var temp = 0
        for (r in records) {
            if (r.goalMet) {
                temp++
                if (temp > longest) longest = temp
            } else {
                temp = 0
            }
        }

        assertEquals(3, longest)
    }
}
