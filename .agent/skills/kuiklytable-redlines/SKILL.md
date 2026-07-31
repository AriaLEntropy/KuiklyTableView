---
name: kuiklytable-redlines
description: >-
  KuiklyTableView 仓库红线：官方边界、Issue 写法、截图与提交说明。
  触发：写 Issue、提官方、截图、GIF、提交说明、导师评论草稿、cross-reference、
  owner/repo#、README 进度语。
  可与其它 Skill 并行。不要替代 vision/requirements/design/implementation 主流程。
---

# KuiklyTable 红线

本仓库：`AriaLEntropy/KuiklyTableView`。

## 仓库边界

1. **禁止**向官方 KuiklyUI 提 PR、建 Issue、发评论、推分支；官方只读。
2. 本仓库改动合 `main`（或功能分支合回）；**不主动建 PR**，提交信息不写 PR 措辞。

## Issue 与对外沟通

1. 禁止正文写官方 `owner/repo#N`；用纯文字如「官方表格组件需求」。
2. Issue 只内联公开设计；可引本仓库源码、README、`site/`、官方公开文档；禁止本地私有路径。
3. 导师/维护者评论：AI 只写草稿，本人定稿发送。

## 截图与验证

1. 截图完整干净；无状态栏。
2. 截图须展示给用户确认。
3. 结论写明平台；未验证平台不宣称。

## 提交说明

写明验证范围与未验证项；勿把仅 Android 验证写成跨端已验证。

## 快速对照

| 场景 | 做 | 不做 |
| --- | --- | --- |
| 提官方需求 | 「官方表格组件需求」 | `Tencent-TDS/KuiklyUI#…` |
| Issue | 内联 API/验收 | 本地私有路径 |
| README | 已交付事实 | Issue 号、「验收中」 |
| 截图 | 裁净后给人看 | 口头确认 |
