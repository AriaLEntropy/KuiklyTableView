package com.arialentropy.kuiklytable

/** Controls how many row DSL nodes a [TableView] mounts. */
sealed class TableRowRenderMode {
    /**
     * Creates one DSL row node for every item.
     *
     * The platform list still recycles native views, but DSL nodes grow with the data size.
     * Prefer [Windowed] for large data sets.
     */
    object Standard : TableRowRenderMode()

    /**
     * Renders through Kuikly's lazy loop, which mounts at most [maxRenderedRows] row nodes and
     * keeps the remaining scroll extent with head and tail placeholders.
     *
     * Sizing rule: the lazy loop keeps roughly one third of the window as leading buffer, so the
     * visible row count must not exceed two thirds of [maxRenderedRows]. Undersized windows leave
     * blank space while scrolling. Estimate as `visibleRows * 3`, where
     * `visibleRows = tableHeight / rowHeight`.
     *
     * Configure this when the Table is created. Runtime switching on the same mounted
     * [TableView], fixed columns, and variable-height rows are not supported.
     */
    data class Windowed(val maxRenderedRows: Int = DEFAULT_MAX_RENDERED_ROWS) : TableRowRenderMode() {
        init {
            require(maxRenderedRows > 0) { "maxRenderedRows must be greater than 0" }
        }
    }

    companion object {
        /** Covers about 20 visible rows at the 3x sizing rule, i.e. ~960dp of 48dp rows. */
        const val DEFAULT_MAX_RENDERED_ROWS = 60
    }
}
