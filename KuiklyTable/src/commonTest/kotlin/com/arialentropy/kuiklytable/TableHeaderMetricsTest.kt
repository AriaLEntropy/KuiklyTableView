package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertEquals

class TableHeaderMetricsTest {
    @Test
    fun resolvedHeightUsesExplicitStyleHeight() {
        assertEquals(40f, TableHeaderMetrics.resolvedHeight(TableHeaderStyle(height = 40f)))
    }

    @Test
    fun resolvedHeightFallsBackToDefaultWhenZero() {
        assertEquals(TableHeaderMetrics.DEFAULT_HEIGHT, TableHeaderMetrics.resolvedHeight(TableHeaderStyle()))
    }

    @Test
    fun blockHeightIncludesHeaderDivider() {
        val style = TableHeaderStyle(height = 40f)
        val theme = TableThemeColors.Light
        val grid = TableHeaderMetrics.blockHeight(style, TableLineMode.Grid, theme)
        val none = TableHeaderMetrics.blockHeight(style, TableLineMode.None, theme)
        assertEquals(40f, none)
        assertEquals(41f, grid) // Grid header stroke width 1f
    }
}
