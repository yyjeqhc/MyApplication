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

    /** 视频链接（第一阶段为占位符） */
    val videoUrl: String,

    /** 标签列表 */
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
    val clickCount: Int
)
