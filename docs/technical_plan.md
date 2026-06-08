# AI 广告推荐信息流 App 技术方案与开发计划

## 1. 项目基本信息

### 1.1 训练营课题

课题名称：AI 广告推荐信息流

项目目标：实现一个单列广告信息流 App，支持广告信息流浏览、多样式广告卡片、详情页交互、刷新加载、状态同步、资源复用、埋点统计，并结合大模型能力实现广告摘要、智能标签与对话式搜索。

### 1.2 技术栈选择

端侧选择：Android

主要技术栈：

* Kotlin
* Jetpack Compose
* MVVM 架构
* ViewModel + Repository 状态管理
* 本地 Mock 数据源
* Coil 图片加载与缓存
* Media3 / ExoPlayer 视频播放
* Windows 本地 Ollama / Qwen 模型服务
* FastAPI 或轻量 HTTP Server 作为本地 AI Search Server

### 1.3 入营前技术基础

本人入营前主要技术方向偏后端、系统开发与工程实践，对 Android 原生开发经验较少。本项目希望通过实现一个完整的信息流客户端，补充 Android UI、状态管理、页面导航、资源复用和移动端工程设计经验。

### 1.4 当前主要使用的 AI 工具

当前主要使用的 AI 工具包括：

* ChatGPT：用于需求拆解、方案设计、文档整理、开发思路讨论。
* Codex：用于具体 Android 代码实现、局部功能补全、问题修复和重构建议。
* 本地 Ollama / Qwen：计划用于对话式广告搜索中的自然语言理解和 tool calling。

AI 使用原则：

* AI 可以辅助生成代码和文档初稿，但核心方案设计、功能取舍、代码整合和最终验证由本人完成。
* 对 AI 生成代码进行人工检查，重点关注状态同步、页面跳转、列表性能、异常处理和功能边界。
* 对大模型输出进行结构化约束，避免模型直接生成不存在的广告内容。

---

## 2. 项目整体方案设计

### 2.1 总体目标

本项目不是简单实现一个广告列表，而是围绕广告信息流场景中的几个核心问题进行设计：

1. 用户能否流畅浏览广告信息流。
2. 广告卡片是否支持不同内容样式。
3. 用户从信息流进入详情页后，状态是否保持一致。
4. 视频、图片、列表 cell 等资源是否避免重复创建和浪费。
5. 用户行为是否能被本地模拟统计。
6. AI 能力是否能真正服务于广告内容理解和搜索，而不是简单堆接口。

最终目标是实现一个可运行、可演示、可解释的 Android 单列广告信息流 App。

### 2.2 整体架构

项目采用分层架构：

```text
UI 层
├── FeedScreen
├── AdDetailScreen
├── SearchScreen / SearchDialog
├── LargeImageAdCard
├── SmallImageAdCard
├── VideoAdCard
├── SkeletonCard
└── DemoControlPanel

ViewModel 层
├── FeedViewModel
├── DetailViewModel（可选）
└── SearchViewModel（后续新增）

数据层
├── AdRepository
├── MockAdRepository
├── AnalyticsRepository
└── AiSearchRepository

模型层
├── AdItem
├── AdCardType
├── AdChannel
├── AdStats
├── SearchQuery
└── SearchResult

资源与服务层
├── ImageLoader / Coil
├── VideoPlayerManager
├── AnalyticsTracker
├── LocalSearchMatcher
└── OllamaSearchClient / AiSearchServer
```

整体数据流：

```text
MockAdRepository / AiSearchServer
        ↓
ViewModel 统一管理 UI 状态
        ↓
Compose UI 渲染
        ↓
用户点击、点赞、收藏、播放、搜索
        ↓
ViewModel 调用 Repository 更新状态
        ↓
UI 自动刷新
```

---

## 3. 核心功能设计

## 3.1 单列广告信息流

首页采用单列信息流布局，使用 Jetpack Compose 的 LazyColumn 实现。

设计要点：

* 使用 LazyColumn 避免一次性渲染全部广告。
* 每条广告使用稳定的 adId 作为 key，减少列表刷新和状态更新时的错位问题。
* 支持滚动位置保持，用户进入详情页再返回时，不回到列表顶部。
* 支持 loading、empty、error、refreshing、loadingMore 等状态。

实现策略：

