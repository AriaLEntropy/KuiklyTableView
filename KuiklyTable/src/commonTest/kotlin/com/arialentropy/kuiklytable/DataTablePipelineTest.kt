package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertEquals

class DataTablePipelineTest {
    private data class Row(val id: String, val name: String, val status: String, val age: Int)

    private val rows = listOf(
        Row("1", "Alice", "在职", 23),
        Row("2", "Bob", "休假", 26),
        Row("3", "Carol", "在职", 32),
        Row("4", "Dave", "离职", 28),
        Row("5", "Eve", "在职", 21),
    )
    private val columns = listOf(
        ColumnModel(key = "name", title = "Name", accessor = Row::name, sortable = true),
        ColumnModel(
            key = "age",
            title = "Age",
            accessor = { it.age.toString() },
            sortable = true,
            sortComparator = compareBy(Row::age),
        ),
        ColumnModel(key = "status", title = "Status", accessor = Row::status),
    )

    @Test
    fun filterThenSortThenPaginate() {
        val page = DataTablePipeline.buildPage(
            data = rows,
            rowKey = { it.id },
            columns = columns,
            sortState = TableSortState("age", TableSortDirection.Ascending),
            filterPredicate = { it.status == "在职" },
            enablePagination = true,
            pageIndex = 0,
            pageSize = 2,
        )
        assertEquals(3, page.filteredTotal)
        assertEquals(2, page.pageCount)
        assertEquals(listOf("5", "1"), page.pageKeys)
        assertEquals(listOf(21, 23), page.pageItems.map { it.age })
    }

    @Test
    fun pageIndexIsClamped() {
        val page = DataTablePipeline.buildPage(
            data = rows,
            rowKey = { it.id },
            columns = columns,
            sortState = TableSortState(),
            filterPredicate = null,
            enablePagination = true,
            pageIndex = 99,
            pageSize = 2,
        )
        assertEquals(3, page.pageCount)
        assertEquals(2, page.pageIndex)
        assertEquals(listOf("5"), page.pageKeys)
    }

    @Test
    fun paginationOffReturnsAllFilteredSorted() {
        val page = DataTablePipeline.buildPage(
            data = rows,
            rowKey = { it.id },
            columns = columns,
            sortState = TableSortState("age", TableSortDirection.Descending),
            filterPredicate = { it.status != "离职" },
            enablePagination = false,
            pageIndex = 2,
            pageSize = 1,
        )
        assertEquals(4, page.filteredTotal)
        assertEquals(0, page.pageIndex)
        assertEquals(1, page.pageCount)
        assertEquals(listOf("3", "2", "1", "5"), page.pageKeys)
    }

    @Test
    fun emptyFilterStillHasOneLogicalPage() {
        val page = DataTablePipeline.buildPage(
            data = rows,
            rowKey = { it.id },
            columns = columns,
            sortState = TableSortState(),
            filterPredicate = { false },
            enablePagination = true,
            pageIndex = 0,
            pageSize = 10,
        )
        assertEquals(0, page.filteredTotal)
        assertEquals(1, page.pageCount)
        assertEquals(0, page.pageIndex)
        assertEquals(emptyList(), page.pageItems)
    }
}
