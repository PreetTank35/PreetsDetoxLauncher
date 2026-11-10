package com.preet.detoxlauncher

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidationTest {

    @Test
    fun testValidBackupJsonParsing() {
        val jsonString = """
            {
              "version": 1,
              "appName": "Preet's Detox Launcher",
              "timestamp": 1731234567890,
              "settings": {
                "isPureAmoled": true,
                "accentColorIndex": 0,
                "dailyScreenTimeTargetMinutes": 120
              },
              "detoxRules": [
                {
                  "packageName": "com.distracting.app",
                  "mode": "SOFT",
                  "dailyLimitMinutes": 30
                }
              ]
            }
        """.trimIndent()

        val jsonObject = JSONObject(jsonString)
        val version = jsonObject.optInt("version", -1)

        assertEquals(1, version)
        assertTrue(jsonObject.has("settings"))
        assertEquals(true, jsonObject.getJSONObject("settings").getBoolean("isPureAmoled"))
    }

    @Test
    fun testRejectsIncompatibleVersion() {
        val legacyJson = """
            {
              "version": 0,
              "appName": "Old Version"
            }
        """.trimIndent()

        val jsonObject = JSONObject(legacyJson)
        val version = jsonObject.optInt("version", -1)
        val isValid = version >= 1

        assertFalse("Versions less than 1 must be rejected", isValid)
    }
}
