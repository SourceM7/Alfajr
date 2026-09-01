package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.domain.AlarmKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmIdentityTest {
    @Test fun `a test or snooze alarm cannot replace the daily pending intent`() {
        // Android matches pending intents by action, data, and request code and
        // ignores extras, so both must differ for every kind.
        val actions = AlarmKind.entries.map(AlarmIntents::action)
        val requestCodes = AlarmKind.entries.map(AlarmIntents::requestCode)

        assertEquals(AlarmKind.entries.size, actions.distinct().size)
        assertEquals(AlarmKind.entries.size, requestCodes.distinct().size)
    }

    @Test fun `only known alarm actions resolve to a kind`() {
        AlarmKind.entries.forEach { kind ->
            assertEquals(kind, AlarmIntents.kindOf(AlarmIntents.action(kind)))
        }
        assertNull(AlarmIntents.kindOf("android.intent.action.BOOT_COMPLETED"))
        assertNull(AlarmIntents.kindOf(null))
    }
}
