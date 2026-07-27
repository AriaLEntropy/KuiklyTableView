package com.arialentropy.kuiklytable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TableColumnLayoutResolverTest {
    @Test
    fun singleColumnFillsViewportWhenViewportIsAvailable() {
        // Given
        val column = column(key = "name", width = 180f, minWidth = 120f, flex = 0f)

        // When
        val layout = TableColumnLayoutResolver.resolve(
            columns = listOf(column),
            viewportWidth = 360f,
            autoIndexColumn = false,
            indexColumnWidth = 56f,
            fixedColumnCount = 0,
        )

        // Then
        assertEquals(360f, layout.contentWidth)
        assertEquals(360f, layout.all.single().width)
        assertEquals(0f, layout.all.single().x)
    }

    @Test
    fun singleColumnUsesConfiguredWidthWhenViewportIsUnavailable() {
        // Given
        val column = column(key = "name", width = 180f, minWidth = 120f)

        // When
        val layout = TableColumnLayoutResolver.resolve(
            columns = listOf(column),
            viewportWidth = 0f,
            autoIndexColumn = false,
            indexColumnWidth = 56f,
            fixedColumnCount = 0,
        )

        // Then
        assertEquals(180f, layout.contentWidth)
        assertEquals(180f, layout.all.single().width)
    }

    @Test
    fun fixedAndFlexColumnsShareRemainingViewportByWeight() {
        // Given
        val columns = listOf(
            column(key = "fixed", width = 100f),
            column(key = "flexOne", minWidth = 50f, flex = 1f),
            column(key = "flexTwo", minWidth = 50f, flex = 2f),
        )

        // When
        val layout = TableColumnLayoutResolver.resolve(
            columns = columns,
            viewportWidth = 400f,
            autoIndexColumn = false,
            indexColumnWidth = 56f,
            fixedColumnCount = 0,
        )

        // Then
        assertEquals(400f, layout.contentWidth, 0.001f)
        assertEquals(100f, layout.all[0].width, 0.001f)
        assertEquals(116.666f, layout.all[1].width, 0.001f)
        assertEquals(183.333f, layout.all[2].width, 0.001f)
        assertEquals(216.666f, layout.all[2].x, 0.001f)
    }

    @Test
    fun minimumWidthsRemainWhenNaturalWidthExceedsViewport() {
        // Given
        val columns = listOf(
            column(key = "fixed", width = 100f),
            column(key = "first", minWidth = 150f, flex = 1f),
            column(key = "second", minWidth = 150f, flex = 1f),
        )

        // When
        val layout = TableColumnLayoutResolver.resolve(
            columns = columns,
            viewportWidth = 300f,
            autoIndexColumn = false,
            indexColumnWidth = 56f,
            fixedColumnCount = 0,
        )

        // Then
        assertEquals(400f, layout.contentWidth)
        assertEquals(listOf(100f, 150f, 150f), layout.all.map { it.width })
    }

    @Test
    fun nonPositiveFlexValuesDoNotReceiveRemainingWidth() {
        // Given
        val columns = listOf(
            column(key = "zero", minWidth = 80f, flex = 0f),
            column(key = "negative", minWidth = 120f, flex = -2f),
        )

        // When
        val layout = TableColumnLayoutResolver.resolve(
            columns = columns,
            viewportWidth = 400f,
            autoIndexColumn = false,
            indexColumnWidth = 56f,
            fixedColumnCount = 0,
        )

        // Then
        assertEquals(200f, layout.contentWidth)
        assertEquals(listOf(80f, 120f), layout.all.map { it.width })
    }

    @Test
    fun negativeMinimumWidthIsClampedToZero() {
        // Given
        val columns = listOf(
            column(key = "negative", minWidth = -40f, flex = 0f),
            column(key = "positive", minWidth = 100f, flex = 0f),
        )

        // When
        val layout = TableColumnLayoutResolver.resolve(
            columns = columns,
            viewportWidth = 80f,
            autoIndexColumn = false,
            indexColumnWidth = 56f,
            fixedColumnCount = 0,
        )

        // Then
        assertEquals(listOf(0f, 100f), layout.all.map { it.width })
        assertEquals(100f, layout.contentWidth)
    }

    @Test
    fun generatedIndexParticipatesInLayoutAndFixedPartition() {
        // Given
        val columns = listOf(
            column(key = "name", width = 100f),
            column(key = "age", width = 80f),
        )

        // When
        val layout = TableColumnLayoutResolver.resolve(
            columns = columns,
            viewportWidth = 236f,
            autoIndexColumn = true,
            indexColumnWidth = 56f,
            fixedColumnCount = 2,
        )

        // Then
        assertEquals(236f, layout.contentWidth)
        assertEquals(156f, layout.fixedWidth)
        assertEquals(2, layout.fixed.size)
        assertEquals(1, layout.scrollable.size)
        assertTrue(layout.all[0].isGeneratedIndex)
        assertNull(layout.all[0].model)
        assertEquals(56f, layout.all[1].x)
        assertFalse(layout.all[1].isGeneratedIndex)
    }

    @Test
    fun naturalWidthUsesConfiguredWidthsAndClampedMinimums() {
        // Given
        val columns = listOf(
            column(key = "fixed", width = 90f),
            column(key = "flex", minWidth = 110f),
            column(key = "negative", minWidth = -20f),
        )

        // When
        val width = TableColumnLayoutResolver.naturalWidth(
            columns = columns,
            autoIndexColumn = true,
            indexColumnWidth = 56f,
        )

        // Then
        assertEquals(256f, width)
    }

    private fun column(
        key: String,
        width: Float? = null,
        minWidth: Float = ColumnModel.DEFAULT_MIN_WIDTH,
        flex: Float = 1f,
    ): ColumnModel<String> = ColumnModel(
        key = key,
        title = key,
        accessor = { it },
        width = width,
        minWidth = minWidth,
        flex = flex,
    )
}
