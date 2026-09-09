package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.domain.AlarmWarning
import io.github.sourcem7.alfajralarm.domain.CapabilityProblem
import io.github.sourcem7.alfajralarm.domain.evaluateAlarmHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmHealthTest {
    @Test fun `complete configuration and capabilities are healthy`() {
        val health = evaluateAlarmHealth(testPreferences(), healthyCapabilities(), DAMASCUS.zoneId)

        assertTrue(health.isHealthy)
        assertEquals(emptyList<AlarmWarning>(), health.warnings)
    }

    @Test fun `each missing capability blocks activation`() {
        val cases = listOf(
            CapabilityProblem.EXACT_ALARMS_UNAVAILABLE to healthyCapabilities().copy(canScheduleExactAlarms = false),
            CapabilityProblem.NOTIFICATIONS_DISABLED to healthyCapabilities().copy(notificationsEnabled = false),
            CapabilityProblem.NOTIFICATIONS_DISABLED to healthyCapabilities().copy(alarmChannelEnabled = false),
            CapabilityProblem.FULL_SCREEN_UNAVAILABLE to healthyCapabilities().copy(canUseFullScreenIntent = false),
        )

        cases.forEach { (problem, capabilities) ->
            val health = evaluateAlarmHealth(testPreferences(), capabilities, DAMASCUS.zoneId)
            assertEquals(listOf(problem), health.problems)
        }
    }

    @Test fun `a missing full-screen permission never blocks the test alarm`() {
        // The test alarm is how the user finds out the permission is missing, so
        // refusing it would hide the very problem it exists to reveal.
        val health = evaluateAlarmHealth(
            testPreferences(),
            healthyCapabilities().copy(canUseFullScreenIntent = false),
            DAMASCUS.zoneId,
        )

        assertNull(health.testAlarmBlocker)
        assertEquals(listOf(CapabilityProblem.FULL_SCREEN_UNAVAILABLE), health.testAlarmDegradations)
    }

    @Test fun `incomplete configuration never blocks the test alarm`() {
        // Onboarding offers the test before a location or method exists.
        val health = evaluateAlarmHealth(testPreferences(method = null), healthyCapabilities(), DAMASCUS.zoneId)

        assertNull(health.testAlarmBlocker)
        assertEquals(emptyList<CapabilityProblem>(), health.testAlarmDegradations)
    }

    @Test fun `capabilities the ringing path cannot do without block the test alarm`() {
        val cases = listOf(
            CapabilityProblem.EXACT_ALARMS_UNAVAILABLE to healthyCapabilities().copy(canScheduleExactAlarms = false),
            CapabilityProblem.NOTIFICATIONS_DISABLED to healthyCapabilities().copy(notificationsEnabled = false),
        )

        cases.forEach { (problem, capabilities) ->
            val health = evaluateAlarmHealth(testPreferences(), capabilities, DAMASCUS.zoneId)
            assertEquals(problem, health.testAlarmBlocker)
        }
    }

    @Test fun `incomplete configuration is a blocking problem`() {
        val health = evaluateAlarmHealth(testPreferences(method = null), healthyCapabilities(), DAMASCUS.zoneId)

        assertEquals(listOf(CapabilityProblem.CONFIGURATION_INCOMPLETE), health.problems)
    }

    @Test fun `muted and low alarm volume warn without blocking`() {
        val muted = evaluateAlarmHealth(testPreferences(), healthyCapabilities().copy(alarmVolume = 0), DAMASCUS.zoneId)
        val low = evaluateAlarmHealth(testPreferences(), healthyCapabilities().copy(alarmVolume = 1), DAMASCUS.zoneId)

        assertTrue(muted.isHealthy)
        assertEquals(listOf(AlarmWarning.ALARM_VOLUME_MUTED), muted.warnings)
        assertTrue(low.isHealthy)
        assertEquals(listOf(AlarmWarning.ALARM_VOLUME_LOW), low.warnings)
    }

    @Test fun `a device time zone that differs from the selected location warns`() {
        val health = evaluateAlarmHealth(testPreferences(), healthyCapabilities(), "Europe/London")

        assertTrue(health.isHealthy)
        assertTrue(health.warnings.contains(AlarmWarning.TIME_ZONE_MISMATCH))
        assertFalse(health.problems.contains(CapabilityProblem.EXACT_ALARMS_UNAVAILABLE))
    }
}
