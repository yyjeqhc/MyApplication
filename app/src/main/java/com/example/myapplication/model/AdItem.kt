package com.example.myapplication.model

/**
 * 广告数据模型
 * 包含广告的所有展示信息和互动状态
 */
data class AdItem(
    /** 广告唯一标识 */
    val id: String,

    /** 广告标题 */
    val title: String,

    /** 广告副标题 */
    val subtitle: String,

    /** 所属频道 */
    val channel: AdChannel,

    /** 卡片类型 */
    val cardType: AdCardType,

    /** 图片链接（第一阶段为占位符） */
    val imageUrl: String,

    /** 本地 assets 图片路径，如 ad_images/example.jpg */
    val imageAsset: String = "",

    /** 视频链接（第一阶段为占位符） */
    val videoUrl: String,

    /** 本地 assets 视频路径，如 ad_videos/example.mp4 */
    val videoAsset: String = "",

    /** 图片展示类型（从 assets mock 数据读取，用于后续扩展） */
    val imageType: String = "",

    /** 视频时长，非视频广告为空 */
    val videoDuration: String = "",

    /** 智能标签列表 */
    val tags: List<String>,

    /** 广告摘要/描述 */
    val summary: String,

    /** 是否已点赞 */
    val liked: Boolean,

    /** 是否已收藏 */
    val favorited: Boolean,

    /** 点赞数量 */
    val likeCount: Int,

    /** 曝光数量 */
    val exposureCount: Int,

    /** 点击数量 */
    val clickCount: Int,

    /** 分享数量 */
    val shareCount: Int = 0,

    /** 品牌名称 */
    val brandName: String = "",

    /** CTA 按钮文案 */
    val ctaText: String = "了解更多",

    /** AI 摘要 */
    val aiSummary: String = "",

    /** 品类 */
    val category: String = "",

    /** 使用场景 */
    val scene: String = "",

    /** 推荐理由 */
    val recommendationReason: String = "",

    /** 目标受众 */
    val targetAudience: String = "",

    /** 是否为广告标识 */
    val isAd: Boolean = true
)
