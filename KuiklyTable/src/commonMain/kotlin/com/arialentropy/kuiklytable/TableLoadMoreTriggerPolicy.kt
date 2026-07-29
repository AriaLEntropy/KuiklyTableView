package com.arialentropy.kuiklytable

internal fun <T> shouldResetLoadMoreTrigger(
    previousRows: List<TableDisplayRow<T>>,
    nextRows: List<TableDisplayRow<T>>,
): Boolean = previousRows.size != nextRows.size ||
    previousRows.mapTo(mutableSetOf()) { it.key } != nextRows.mapTo(mutableSetOf()) { it.key }
