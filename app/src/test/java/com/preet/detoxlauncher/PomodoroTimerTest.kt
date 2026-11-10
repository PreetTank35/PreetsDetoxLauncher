package com.preet.detoxlauncher

import com.preet.detoxlauncher.data.model.FocusSessionType
import com.preet.detoxlauncher.data.model.PomodoroState
import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroTimerTest {

    @Test
    fun testFormattedRemainingTime() {
        val stateAtStart = PomodoroState(
            totalSeconds = 25 * 60,
            remainingSeconds = 25 * 60
        )
        assertEquals("25:00", stateAtStart.formattedRemainingTime)

        val stateMidway = PomodoroState(
            totalSeconds = 25 * 60,
            remainingSeconds = 4 * 60 + 5 // 4 mins 5 secs
        )
        assertEquals("04:05", stateMidway.formattedRemainingTime)
    }

    @Test
    fun testProgressCalculation() {
        val stateAtHalfway = PomodoroState(
            totalSeconds = 100,
            remainingSeconds = 50
        )
        assertEquals(0.5f, stateAtHalfway.progress, 0.001f)

        val stateAtEnd = PomodoroState(
            totalSeconds = 100,
            remainingSeconds = 0
        )
        assertEquals(1.0f, stateAtEnd.progress, 0.001f)
    }
}
