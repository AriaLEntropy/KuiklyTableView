package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable

/**
 * 列对齐方式
 */
sealed class ColumnAlignment {
    /** 左对齐（默认，适合文本） */
    object Start : ColumnAlignment()

    /** 居中 */
    object Center : ColumnAlignment()

    /** 右对齐（适合数字） */
    object End : ColumnAlignment()
}

/**
 * 列定义模型
 *
 * @param key 列唯一标识
 * @param title 表头显示文字
 * @param accessor 从数据行提取该列显示值的函数
 * @param width 固定列宽（dp），非 null 时优先于 minWidth 和 flex
 * @param minWidth 最小列宽（dp），仅在 width 为 null 时生效
 * @param flex 剩余空间分配权重，仅在 width 为 null 时生效
 * @param alignment 单元格文字对齐方式（响应式，运行时修改会触发表格重渲染）
 * @param sortable 是否允许点击表头切换排序
 * @param sortComparator 可选的业务值比较器；未配置时按 accessor 返回的字符串比较
 * @param cellRenderer 可选的单元格渲染器；未配置时使用默认 Text
 * @param headerRenderer 可选的表头渲染器；未配置时使用默认 Text
 * @param enableRowClick 点击该列单元格时是否允许触发 Table 的 [TableEvent.rowClick]；与是否配置 cellRenderer 无关
 * @param enableCellClick 点击该列单元格时是否允许触发 Table 的 [TableEvent.cellClick]；与是否配置 cellRenderer 无关
 */
class ColumnModel<T>(
    val key: String,
    val title: String,
    val accessor: (T) -> String,
    val width: Float? = null,
    val minWidth: Float = DEFAULT_MIN_WIDTH,
    val flex: Float = 1f,
    alignment: ColumnAlignment = ColumnAlignment.Start,
    val sortable: Boolean = false,
    val sortComparator: Comparator<T>? = null,
    val cellRenderer: (ViewContainer<*, *>.(T, ColumnModel<T>) -> Unit)? = null,
    val headerRenderer: (ViewContainer<*, *>.(ColumnModel<T>) -> Unit)? = null,
    val enableRowClick: Boolean = true,
    val enableCellClick: Boolean = false,
) {
    var alignment: ColumnAlignment by observable(alignment)

    companion object {
        const val DEFAULT_MIN_WIDTH = 100f
    }
}
