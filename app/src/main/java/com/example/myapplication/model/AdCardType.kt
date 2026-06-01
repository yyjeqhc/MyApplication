package com.example.myapplication.model

/**
 * 广告卡片类型枚举
 * 用于区分信息流中不同样式的广告卡片
 */
enum class AdCardType {
    /** 大图广告卡片 - 全宽大图展示 */
    LARGE_IMAGE,

    /** 小图广告卡片 - 左图右文布局 */
    SMALL_IMAGE,

    /** 视频广告卡片 - 带播放按钮的视频占位 */
    VIDEO
}
