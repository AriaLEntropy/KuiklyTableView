package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TableBorderTest {
    @Test
    fun none_hides_outer_and_column_lines() {
        assertFalse(TableBorderMode.None.hasVisibleLines())
    }

    @Test
    fun default_and_custom_show_column_lines() {
        assertTrue(TableBorderMode.Default.hasVisibleLines())
        assertTrue(TableBorderMode.Custom(0xFF123456).hasVisibleLines())
    }
}
