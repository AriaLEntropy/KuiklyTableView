package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertEquals

class DataTableSelectionTest {
    @Test
    fun selectAllStateNonePartialAll() {
        val visible = listOf("a", "b", "c")
        assertEquals(
            DataTableSelectAllState.None,
            DataTableSelection.selectAllState(visible, emptyList()),
        )
        assertEquals(
            DataTableSelectAllState.Partial,
            DataTableSelection.selectAllState(visible, listOf("b")),
        )
        assertEquals(
            DataTableSelectAllState.All,
            DataTableSelection.selectAllState(visible, listOf("c", "a", "b")),
        )
    }

    @Test
    fun toggleKeyAddsAndRemoves() {
        assertEquals(listOf("a"), DataTableSelection.toggleKey(emptyList(), "a"))
        assertEquals(listOf("a", "b"), DataTableSelection.toggleKey(listOf("a"), "b"))
        assertEquals(listOf("a"), DataTableSelection.toggleKey(listOf("a", "b"), "b"))
    }

    @Test
    fun toggleSelectAllFromNoneAndPartialSelectsVisible() {
        val visible = listOf("a", "b", "c")
        assertEquals(
            listOf("a", "b", "c"),
            DataTableSelection.toggleSelectAll(visible, emptyList()),
        )
        assertEquals(
            listOf("x", "a", "b", "c"),
            DataTableSelection.toggleSelectAll(visible, listOf("x", "b")),
        )
    }

    @Test
    fun toggleSelectAllFromAllClearsOnlyVisible() {
        val visible = listOf("a", "b")
        assertEquals(
            listOf("x"),
            DataTableSelection.toggleSelectAll(visible, listOf("x", "a", "b")),
        )
    }

    @Test
    fun sortDoesNotChangeSelectedKeysIdentity() {
        val rows = listOf(
            TestRow(id = "c", name = "Charlie", age = 32),
            TestRow(id = "a", name = "Alice", age = 23),
            TestRow(id = "b", name = "Bob", age = 26),
        )
        val columns = listOf(
            ColumnModel(
                key = "age",
                title = "Age",
                accessor = { it.age.toString() },
                sortable = true,
                sortComparator = compareBy(TestRow::age),
            ),
        )
        val selected = listOf<Any>("c", "a")
        val before = DataTableSelection.visibleKeys(
            data = rows,
            rowKey = { it.id },
            columns = columns,
            sortState = TableSortState(),
        )
        val after = DataTableSelection.visibleKeys(
            data = rows,
            rowKey = { it.id },
            columns = columns,
            sortState = TableSortState("age", TableSortDirection.Ascending),
        )
        assertEquals(listOf("c", "a", "b"), before)
        assertEquals(listOf("a", "b", "c"), after)
        assertEquals(
            DataTableSelectAllState.Partial,
            DataTableSelection.selectAllState(after, selected),
        )
        assertEquals(true, DataTableSelection.isSelected(selected, "c"))
        assertEquals(true, DataTableSelection.isSelected(selected, "a"))
    }

    private data class TestRow(val id: String, val name: String, val age: Int)
}
