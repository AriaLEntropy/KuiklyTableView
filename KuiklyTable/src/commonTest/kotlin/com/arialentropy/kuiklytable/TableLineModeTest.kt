package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TableLineModeTest {
    private val theme = TableThemeColors()

    @Test
    fun none_closes_all_strokes() {
        val style = TableLineMode.None.resolve(theme)
        assertNull(style.outer)
        assertNull(style.header)
        assertNull(style.row)
        assertNull(style.column)
    }

    @Test
    fun horizontal_keeps_only_horizontal_family() {
        val style = TableLineMode.Horizontal.resolve(theme)
        assertNull(style.outer)
        assertNull(style.column)
        assertNotNull(style.header)
        assertNotNull(style.row)
        assertEquals(theme.gridLine, style.header?.color)
    }

    @Test
    fun grid_enables_full_mesh() {
        val style = TableLineMode.Grid.resolve(theme)
        assertNotNull(style.outer)
        assertNotNull(style.header)
        assertNotNull(style.row)
        assertNotNull(style.column)
    }

    @Test
    fun custom_honors_explicit_nulls_and_divider_override() {
        val themed = TableThemeColors(dividerColor = 0xFF112233)
        val hair = TableStroke(themed.effectiveDividerColor(), 2f)
        val style = TableLineMode.Custom(
            TableLineStyle(outer = hair, header = null, row = hair, column = null),
        ).resolve(themed)
        assertEquals(0xFF112233, style.outer?.color)
        assertEquals(2f, style.outer?.width)
        assertNull(style.header)
        assertNull(style.column)
        assertNotNull(style.row)
    }

    @Test
    fun column_divider_width_hides_on_last_or_null() {
        assertEquals(0f, null.columnDividerWidth(isLastColumn = false))
        assertEquals(0f, TableStroke(0xFF000000, 1f).columnDividerWidth(isLastColumn = true))
        assertEquals(1.5f, TableStroke(0xFF000000, 1.5f).columnDividerWidth(isLastColumn = false))
    }
}