```text
FeedViewModel 持有 FeedUiState
FeedUiState 包含：
- 当前频道 selectedChannel
- 当前广告列表 ads
- 当前加载状态 isRefreshing / isLoadingMore
- 当前错误信息 errorMessage
- 当前过滤标签 selectedTag
- 当前播放视频 playingVideoAdId
```

---

## 3.2 广告卡片多样式

广告卡片支持三种基础样式：

1. 大图广告卡片
2. 小图广告卡片
3. 视频广告卡片

设计上不把所有样式写在一个巨大组件中，而是按照 cardType 拆分：

```text
AdCard
├── LargeImageAdCard
├── SmallImageAdCard
└── VideoAdCard
```

这样后续扩展新广告样式时，只需要新增 cardType 和对应组件，不需要破坏已有逻辑。

每张广告卡片展示内容包括：

* 标题
* 图片或视频封面
* AI 摘要
* 智能标签
* 点赞 / 收藏 / 分享入口
* 曝光 / 点击统计信息

---

## 3.3 顶部 Tab 频道切换

顶部频道包括：

* 精选
* 电商
* 本地

切换频道时刷新广告数据。

设计策略：

```text
用户点击 Tab
  ↓
FeedViewModel.updateChannel(channel)
  ↓
清空或切换当前列表状态
  ↓
MockAdRepository 加载对应频道数据
  ↓
FeedScreen 更新列表
```

当前阶段采用本地 Mock 数据模拟频道切换，后续可以替换为真实网络接口。

---

## 3.4 详情页与返回位置保持

点击广告卡片进入详情页。

页面跳转时只传递 adId，而不是传递完整 AdItem 对象：

```text
FeedScreen 点击 adId
  ↓
Navigate to detail/{adId}
  ↓
AdDetailScreen 根据 adId 从统一数据源读取广告
```

这样做的原因：

* 避免详情页和信息流各自维护一份广告状态。
* 点赞、收藏状态可以跨页面同步。
* 返回信息流后列表位置和用户操作状态保持一致。

详情页根据广告类型展示不同内容：

* 图文广告：大图、标题、摘要、标签、正文信息。
* 视频广告：视频区域、标题、摘要、标签、播放控制。

---

## 3.5 下拉刷新与上拉加载更多

信息流支持：

* 下拉刷新
* 上拉加载更多

当前采用本地 Mock 分页模拟。

设计策略：

```text
下拉刷新：
重新加载当前频道第一页数据。

上拉加载更多：
当列表接近底部时，加载下一页数据并追加到当前列表。
```

需要处理的状态：

* 正在刷新
* 正在加载更多
* 没有更多数据
* 加载失败
* 空列表

---

## 4. 资源复用设计

## 4.1 外流 cell 复用

“外流 cell”指信息流外层列表中的广告卡片，也就是 FeedScreen 里的每一条广告 item。

本项目使用 LazyColumn 实现信息流，只渲染当前可见区域附近的卡片，避免一次性创建全部广告 UI。

关键策略：

* 使用 LazyColumn，而不是普通 Column。
* items 使用稳定 key：ad.id。
* 卡片内部尽量不保存独立业务状态。
* 点赞、收藏、播放状态上提到 ViewModel / Repository。
* 图片和视频资源不跟随每个卡片重复创建。

---

## 4.2 播放器资源复用

视频广告不能为每个 VideoAdCard 都创建播放器。否则当信息流中存在多个视频广告时，会造成资源浪费和滚动卡顿。

设计策略：

```text
外流信息流：
- 视频默认显示封面，不自动播放。
- 用户点击播放按钮后才播放。
- 同一时间只允许一个视频播放。
- 播放新视频时，自动暂停旧视频。
- 视频滑出屏幕或离开页面时暂停。

详情页：
- 如果是视频广告，进入详情页后自动播放。
- 离开详情页时暂停。
- 静音状态沿用全局 isMuted 状态。
```

计划实现 VideoPlayerManager：

```text
VideoPlayerManager
├── 持有 ExoPlayer 实例
├── 记录 currentPlayingAdId
├── 控制 play / pause / release
└── 管理 isMuted 状态
```

如果时间不足，先实现训练营版本：

* 视频卡片展示封面和播放按钮。
* ViewModel 记录 playingVideoAdId。
* 同一时间只允许一个视频处于播放状态。
* 详情页实现基础播放控制。

---

## 4.3 缓存池设计

本项目中的缓存主要分为四类：

