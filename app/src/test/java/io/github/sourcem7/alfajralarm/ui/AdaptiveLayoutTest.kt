package io.github.sourcem7.alfajralarm.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun `expanded width uses two panes when height is available`() {
        assertTrue(AppWindowLayout(expandedWidth = true, compactHeight = false).showTwoPanes)
    }

    @Test
    fun `compact height keeps content in a single pane`() {
        assertFalse(AppWindowLayout(expandedWidth = true, compactHeight = true).showTwoPanes)
    }

    @Test
    fun `compact width keeps content in a single pane`() {
        assertFalse(AppWindowLayout(expandedWidth = false, compactHeight = false).showTwoPanes)
    }
}
