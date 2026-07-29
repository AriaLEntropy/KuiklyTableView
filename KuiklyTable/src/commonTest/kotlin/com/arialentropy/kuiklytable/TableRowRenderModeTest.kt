package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class TableRowRenderModeTest {
    @Test
    fun tableAttrDefaultsToStandardRendering() {
        assertIs<TableRowRenderMode.Standard>(TableAttr<Any>().rowRenderMode)
    }

    @Test
    fun windowedRenderingUsesBoundedDefault() {
        val mode = TableRowRenderMode.Windowed()

        assertEquals(TableRowRenderMode.DEFAULT_MAX_RENDERED_ROWS, mode.maxRenderedRows)
    }

    @Test
    fun windowedRenderingRejectsNonPositiveBounds() {
        assertFailsWith<IllegalArgumentException> { TableRowRenderMode.Windowed(0) }
        assertFailsWith<IllegalArgumentException> { TableRowRenderMode.Windowed(-1) }
    }
}
