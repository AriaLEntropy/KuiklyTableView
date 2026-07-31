---
name: kuiklytable-vision
description: >-
  KuiklyTable 愿景与范围入口。固定产品目标、组件族、阶段边界、职责归属与公开契约面。
  触发：愿景、产品目标、范围、组件族、Table/DataTable/Tree、阶段优先级、
  职责边界、范围收窄、为什么做 Table、roadmap。
  不要用于：写验收条目/拆 Issue（改 kuiklytable-requirements）；画交互或改 Demo 文案
  （改 kuiklytable-uiux-design）；改布局/Windowed API（改 kuiklytable-tech-design）；
  直接改业务代码（改 kuiklytable-implementation）。
---

# KuiklyTable 愿景

本仓库：`AriaLEntropy/KuiklyTableView`。本 Skill 只**固定愿景与边界**，不写需求编号、不写详细设计、不改实现。

下游：`kuiklytable-requirements` → 设计 skills → `kuiklytable-implementation`。交付红线：`kuiklytable-redlines`。

## 目标

1. 用一两句话说清：为谁、解决什么、交付物是什么。
2. 钉死组件族与阶段：`KuiklyTable` → `KuiklyDataTable` → `KuiklyTreeTable`。
3. 用**当前交付 / 后续 / 范围外归属**划清边界，避免竞品功能自动扩范围。
4. 对齐公开契约面：README + `site/` + Showcase；进度只留 Issue。

## 工作步骤

1. **重述愿景**：独立仓库闭环交付可运行组件 + Showcase + 构建/截图；官方 KuiklyUI 只读参考。
2. **划阶段**：Basic 已交付基线；当前主线 DataTable；Tree 后续。
3. **写清归属**（正向表述）：例如表头始终纵滚固定；搜索/导出由业务层；窄屏列表用原生 `List`；树形归 TreeTable。
4. **公开面检查**：README 只写已交付事实；无 Issue 号与进度语。
5. **输出愿景摘要**：目标一句话、组件族、当前阶段、范围归属、下一步（通常交 requirements）。

## 边界

| 本 Skill | 交给其它 Skill |
| --- | --- |
| 范围、优先级、契约面 | 验收编号与 Issue → requirements |
| 组件族职责一句话 | 渲染树、布局、API 表 → tech-design |
| 阶段与归属 | 改代码 → implementation |

## 完成前检查

- [ ] 边界用「当前交付 / 后续 / 由谁负责」写清，可执行
- [ ] 未把后续能力写成当前必交付
- [ ] 未越权写详细验收或技术方案
- [ ] 已标明下游 Skill（通常 `kuiklytable-requirements`）