1. 图片缓存
2. 广告数据缓存
3. AI 摘要和标签缓存
4. 播放器资源缓存

设计策略：

```text
图片缓存：
使用 Coil 的内存缓存和磁盘缓存能力。

广告数据缓存：
由 MockAdRepository / FeedViewModel 持有当前频道列表和分页状态。

AI 结果缓存：
广告摘要和标签采用预计算方式，直接写入 mock 数据。
后续对话式搜索结果可按 query 缓存。

播放器缓存：
通过 VideoPlayerManager 复用播放器实例，避免重复创建。
```

---

## 5. 数据与状态同步设计

## 5.1 数据获取方式

当前阶段采用本地 Mock 数据源。

原因：

* 训练营重点在客户端信息流体验和工程设计。
* 本地 Mock 便于稳定演示。
* 可以模拟分页、刷新、错误态、空态。
* 后续可替换为真实网络请求，不影响 UI 层结构。

AI 对话式搜索单独通过 Windows 本地服务实现，属于 AI Search Server，不影响主信息流数据源。

---

## 5.2 数据生命周期

广告数据由 Repository 统一管理，ViewModel 负责转化为 UI 状态。

```text
MockAdRepository
  ↓
FeedViewModel
  ↓
FeedUiState
  ↓
FeedScreen / AdDetailScreen
```

状态包括：

* 广告列表
* 当前频道
* 当前过滤标签
* 当前加载状态
* 点赞状态
* 收藏状态
* 分享次数
* 曝光次数
* 点击次数
* 视频播放状态

---

## 5.3 跨页面状态同步

页面之间不传递完整广告对象，只传递 adId。

点赞状态同步流程：

```text
用户在详情页点击点赞
  ↓
FeedViewModel.toggleLike(adId)
  ↓
Repository 更新对应广告状态
  ↓
FeedScreen 和 AdDetailScreen 读取同一份状态
  ↓
返回信息流后卡片点赞状态保持一致
```

收藏、分享、点击统计同理。

这样可以避免信息流和详情页状态不一致。

---

## 5.4 视频状态同步

视频播放相关状态由统一状态管理：

```text
playingVideoAdId
isMuted
isPlayingInDetail
```

策略：

* 外流视频默认暂停。
* 点击外流视频播放按钮后开始播放。
* 同一时间只允许一个视频播放。
* 进入视频详情页后自动播放。
* 离开详情页时暂停。
* 静音状态全局共享。

---

## 6. 动画设计

动画不追求复杂视觉效果，主要围绕“反馈明确”和“降低等待感”。

### 6.1 页面切换动画

信息流进入详情页时使用滑入或淡入动画。

返回时保留列表位置，使用户感知当前仍处于同一浏览链路中。

### 6.2 互动动画

点赞和收藏按钮加入轻微缩放动画。

用户点击后：

```text
图标状态变化
数字更新
图标轻微放大后恢复
```

### 6.3 加载动画

首屏加载和刷新时展示 SkeletonCard 或 loading indicator，减少空白等待感。

---

## 7. 埋点统计设计

埋点指在用户关键行为发生时记录事件。本项目采用本地模拟统计，不接真实数据平台。

支持事件：

* 广告曝光
* 广告点击
* 点赞
* 收藏
* 分享
* 视频播放
* 标签点击
* 对话式搜索

曝光口径：

```text
训练营简化版：
广告卡片第一次出现在可见列表区域时，记一次曝光。

更严格口径：
广告卡片可见超过 50%，并停留超过 1 秒，记一次曝光。
同一广告在一次会话中只记录一次曝光。
```

点击口径：

```text
用户点击广告卡片进入详情页，记一次点击。
```

展示指标：

* 曝光数
* 点击数
* 点赞数
* 收藏数
* 分享数
* 点击率 CTR = 点击数 / 曝光数

实现方式：

```text
AnalyticsTracker
├── trackExposure(adId)
├── trackClick(adId)
├── trackLike(adId)
├── trackFavorite(adId)
├── trackShare(adId)
└── trackSearch(query)
```

数据可先保存在内存中，后续如有时间再使用 DataStore 做轻量持久化。

---

## 8. AI 功能设计

## 8.1 AI 摘要与智能标签

广告摘要和标签采用预计算方式。

原因：

真实广告业务中，广告标题、描述、类目、标签等内容通常在投放前或审核阶段生成，不会在用户每次滚动时实时调用大模型。

