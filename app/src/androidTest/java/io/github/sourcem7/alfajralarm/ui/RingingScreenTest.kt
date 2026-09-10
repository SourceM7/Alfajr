package io.github.sourcem7.alfajralarm.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.github.sourcem7.alfajralarm.R
import io.github.sourcem7.alfajralarm.domain.RingingSession
import io.github.sourcem7.alfajralarm.domain.RingtoneSource
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RingingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ringingScreenShowsPrimaryActionsAndSupportsAccessibleDismiss() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var dismissed = false
        val session = RingingSession(
            sessionId = "ui-test",
            isTest = true,
            alarmAt = Instant.fromEpochMilliseconds(0),
            startedAt = Instant.fromEpochMilliseconds(0),
            snoozesUsed = 0,
            snoozeMinutes = 5,
            tapToDismiss = false,
            vibrating = false,
            ringtoneSource = RingtoneSource.SYSTEM_ALARM,
        )

        composeRule.setContent {
            RingingScreen(
                session = session,
                onSnooze = {},
                onDismiss = { dismissed = true },
            )
        }

        val snoozeLabel = context.resources.getQuantityString(
            R.plurals.action_snooze_minutes,
            session.snoozeMinutes,
            session.snoozeMinutes,
        )
        composeRule.onNodeWithText(snoozeLabel).assertIsDisplayed()

        val dismissLabel = context.getString(R.string.action_dismiss)
        val dismissHint = context.getString(R.string.ringing_hold_to_dismiss)
        composeRule.onNodeWithContentDescription("$dismissLabel. $dismissHint")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun ringingScreenUsesItsParentMaterialColorScheme() {
        val background = Color(0xFF7A1E70)
        val session = RingingSession(
            sessionId = "theme-test",
            isTest = true,
            alarmAt = Instant.fromEpochMilliseconds(0),
            startedAt = Instant.fromEpochMilliseconds(0),
            snoozesUsed = 0,
            snoozeMinutes = 5,
            tapToDismiss = false,
            vibrating = false,
            ringtoneSource = RingtoneSource.SYSTEM_ALARM,
        )

        composeRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme(background = background)) {
                RingingScreen(session = session, onSnooze = {}, onDismiss = {})
            }
        }

        val renderedBackground = composeRule.onNodeWithTag("ringing_surface")
            .captureToImage()
            .toPixelMap()[0, 0]
        assertEquals(background, renderedBackground)
    }
}
