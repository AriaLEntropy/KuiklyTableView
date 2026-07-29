package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TableLoadMoreTriggerPolicyTest {
    @Test
    fun sortingTheSameRowsDoesNotUnlockLoadMore() {
        val previousRows = rows("a", "b", "c")
        val sortedRows = rows("c", "b", "a")

        assertFalse(shouldResetLoadMoreTrigger(previousRows, sortedRows))
    }

    @Test
    fun appendingRowsUnlocksLoadMore() {
        val previousRows = rows("a", "b")
        val appendedRows = rows("a", "b", "c")

        assertTrue(shouldResetLoadMoreTrigger(previousRows, appendedRows))
    }

    @Test
    fun replacingRowsUnlocksLoadMoreWhenCountIsUnchanged() {
        val previousRows = rows("a", "b")
        val replacementRows = rows("c", "d")

        assertTrue(shouldResetLoadMoreTrigger(previousRows, replacementRows))
    }

    private fun rows(vararg keys: String): List<TableDisplayRow<String>> = keys.mapIndexed { index, key ->
        TableDisplayRow(item = key, key = key, sourceIndex = index, displayIndex = index)
    }
}
