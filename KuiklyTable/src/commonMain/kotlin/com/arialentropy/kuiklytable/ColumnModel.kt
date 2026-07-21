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
 * @param width 固定列宽（dp），为 null 时使用 flex 分配
 * @param flex 弹性权重，仅在 width 为 null 时生效
 * @param alignment 单元格文字对齐方式（响应式，运行时修改会触发表格重渲染）
 * @param cellRenderer 可选的单元格渲染器；未配置时使用默认 Text
 */
class ColumnModel<T>(
    val key: String,
    val title: String,
    val accessor: (T) -> String,
    val width: Float? = null,
    val flex: Float = 1f,
    alignment: ColumnAlignment = ColumnAlignment.Start,
    val cellRenderer: (ViewContainer<*, *>.(T, ColumnModel<T>) -> Unit)? = null,
) {
    var alignment: ColumnAlignment by observable(alignment)
}