本项目中，每条广告在 Mock 数据中包含：

```text
aiSummary
tags
category
style
targetAudience
scene
```

卡片展示：

* 标题
* AI 摘要
* 标签

详情页展示：

* 更完整摘要
* 标签
* 类目 / 受众 / 场景信息

文档解释：

```text
摘要和标签采用预计算缓存策略，模拟真实广告投放链路中的离线内容理解。
客户端只负责展示和交互，不在滚动过程中实时调用大模型，避免延迟和不稳定。
```

---

## 8.2 标签点击过滤

用户点击广告卡片上的标签后，信息流只展示包含该标签的广告。

流程：

```text
用户点击标签“学生党”
  ↓
FeedViewModel 设置 selectedTag = "学生党"
  ↓
广告列表过滤
  ↓
顶部展示当前过滤条件
  ↓
用户可以清除过滤
```

标签过滤是 AI 标签的交互落地，使 AI 结果真正影响信息流浏览体验。

---

## 8.3 基于 Tool Calling 的对话式搜索

对话式搜索是本项目的主要 AI 亮点。

用户输入自然语言：

```text
我想看适合学生党的运动商品，最好性价比高一点
```

系统流程：

```text
Android App
  ↓ POST /chat-search
Windows 本地 AI Search Server
  ↓
Ollama / Qwen 8B
  ↓
模型通过 tool calling 调用 search_ads 工具
  ↓
search_ads 根据 tags / channel / keywords 搜索本地广告库
  ↓
返回真实存在的广告结果
  ↓
Android 展示搜索结果
```

关键设计：

本项目不让大模型直接生成广告列表，而是让大模型调用工具。

原因：

* 避免模型幻觉，返回不存在的广告。
* 搜索结果来自本地真实广告数据。
* 工具参数可校验。
* 便于降级为规则搜索。
* 便于记录搜索日志和调试。

工具定义示例：

```json
{
  "name": "search_ads",
  "description": "根据用户需求搜索广告信息流中的广告",
  "parameters": {
    "type": "object",
    "properties": {
      "channel": {
        "type": "string",
        "enum": ["精选", "电商", "本地"]
      },
      "tags": {
        "type": "array",
        "items": {
          "type": "string",
          "enum": ["运动", "学生党", "性价比", "数码", "美妆", "通勤", "外卖", "家居", "游戏", "旅游", "亲子", "本地生活", "优惠", "轻奢", "效率"]
        }
      },
      "keywords": {
        "type": "array",
        "items": {
          "type": "string"
        }
      }
    }
  }
}
```

降级方案：

```text
如果 Ollama 不可用：
  使用本地关键词匹配。

如果模型输出解析失败：
  使用 RuleBasedSearchParser。

如果搜索结果为空：
  展示空态和推荐标签。
```

---

## 9. 模块划分

## 9.1 UI 模块

负责页面展示和用户交互。

包含：

* FeedScreen
* AdDetailScreen
* SearchScreen / SearchDialog
* LargeImageAdCard
* SmallImageAdCard
* VideoAdCard
* SkeletonCard
* DemoControlPanel

重点：

* 单列信息流
* 多样式卡片
* 详情页
* 搜索入口
* 标签过滤
* 点赞收藏分享 UI
* loading / empty / error 状态

---

## 9.2 ViewModel 模块

负责 UI 状态管理。

包含：

* FeedViewModel
* SearchViewModel

重点：

* 当前频道
* 广告列表
* 分页状态
* 刷新状态
* 标签过滤
* 点赞收藏状态
* 视频播放状态
* 搜索状态

---

## 9.3 Repository 模块

负责数据管理。

包含：

* MockAdRepository
* AnalyticsRepository
* AiSearchRepository

重点：

* Mock 广告数据
* 分页模拟
* 刷新模拟
* 状态更新
* 埋点统计
* AI 搜索结果管理

---

## 9.4 AI Search Server 模块

运行在 Windows 本地。

建议使用 FastAPI 实现。

接口：

```text
POST /chat-search
```

输入：

```json
{
  "query": "我想看适合学生党的运动商品"
}
```

输出：

```json
{
  "message": "已为你找到 3 条相关广告",
  "ads": [],
  "debug": {
    "tags": ["学生党", "运动", "性价比"],
    "channel": "电商",
    "keywords": ["运动", "便宜"]
  }
}
```

---

## 10. 当前开发进展

