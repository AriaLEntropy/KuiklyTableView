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
     * Mounts at most [maxRenderedRows] row nodes via a self-managed window (slice + spacers).
     * Full [TableAttr.data] / display rows remain in memory; only the UI subtree is windowed.
     *
     * Sizing rule: keep roughly one third of the window as leading buffer, so the visible row
     * count must not exceed two thirds of [maxRenderedRows]. Undersized windows leave blank space
     * while scrolling. Estimate as `visibleRows * 3`, where `visibleRows = tableHeight / rowHeight`.
     *
     * Requires fixed row height (`rowHeight > 0`). Configure when the Table is created. Runtime
     * switching on the same mounted [TableView], fixed columns, and variable-height rows are not
     * supported.
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
