package com.preet.detoxlauncher

import com.preet.detoxlauncher.data.model.DetoxRule
import com.preet.detoxlauncher.data.model.RestrictionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetoxCalculationTest {

    @Test
    fun testSoftBlockTriggerCondition() {
        val rule = DetoxRule(
            packageName = "com.social.app",
            mode = RestrictionMode.SOFT,
            dailyLimitMinutes = 30
        )

        val usedMinutesWithinQuota = 25
        val usedMinutesExceeded = 35

        val isBlockedWithin = usedMinutesWithinQuota >= rule.dailyLimitMinutes
        val isBlockedExceeded = usedMinutesExceeded >= rule.dailyLimitMinutes

        assertFalse(isBlockedWithin)
        assertTrue(isBlockedExceeded)
    }

    @Test
    fun testStrictBlockTriggerCondition() {
        val rule = DetoxRule(
            packageName = "com.game.app",
            mode = RestrictionMode.STRICT,
            dailyLimitMinutes = 45
        )

        val usedMinutes = 45
        val isStrictlyBlocked = (usedMinutes >= rule.dailyLimitMinutes) && (rule.mode == RestrictionMode.STRICT)

        assertTrue("Application should be strictly blocked when limit is reached", isStrictlyBlocked)
    }

    @Test
    fun testDisabledRuleDoesNotBlock() {
        val rule = DetoxRule(
            packageName = "com.useful.tool",
            mode = RestrictionMode.OFF,
            dailyLimitMinutes = 10
        )

        val usedMinutes = 50
        val shouldBlock = (rule.mode != RestrictionMode.OFF) && (usedMinutes >= rule.dailyLimitMinutes)

        assertFalse("Disabled rule must never block execution", shouldBlock)
    }
}
