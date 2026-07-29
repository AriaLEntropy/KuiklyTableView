package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TableCellClickPolicyTest {
    private data class Row(val name: String)

    private val row = Row("Alice")
    private val overflowInfo = TableOverflowCellInfo(
        rowIndex = 0,
        columnIndex = 1,
        columnKey = "email",
        rowData = row,
        text = "very-long@example.com",
        isOverflow = true,
        estimatedCellX = 0f,
        estimatedCellY = 0f,
        estimatedCellWidth = 100f,
        estimatedCellHeight = 48f,
    )
    private val cellInfo = TableCellClickInfo(
        rowIndex = 0,
        columnIndex = 0,
        columnKey = "name",
        rowData = row,
    )

    @Test
    fun onlyRowClick_firesRow() {
        val action = resolveTableCellClickAction(
            enableRowClick = true,
            enableCellClick = false,
            isTruncated = false,
            hasOverflowListener = true,
            hasCellClickListener = true,
            hasRowClickListener = true,
            overflowInfo = overflowInfo,
            cellInfo = cellInfo,
            rowData = row,
        )
        assertIs<TableCellClickAction.Row<Row>>(action)
        assertEquals(row, action.rowData)
    }

    @Test
    fun onlyCellClick_firesCell() {
        val action = resolveTableCellClickAction(
            enableRowClick = false,
            enableCellClick = true,
            isTruncated = false,
            hasOverflowListener = true,
            hasCellClickListener = true,
            hasRowClickListener = true,
            overflowInfo = overflowInfo,
            cellInfo = cellInfo,
            rowData = row,
        )
        assertIs<TableCellClickAction.Cell<Row>>(action)
        assertEquals("name", action.info.columnKey)
    }

    @Test
    fun bothEnabled_prefersCellOverRow() {
        val action = resolveTableCellClickAction(
            enableRowClick = true,
            enableCellClick = true,
            isTruncated = false,
            hasOverflowListener = true,
            hasCellClickListener = true,
            hasRowClickListener = true,
            overflowInfo = overflowInfo,
            cellInfo = cellInfo,
            rowData = row,
        )
        assertIs<TableCellClickAction.Cell<Row>>(action)
    }

    @Test
    fun truncatedWithOverflowListener_prefersOverflow() {
        val action = resolveTableCellClickAction(
            enableRowClick = true,
            enableCellClick = true,
            isTruncated = true,
            hasOverflowListener = true,
            hasCellClickListener = true,
            hasRowClickListener = true,
            overflowInfo = overflowInfo,
            cellInfo = cellInfo,
            rowData = row,
        )
        assertIs<TableCellClickAction.Overflow<Row>>(action)
    }

    @Test
    fun bothSwitchesOff_firesNone() {
        val action = resolveTableCellClickAction(
            enableRowClick = false,
            enableCellClick = false,
            isTruncated = false,
            hasOverflowListener = true,
            hasCellClickListener = true,
            hasRowClickListener = true,
            overflowInfo = overflowInfo,
            cellInfo = cellInfo,
            rowData = row,
        )
        assertIs<TableCellClickAction.None<Row>>(action)
    }

    @Test
    fun cellEnabledWithoutListener_fallsBackToRow() {
        val action = resolveTableCellClickAction(
            enableRowClick = true,
            enableCellClick = true,
            isTruncated = false,
            hasOverflowListener = false,
            hasCellClickListener = false,
            hasRowClickListener = true,
            overflowInfo = overflowInfo,
            cellInfo = cellInfo,
            rowData = row,
        )
        assertIs<TableCellClickAction.Row<Row>>(action)
    }

    @Test
    fun columnDefaults_matchPlan() {
        val column = ColumnModel(
            key = "name",
            title = "姓名",
            accessor = { row: Row -> row.name },
        )
        assertTrue(column.enableRowClick)
        assertTrue(!column.enableCellClick)
    }
}
