---
name: kuiklytable-tech-design
description: >-
  KuiklyTable 概要设计与详细设计。定组件族职责、模块依赖、渲染结构、布局/Windowed/
  固定列与公开 API 契约。
  触发：概要设计、详细设计、架构、组件族、模块边界、渲染树、列宽、Windowed、
  固定列、API 契约、互斥、正交、DataTable 封装。
  需求未清先 requirements；纯视觉文案先 uiux-design。不要用于：愿景口号（vision）；
  只改 Demo 文案（uiux）；直接大规模写码（implementation，设计结论后再交）。
---

# KuiklyTable 概要与详细设计

上游：`kuiklytable-requirements`；并行：`kuiklytable-uiux-design`。下游：`kuiklytable-implementation`。

完整 API 表：`site/index.html`。默认值以 `KuiklyTable` 源码为准。

## 分层

| 子层 | 写什么 | 仓库落点 |
| --- | --- | --- |
| 概要 | 组件族、职责、依赖、是否拆 module | `KuiklyTable` 边界；Issue 设计说明 |
| 详细 | 渲染树、布局、状态管线、Windowed、边界 | 源码结构；与 `site` 一致的契约 |

## 工作步骤

1. **概要**：Table → DataTable（组合封装）→ Tree（后续）；单 KMP module；形态为 Table（List 展示模式已移除）。
2. **详细要点**（变更时显式写出）：
   - `data` 只读；`displayRows` 派生；整体替换 ObservableList
   - Root/Viewport/Content；`width` 优先于 `minWidth+flex`
   - 表头纵滚固定；`fixedHeader=false` 无效
   - Windowed：固定行高、切片+spacer；与固定列互斥、勿热切换；与筛选/分页正交
   - 固定列：显式正数 `width` + 行高；仅左侧首列（多选时含选择列）
   - 点击：overflow > cellClick > rowClick
   - 已否定方案不得回潮：双纵向 List、pan 反向补偿、多行 H-Scroller 互相同步
3. **契约**：稳定字段进 README 摘要 + `site` 完整表；未验证不进「已交付」。
4. **交接**：可编码则 `kuiklytable-implementation`；表现文案并行 uiux-design。

## 边界

| 本 Skill | 交给其它 Skill |
| --- | --- |
| 架构、算法、互斥、API 契约 | Showcase 文案 → uiux-design |
| 标记待验证 | README「已交付」表述 → implementation / redlines |
| 指导实现顺序 | 愿景与验收 → vision / requirements |

## 完成前检查

- [ ] 概要与详细无矛盾；能对上 Issue 验收
- [ ] 待验证未写入 README 已交付
- [ ] 与 `site`/源码默认一致或已列出待同步项
