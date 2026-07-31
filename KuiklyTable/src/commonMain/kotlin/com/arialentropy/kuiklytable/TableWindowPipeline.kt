package com.arialentropy.kuiklytable

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Result of a Windowed slice computation over a fixed-height row list. */
internal data class TableWindowMetrics(
    val start: Int,
    val count: Int,
    val topSpacer: Float,
    val bottomSpacer: Float,
)

/**
 * Pure window math for [TableRowRenderMode.Windowed].
 *
 * Keeps roughly one third of the window as a leading buffer (same sizing rule as the former
 * `vforLazy` path). Spacers preserve total scroll height while only [count] rows are mounted.
 */
internal object TableWindowPipeline {

    fun computeWindow(
        scrollY: Float,
        viewportHeight: Float,
        totalRows: Int,
        rowStride: Float,
        maxRenderedRows: Int,
        contentLeading: Float = 0f,
    ): TableWindowMetrics {
        if (totalRows <= 0 || rowStride <= 0f || maxRenderedRows <= 0) {
            return TableWindowMetrics(start = 0, count = 0, topSpacer = 0f, bottomSpacer = 0f)
        }

        val visibleCount = max(1, ceil(viewportHeight.coerceAtLeast(0f) / rowStride).toInt())
        val windowSize = max(maxRenderedRows, visibleCount).coerceAtMost(totalRows)
        val totalHeight = totalRows * rowStride
        val lead = windowSize / 3
        val effectiveScrollY = max(0f, scrollY - contentLeading.coerceAtLeast(0f))
        val rawStart = floor(effectiveScrollY / rowStride).toInt() - lead
        val maxStart = (totalRows - windowSize).coerceAtLeast(0)
        val start = rawStart.coerceIn(0, maxStart)
        val topSpacer = start * rowStride
        val bottomSpacer = (totalHeight - topSpacer - windowSize * rowStride).coerceAtLeast(0f)
        return TableWindowMetrics(
            start = start,
            count = windowSize,
            topSpacer = topSpacer,
            bottomSpacer = bottomSpacer,
        )
    }
}
