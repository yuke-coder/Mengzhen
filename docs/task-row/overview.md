# 任务行组件重构报告

## 做了什么

从 HTML 原型到安卓原生 Compose 组件，完整重构了梦枕 App 的任务行样式。

### 产物清单

| 文件 | 说明 |
|------|------|
| `task-row-final.html` | HTML 原型 v4，可浏览器预览 |
| `android/.../ui/screens/TaskRowCard.kt` | Jetpack Compose 原生组件 |

---

## HTML 原型迭代过程

### v1 → v2：紧凑瘦身
把间距、字号、圆角全面压了一圈，min-height 从 72px 降到 56px。

### v2 → v3：DOM 扁平化 + 类名精简
- 砍掉 `compact__leading / compact__content / compact__trailing` 三层包裹
- 紧凑态直接 `dot + body + chev` 三个兄弟节点
- CSS 变量体系化：`sp-1~5` 间距、`r-sm/md/lg/xl` 圆角、`sh1~3` 阴影
- 纯 CSS 波形动画替代 SVG
- 新增 `card:active` 按压缩放

### v3 → v4：彻底减嵌套
调用原型构建师 agent 重写，DOM 深度砍到极限：

| 区域 | v3 | v4 |
|------|-----|-----|
| 紧凑态 | 5-6 层 | 最多 3 层 |
| 展开态 | 7-8 层 | 最多 4 层 |
| 辉光条 | DOM 元素 | 伪元素 |
| 网格标签 | span + span | 伪元素 attr() |
| Chip 闪烁点 | DOM span | 伪元素 |

关键手法：
- `data-st` 属性驱动主题（on/amber/wait/done/off）
- CSS Grid `grid-template-areas` 布局展开态
- `tc::before` + `attr(data-lbl)` 伪元素显示标签
- 音量改为 7 根柱子可视化

### 最终调整：换行显示
紧凑态时间从挤在一行改为独占第二行：
```
第一行：●  深海鲸歌 · 低频共振  ▮▮▮▮  播放中  ›
第二行：   22:30:00 → 23:15:00
```
HTML 用 `flex-basis:100%` + `flex-wrap:wrap` 实现，Compose 用 Column 分两行。

---

## 安卓 Compose 组件

### 文件结构
`TaskRowCard.kt` 包含：

- **TaskRowTokens** — 暗/亮色全套色值、圆角、间距常量
- **TaskRowVisualState** — 5 态枚举：执行中播放 / 执行中渐强 / 待执行 / 已完成 / 已关闭
- **TaskRowData** — 纯数据类，从 ScheduledTask + PlaybackSnapshot 推导
- **ScheduledTask.toRowData()** — 数据模型转换扩展函数
- **rememberPalette()** — 根据主题+状态返回色板
- **TaskRowCard** — 主 Composable
- 子组件：GlowBar / StatusDot / WaveBars / Chip / Chevron / TimeGrid / VolumeBars / FadeTags

### 数据对齐
直接使用项目现有模型：
- `ScheduledTask` / `TaskStartTime`（含 second 字段）
- `TaskPhase`（FADING_IN / PLAYING / FADING_OUT）
- `PlaybackSnapshot` / `PlaybackTransportState`
- `TaskRepeatType` / `ScheduledStopMode`

### 动画
- `rememberInfiniteTransition` 驱动辉光脉冲、状态点缩放、波形柱跳动
- `animateFloatAsState` 驱动按压缩放和箭头旋转

---

## 设计系统

沿用 Spotify × Soft Warm × Tech Utility 混合方向：
- 暗色背景偏暖黑 #0F0D0B
- 品牌色 #2BC496（执行中）、琥珀色 #E8A04C（渐强中）
- 去边框设计：零 border/stroke，靠 bg 色差 + elevation 分层
- 等宽字体 JetBrains Mono / tabular-nums 用于所有时间数字
- prefers-reduced-motion 降级

---

## 下一步

1. 把 `XimalyaAlarmListAdapter` + `main_alarm_item.xml` 替换为 `LazyColumn` + `TaskRowCard`
2. 编译验证，修复可能的类型问题
3. 真机预览效果
