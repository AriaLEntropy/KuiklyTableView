package com.arialentropy.kuiklytable

/** Controls how many row DSL nodes a [TableView] mounts. */
sealed class TableRowRenderMode {
    /** Creates one DSL row node for every item. */
    object Standard : TableRowRenderMode()

    /**
     * Keeps at most [maxRenderedRows] row nodes mounted through Kuikly's lazy loop.
     *
     * The window must cover the viewport plus a scrolling buffer. Values smaller than the
     * number of visible rows can leave blank space while scrolling.
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
        const val DEFAULT_MAX_RENDERED_ROWS = 30
    }
}