## 10.1 初始起步阶段：6 月 4 日前

6 月 4 日前主要完成项目初始化和基础探索。

已完成内容包括：

* 创建 Android 项目。
* 完成信息流基础骨架。
* 初步实现 FeedScreen。
* 初步实现广告 Mock 数据。
* 完成刷新和分页状态的基础实现。
* 建立基本 Git 提交历史。

已有提交包括：

```text
Initial Android project
Implement feed skeleton
Implement refresh and pagination states
```

该阶段主要目标是让项目能跑起来，并形成最小的信息流页面。

---

## 10.2 6 月 4 日：需求重新拆解与核心功能确认

完成内容：

* 重新理解训练营题目要求。
* 将需求拆分为核心交互、资源复用、数据状态同步、动画、埋点统计和 AI 可选功能。
* 明确核心功能是基本盘，AI 功能是加分点。
* 初步确定项目采用 Android + Compose + MVVM + Mock 数据方案。

阶段结论：

```text
先保证核心信息流完整，再补 AI 摘要、标签过滤和对话式搜索。
```

---

## 10.3 6 月 5 日：信息流与状态管理完善

完成内容：

* 梳理 FeedViewModel 的职责。
* 明确信息流状态应由 ViewModel 统一管理。
* 明确详情页与信息流之间只传递 adId，避免状态复制。
* 梳理点赞、收藏、分享、曝光、点击等本地状态管理方式。

阶段结论：

```text
广告数据和交互状态应统一放在 Repository / ViewModel 中管理，页面不单独保存业务状态。
```

---

## 10.4 6 月 6 日：AI 功能方案讨论

完成内容：

* 分析可选功能中真正属于 AI 的部分。
* 明确第 1 条“AI 摘要与智能标签”和第 3 条“对话式搜索”是主要 AI 功能。
* 确定广告摘要和标签采用预计算方式，不在客户端滚动时实时调用模型。
* 初步确定对话式搜索使用本地 Ollama / Qwen 模型服务。

阶段结论：

```text
AI 摘要与标签采用预计算缓存，对话式搜索采用本地大模型服务。
```

---

## 10.5 6 月 7 日：Tool Calling 搜索方案确定

完成内容：

* 讨论自然语言搜索的实现方式。
* 确认不做实时语音流，优先做文本对话式搜索。
* 确认对话式搜索采用 tool calling 方式。
* 模型不直接返回广告，而是调用 search_ads 工具。
* App 或本地服务根据工具参数从真实广告数据中检索结果。

阶段结论：

```text
大模型负责理解用户意图和调用工具，广告结果由本地 search_ads 工具确定性返回。
```

---

## 10.6 6 月 8 日：技术方案整理与后续开发准备

完成内容：

* 整理整体技术方案。
* 明确资源复用、状态同步、动画、埋点统计的实现边界。
* 确定后续开发应分阶段推进，避免 Codex 一次性大改项目。
* 准备将当前阶段作为 Git checkpoint 提交，再开分支进行 AI 搜索开发。

阶段结论：

```text
当前应先提交已有进度，再让 Codex 按模块增量开发。
```

---

## 11. 后续时间规划

## 11.1 6 月 8 日晚：提交当前 checkpoint

目标：

* 检查当前工作区。
* 将已有 feed、card、detail、state、skeleton、demo control 相关修改提交。
* 开新分支进行后续 AI 功能开发。

建议提交：

```bash
git add -u
git add app/src/main/java/com/example/myapplication/ui/feed/DemoControlPanel.kt
git add app/src/main/java/com/example/myapplication/ui/feed/SkeletonCard.kt
git commit -m "Add feed card states and demo controls"
git switch -c ai-search-tool-calling
```

---

## 11.2 6 月 9 日：AI 摘要、标签与标签过滤

目标：

* AdItem 增加 aiSummary、tags、category、scene 等字段。
* MockAdRepository 补充每条广告的摘要和标签。
* 三种广告卡片展示摘要和标签。
* 详情页展示摘要和标签。
* 支持点击标签过滤。
* 支持清除过滤。

验收标准：

```text
用户可以在卡片上看到 AI 摘要和标签。
点击“学生党”等标签后，列表只展示相关广告。
详情页状态与信息流状态保持一致。
```

---

## 11.3 6 月 10 日：对话式搜索 UI 与本地规则搜索

目标：

