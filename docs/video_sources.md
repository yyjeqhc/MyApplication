# Video Sources

本项目当前已在 `app/src/main/assets/ad_videos/` 中放入 8 个本地 mp4 视频文件，并通过 `mock_ads.json` 的 `videoAsset` 字段被视频广告卡片引用。视频用于训练营 Demo 的本地播放能力验证，不依赖网络请求。

## 来源记录口径

本文件记录的是“候选来源 / 下载来源线索 / 本地文件映射”，不是严格的原始文件名追踪表。项目早期通过 GPT / AI 辅助整理过一批 Pexels 视频候选页面，实际开发时从候选来源链路下载素材后，为了便于 `mock_ads.json` 引用和代码阅读，将本地文件统一改成了语义化文件名，例如 `video_coffee_01.mp4`、`video_running_01.mp4`。

因此，本地 mp4 文件名和原始页面标题、原始下载文件名不完全一一对应是正常的开发过程，不表示素材错误或仓库资源不一致。若未来用于正式版权追踪，需要补充更严格的原始页面、作者、下载时间和授权记录。

## 当前本地视频文件

| 本地文件名 | 覆盖场景 | 来源/下载线索 | 本地映射说明 |
|---|---|---|---|
| `video_coffee_01.mp4` | 咖啡/餐饮 | 早期候选页面记录为 Pexels coffee 视频：https://www.pexels.com/video/coffee-17422066/ | 下载后按项目语义重命名，用于咖啡、到店、餐饮场景。 |
| `video_running_01.mp4` | 跑步/健身 | 早期候选页面记录为 Pexels running 视频：https://www.pexels.com/video/runner-in-urban-setting-exercising-outdoors-31328833/ | 下载后按项目语义重命名，用于运动、跑步、训练场景。 |
| `video_fitness_01.mp4` | 健身/运动 | 来源线索来自早期候选/下载素材链路；精确原始页面未在当前文件名中保留 | 下载后按项目语义重命名，用于健身课程、运动装备等场景；如需严格版权表可后续补充原始页面。 |
| `video_digital_desk_01.mp4` | 数码桌面 | 早期候选页面记录为 Pexels work desk 视频：https://www.pexels.com/video/work-desk-with-desktop-computer-6558513/ | 下载后按项目语义重命名，用于数码、办公桌面、效率工具场景。 |
| `video_travel_01.mp4` | 旅行 | 早期候选页面记录为 Pexels travel 视频：https://www.pexels.com/video/travel-15960280/ | 下载后按项目语义重命名，用于旅行、短途出游、生活方式场景。 |
| `video_parent_child_01.mp4` | 亲子活动 | 早期候选页面记录为 Pexels parent-child 视频：https://www.pexels.com/video/kid-and-parent-coloring-together-7102426/ | 下载后按项目语义重命名，用于亲子、周末活动、家庭陪伴场景。 |
| `video_home_01.mp4` | 家居生活 | 早期候选页面记录为 Pexels living room 视频：https://www.pexels.com/video/video-footage-of-a-living-room-4231455/ | 下载后按项目语义重命名，用于家居、生活方式广告。 |
| `video_gaming_01.mp4` | 游戏/电竞桌面 | 早期候选页面记录为 Pexels gaming desk 视频：https://www.pexels.com/video/playing-a-video-game-with-a-desk-computer-7914927/ | 下载后按项目语义重命名，用于电竞、桌面设备、潮流数码场景。 |

## 使用方式

- 本地视频路径写入 `mock_ads.json` 的 `videoAsset` 字段，例如 `ad_videos/video_coffee_01.mp4`。
- `LocalVideoPlayer` 使用 Media3 ExoPlayer + `AssetDataSource` 从 assets 读取视频。
- Feed、Search、Detail 共用同一套按 adId 保存的视频进度状态。
- Feed 支持自动播放预览；Search 页只支持手动播放，不自动播放。

## 注意事项

- 当前视频仅用于本地 Demo 展示，不代表已经完成正式版权、肖像权、商用授权审查。
- 本文件中的链接是候选来源和下载线索，不应理解为严格的一一对应原始文件名追踪表。
- 如用于真实上线产品，需要重新复核素材授权、人物肖像、品牌露出、平台规则，并补充精确原始页面与下载记录。
- 当前没有网络视频、全屏视频或横屏播放。
