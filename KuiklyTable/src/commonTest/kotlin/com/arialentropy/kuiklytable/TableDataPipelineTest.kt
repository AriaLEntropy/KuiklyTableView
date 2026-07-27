package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableDataPipelineTest {
    private val rows = listOf(
        TestRow(id = "c", name = "Charlie", age = 32),
        TestRow(id = "a", name = "Alice", age = 23),
        TestRow(id = "b", name = "Bob", age = 26),
    )
    private val nameColumn = ColumnModel(
        key = "name",
        title = "Name",
        accessor = TestRow::name,
        sortable = true,
    )
    private val ageColumn = ColumnModel(
        key = "age",
        title = "Age",
        accessor = { it.age.toString() },
        sortable = true,
        sortComparator = compareBy(TestRow::age),
    )

    @Test
    fun noSortPreservesSourceOrderAndStableKeys() {
        // Given
        val source = rows.toList()

        // When
        val displayRows = buildRows(
            data = source,
            sortState = TableSortState(),
        )

        // Then
        assertEquals(listOf("c", "a", "b"), displayRows.map { it.key })
        assertEquals(listOf(0, 1, 2), displayRows.map { it.sourceIndex })
        assertEquals(listOf(0, 1, 2), displayRows.map { it.displayIndex })
        assertEquals(rows, source)
    }

    @Test
    fun ascendingSortUsesCustomComparatorWithoutChangingSource() {
        // Given
        val source = rows.toList()

        // When
        val displayRows = buildRows(
            data = source,
            sortState = TableSortState("age", TableSortDirection.Ascending),
        )

        // Then
        assertEquals(listOf(23, 26, 32), displayRows.map { it.item.age })
        assertEquals(listOf(1, 2, 0), displayRows.map { it.sourceIndex })
        assertEquals(listOf(0, 1, 2), displayRows.map { it.displayIndex })
        assertEquals(rows, source)
    }

    @Test
    fun descendingSortUsesCustomComparator() {
        // Given
        val sortState = TableSortState("age", TableSortDirection.Descending)

        // When
        val displayRows = buildRows(rows, sortState)

        // Then
        assertEquals(listOf(32, 26, 23), displayRows.map { it.item.age })
        assertEquals(listOf("c", "b", "a"), displayRows.map { it.key })
    }

    @Test
    fun noneDirectionRestoresSourceOrderAfterSorting() {
        // Given
        buildRows(rows, TableSortState("age", TableSortDirection.Ascending))

        // When
        val displayRows = buildRows(rows, TableSortState("age", TableSortDirection.None))

        // Then
        assertEquals(rows, displayRows.map { it.item })
        assertEquals(listOf(0, 1, 2), displayRows.map { it.sourceIndex })
    }

    @Test
    fun sortableColumnFallsBackToAccessorStringComparison() {
        // Given
        val sortState = TableSortState("name", TableSortDirection.Ascending)

        // When
        val displayRows = buildRows(rows, sortState)

        // Then
        assertEquals(listOf("Alice", "Bob", "Charlie"), displayRows.map { it.item.name })
    }

    @Test
    fun nonSortableColumnLeavesRowsInSourceOrder() {
        // Given
        val columns = listOf(
            ColumnModel(
                key = "name",
                title = "Name",
                accessor = TestRow::name,
                sortable = false,
            ),
        )

        // When
        val displayRows = TableDataPipeline.buildDisplayRows(
            data = rows,
            rowKey = TestRow::id,
            columns = columns,
            sortState = TableSortState("name", TableSortDirection.Ascending),
        )

        // Then
        assertEquals(rows, displayRows.map { it.item })
    }

    @Test
    fun missingSortColumnLeavesRowsInSourceOrder() {
        // Given
        val sortState = TableSortState("missing", TableSortDirection.Ascending)

        // When
        val displayRows = buildRows(rows, sortState)

        // Then
        assertEquals(rows, displayRows.map { it.item })
    }

    @Test
    fun indexFallbackKeepsSourceIdentityAfterSorting() {
        // Given
        val sortState = TableSortState("age", TableSortDirection.Ascending)

        // When
        val displayRows = TableDataPipeline.buildDisplayRows(
            data = rows,
            rowKey = null,
            columns = listOf(nameColumn, ageColumn),
            sortState = sortState,
        )

        // Then
        assertEquals(listOf(1, 2, 0), displayRows.map { it.key })
        assertEquals(listOf(1, 2, 0), displayRows.map { it.sourceIndex })
    }

    @Test
    fun emptyDataProducesNoDisplayRows() {
        // Given
        val data = emptyList<TestRow>()

        // When
        val displayRows = buildRows(data, TableSortState("age", TableSortDirection.Ascending))

        // Then
        assertTrue(displayRows.isEmpty())
    }

    @Test
    fun singleRowKeepsItsSourceAndDisplayIndexes() {
        // Given
        val data = listOf(TestRow(id = "only", name = "Only", age = 1))

        // When
        val displayRow = buildRows(data, TableSortState("age", TableSortDirection.Descending)).single()

        // Then
        assertEquals("only", displayRow.key)
        assertEquals(0, displayRow.sourceIndex)
        assertEquals(0, displayRow.displayIndex)
    }

    private fun buildRows(
        data: List<TestRow>,
        sortState: TableSortState,
    ): List<TableDisplayRow<TestRow>> = TableDataPipeline.buildDisplayRows(
        data = data,
        rowKey = TestRow::id,
        columns = listOf(nameColumn, ageColumn),
        sortState = sortState,
    )

    private data class TestRow(
        val id: String,
        val name: String,
        val age: Int,
    )
}