* 新增搜索入口。
* 支持用户输入自然语言。
* 新增 SearchScreen 或 SearchDialog。
* 先用本地关键词 + 标签匹配实现搜索结果返回。
* 搜索结果复用现有广告卡片展示。

验收标准：

```text
用户输入“适合学生党的运动商品”后，即使不接大模型，也能返回本地匹配结果。
```

---

## 11.4 6 月 11 日：Windows 本地 AI Search Server

目标：

* 搭建 FastAPI 或轻量 HTTP 服务。
* 接入本地 Ollama / Qwen 8B。
* 实现 search_ads 工具。
* 模型通过 tool calling 生成搜索参数。
* search_ads 返回真实广告数据。

验收标准：

```text
在 Windows 本地调用 /chat-search，可以根据自然语言返回广告搜索结果。
```

---

## 11.5 6 月 12 日：Android 接入 AI Search Server

目标：

* Android 端新增 AiSearchRepository。
* 调用 Windows 本地 /chat-search 接口。
* 展示 loading、empty、error 状态。
* 服务不可用时降级到本地规则搜索。
* 记录搜索埋点。

验收标准：

```text
手机 App 可以通过局域网访问本地 AI Search Server。
Ollama 可用时走 tool calling 搜索。
Ollama 不可用时仍能本地搜索。
```

---

## 11.6 6 月 13 日：资源复用、视频和统计完善

目标：

* 完善图片加载和失败重试。
* 完善视频默认暂停、点击播放、静音控制。
* 如时间允许，实现 VideoPlayerManager。
* 完善曝光、点击、点赞、收藏、分享统计。
* DemoControlPanel 展示统计信息。

验收标准：

```text
信息流视频不会多个同时播放。
广告详情页可以展示统计数据。
统计面板能看到曝光、点击、点赞、收藏等模拟数据。
```

---

## 11.7 6 月 14 日：文档、README 与录屏

目标：

* 完成 README。
* 完成技术方案文档。
* 完成 AI 使用声明。
* 完成开发进展和日报整理。
* 录制 3 到 8 分钟 Demo 视频。

Demo 展示路径：

```text
打开 App
  ↓
浏览信息流
  ↓
切换频道
  ↓
下拉刷新 / 上拉加载
  ↓
查看 AI 摘要和标签
  ↓
点击标签过滤
  ↓
进入详情页点赞收藏
  ↓
返回后状态同步
  ↓
使用对话式搜索
  ↓
展示曝光 / 点击统计
```

---

## 12. AI Coding 使用记录

## 12.1 使用方式

AI 主要用于：

* 需求拆解
* 技术方案讨论
* Android 代码增量实现
* Compose UI 组件补全
* ViewModel 状态管理建议
* README 和技术文档整理
* Tool calling 搜索服务设计

## 12.2 关键决策记录

### 决策一：广告摘要采用预计算，而不是实时生成

原因：

* 广告内容相对稳定。
* 实时生成会影响滚动体验。
* 真实业务中也更接近离线或近线生成。
* 训练营项目中预计算更稳定，便于演示。

### 决策二：对话式搜索采用 tool calling，而不是让模型直接返回广告

原因：

* 避免大模型幻觉。
* 保证广告结果来自真实数据。
* 工具参数可以校验。
* 可以降级为规则匹配。
* 更符合 Agent 工具调用思想。

### 决策三：暂不做手机端嵌入模型

原因：

* Android 端侧模型集成复杂度较高。
* 模型包体、性能和兼容性风险较大。
* 当前训练营重点是客户端信息流和 AI 搜索链路。
* Windows 本地 Ollama 服务更适合一周内稳定交付。

### 决策四：语音输入不作为主线

原因：

* 题目要求的是“对话式搜索”，本质是自然语言文本查询。
* 实时语音流会引入麦克风权限、语音识别、噪声和延迟问题。
* 后续如果时间充足，可以增加“语音转文字后填入搜索框”作为加分项。

---

## 13. 日报

## 6 月 4 日

完成：

* 重新拆解训练营题目。
* 明确核心功能和可选功能边界。
* 确定 Android + Compose + MVVM + Mock 数据的基本方案。

问题：

* 对埋点、资源复用、外流 cell 等概念不熟悉。

解决思路：

* 将埋点理解为本地行为统计。
* 将外流 cell 理解为信息流中的广告卡片 item。
* 将资源复用拆解为 LazyColumn、图片缓存、播放器复用和数据缓存。

