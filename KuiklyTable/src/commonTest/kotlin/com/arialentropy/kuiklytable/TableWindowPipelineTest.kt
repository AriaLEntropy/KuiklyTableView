package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertEquals

class TableWindowPipelineTest {

    @Test
    fun emptyTableYieldsEmptyWindow() {
        val metrics = TableWindowPipeline.computeWindow(
            scrollY = 100f,
            viewportHeight = 480f,
            totalRows = 0,
            rowStride = 49f,
            maxRenderedRows = 60,
        )
        assertEquals(TableWindowMetrics(0, 0, 0f, 0f), metrics)
    }

    @Test
    fun fewerRowsThanWindowMountsAll() {
        val metrics = TableWindowPipeline.computeWindow(
            scrollY = 0f,
            viewportHeight = 480f,
            totalRows = 10,
            rowStride = 49f,
            maxRenderedRows = 60,
        )
        assertEquals(0, metrics.start)
        assertEquals(10, metrics.count)
        assertEquals(0f, metrics.topSpacer)
        assertEquals(0f, metrics.bottomSpacer)
    }

    @Test
    fun topOfListUsesLeadingBufferClamp() {
        // visible ≈ 10, window 60 → windowSize 60; lead 20; rawStart = 0 - 20 → clamp 0
        val metrics = TableWindowPipeline.computeWindow(
            scrollY = 0f,
            viewportHeight = 490f,
            totalRows = 3000,
            rowStride = 49f,
            maxRenderedRows = 60,
        )
        assertEquals(0, metrics.start)
        assertEquals(60, metrics.count)
        assertEquals(0f, metrics.topSpacer)
        assertEquals((3000 - 60) * 49f, metrics.bottomSpacer)
    }

    @Test
    fun midScrollAppliesLeadingBuffer() {
        val stride = 49f
        val windowSize = 60
        val lead = windowSize / 3 // 20
        val scrollY = 100 * stride
        val metrics = TableWindowPipeline.computeWindow(
            scrollY = scrollY,
            viewportHeight = 490f,
            totalRows = 3000,
            rowStride = stride,
            maxRenderedRows = windowSize,
        )
        assertEquals(100 - lead, metrics.start)
        assertEquals(windowSize, metrics.count)
        assertEquals((100 - lead) * stride, metrics.topSpacer)
        assertEquals((3000 - (100 - lead) - windowSize) * stride, metrics.bottomSpacer)
    }

    @Test
    fun bottomClampKeepsWindowInsideBounds() {
        val stride = 49f
        val windowSize = 60
        val total = 3000
        val metrics = TableWindowPipeline.computeWindow(
            scrollY = total * stride,
            viewportHeight = 490f,
            totalRows = total,
            rowStride = stride,
            maxRenderedRows = windowSize,
        )
        assertEquals(total - windowSize, metrics.start)
        assertEquals(windowSize, metrics.count)
        assertEquals((total - windowSize) * stride, metrics.topSpacer)
        assertEquals(0f, metrics.bottomSpacer)
    }

    @Test
    fun contentLeadingShiftsEffectiveScroll() {
        val stride = 49f
        val metrics = TableWindowPipeline.computeWindow(
            scrollY = 80f + 50 * stride,
            viewportHeight = 490f,
            totalRows = 3000,
            rowStride = stride,
            maxRenderedRows = 60,
            contentLeading = 80f,
        )
        val withoutLeading = TableWindowPipeline.computeWindow(
            scrollY = 50 * stride,
            viewportHeight = 490f,
            totalRows = 3000,
            rowStride = stride,
            maxRenderedRows = 60,
        )
        assertEquals(withoutLeading, metrics)
    }

    @Test
    fun invalidStrideYieldsEmptyWindow() {
        val metrics = TableWindowPipeline.computeWindow(
            scrollY = 0f,
            viewportHeight = 480f,
            totalRows = 100,
            rowStride = 0f,
            maxRenderedRows = 60,
        )
        assertEquals(TableWindowMetrics(0, 0, 0f, 0f), metrics)
    }
}
