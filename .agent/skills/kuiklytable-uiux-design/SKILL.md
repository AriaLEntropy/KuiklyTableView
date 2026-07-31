---
name: kuiklytable-uiux-design
description: >-
  KuiklyTable UI/UX 设计与 Showcase/site 文案。定开箱观感、交互、配置面板与说明文案。
  触发：UI、UX、交互、视觉、斑马纹、主题、分隔线、圆角、Demo 文案、配置面板、
  ToggleChip、figcaption、Showcase 说明、site 用法章、fallback。
  需求未清时先 kuiklytable-requirements。不要用于：愿景范围（vision）；
  拆验收 Issue（requirements）；Windowed/固定列算法（tech-design）；
  大段业务逻辑实现（可与 implementation 交接，本 Skill 偏表现与文案）。
---

# KuiklyTable UI/UX 设计

上游：`kuiklytable-requirements`。并行/下游：`kuiklytable-tech-design`、`kuiklytable-implementation`。

公开表现面：`shared` Showcase、`site/`、README 截图说明。

## 目标

1. 把需求落成可感知的视觉与交互规则。
2. Showcase / `site` 文案写 **API/设计规则**，不写 Demo 现象。
3. 可点控件必有行为；可配置必能切回默认/fallback。

## 工作步骤

1. **对齐需求**：确认要设计的是哪几条验收，缺则回 requirements。
2. **定开箱默认**（以源码/`site` 为准）：`zebraStripe=false`、`lineMode=Grid`、`cornerRadius=8`。
3. **定交互**：配置在组件上方；Switch/分段；热区 ≥44dp；状态章勿用 sortable 列。
4. **分卡**：分隔线与圆角两个独立示例；斑马纹专卡显式开启。
5. **写文案**：属性名、默认、互斥；figcaption 写配置结果；主题说「预设/配色」不说「角色」。
6. **交接实现**：改页面/配图走 `kuiklytable-implementation`；布局互斥走 tech-design。

## 边界

| 做 | 不做 |
| --- | --- |
| 视觉规则、Demo IA、说明文案 | 列宽算法、Windowed spacer |
| fallback / 开箱体验 | 擅自扩需求范围 |
| Token 语义色方向 | 硬编码竞品 hex 当规范 |

## 完成前检查

- [ ] 规则能对上需求/Issue
- [ ] 开/关 fallback 可演示
- [ ] 文案无现象描述；与 `site`/README 不打架