---

## 6 月 5 日

完成：

* 梳理数据和状态同步设计。
* 明确详情页和信息流之间只传 adId。
* 明确点赞、收藏、分享状态由统一数据源维护。

问题：

* 初期容易把详情页和信息流状态分开维护。

解决思路：

* 使用 Repository + ViewModel 管理统一广告状态。
* 页面只负责展示和触发事件。

---

## 6 月 6 日

完成：

* 梳理 AI 可选功能。
* 明确 AI 摘要和智能标签属于内容理解功能。
* 明确标签过滤是 AI 标签的交互落地。
* 初步规划对话式搜索功能。

问题：

* 不确定是否需要端侧部署模型或实时语音流。

解决思路：

* 端侧嵌入模型暂不作为主线。
* 对话式搜索优先实现文本输入。
* 本地模型服务使用 Windows Ollama 实现。

---

## 6 月 7 日

完成：

* 确定对话式搜索采用 tool calling。
* 明确大模型只负责搜索意图理解和工具调用。
* search_ads 工具负责真实广告检索。

问题：

* 担心大模型直接返回结果会产生幻觉。

解决思路：

* 限制模型只能调用 search_ads 工具。
* 工具参数使用固定标签池和频道枚举。
* 搜索结果来自本地广告库。

---

## 6 月 8 日

完成：

* 整理完整技术方案。
* 明确后续开发计划。
* 准备提交当前 checkpoint。
* 准备让 Codex 按模块继续开发。

问题：

* 后续功能较多，容易一次性让 AI 大改项目。

解决思路：

* 每次只让 Codex 做一个小模块。
* 每个阶段完成后提交 Git。
* 保持 main 分支稳定，AI 功能在新分支开发。

---

## 14. 给 Codex 的后续开发约束

后续使用 Codex 开发时，请遵守以下约束：

```text
1. 不要重写现有项目结构。
2. 不要大规模重构 FeedScreen、AdDetailScreen 和 FeedViewModel。
3. 优先增量修改。
4. 每次只实现一个明确功能模块。
5. 保持现有核心交互可运行。
6. 新增 AI 功能时必须提供降级方案。
7. 修改前先查看现有数据模型和 ViewModel 状态。
8. 修改后检查编译错误和基本交互路径。
```

推荐 Codex 第一阶段任务：

```text
在现有 Android 项目基础上，新增 AI 摘要、智能标签展示和标签点击过滤功能。

具体要求：
1. 为 AdItem 增加 aiSummary、tags、category、scene 字段。
2. 在 MockAdRepository 中为每条广告补充摘要和标签。
3. LargeImageAdCard、SmallImageAdCard、VideoAdCard 展示摘要和标签。
4. AdDetailScreen 展示摘要和标签。
5. 点击标签后回调 FeedViewModel 设置 selectedTag。
6. FeedScreen 根据 selectedTag 过滤广告列表。
7. 提供清除过滤按钮。
8. 不要重写现有信息流结构。
```

推荐 Codex 第二阶段任务：

```text
新增对话式搜索 UI，先使用本地规则匹配，不接入 Ollama。

具体要求：
1. 新增 SearchScreen 或顶部搜索入口。
2. 用户可以输入自然语言。
3. 根据 title、description、aiSummary、tags 进行本地匹配。
4. 展示搜索结果列表。
5. 搜索为空时展示空态。
6. 不影响原有 FeedScreen。
```

推荐 Codex 第三阶段任务：

```text
接入 Windows 本地 AI Search Server。

具体要求：
1. 新增 AiSearchRepository。
2. 支持配置 AI 服务地址。
3. 调用 /chat-search 接口。
4. 成功时展示服务端返回广告。
5. 失败时降级为本地规则搜索。
6. 增加 loading / error / empty 状态。
```

---

## 15. 当前阶段总结

当前项目已经完成从“能跑的信息流 Demo”向“有完整工程设计的信息流 App”的方案梳理。

后续重点不是盲目增加功能，而是按模块逐步完成：

```text
核心信息流稳定
  ↓
AI 摘要和标签展示
  ↓
标签过滤
  ↓
对话式搜索
  ↓
资源复用和视频状态
  ↓
埋点统计
  ↓
文档和演示视频
```

最终希望交付一个功能完整、方案清晰、代码可运行、AI 使用真实可解释的 Android 广告信息流项目。
