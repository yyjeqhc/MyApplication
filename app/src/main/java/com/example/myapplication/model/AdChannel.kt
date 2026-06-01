package com.example.myapplication.model

/**
 * 广告频道枚举
 * 定义信息流顶部的频道 Tab
 */
enum class AdChannel(val displayName: String) {
    /** 精选频道 - 推荐的优质广告 */
    FEATURED("精选"),

    /** 电商频道 - 电商类广告 */
    ECOMMERCE("电商"),

    /** 本地频道 - 本地生活服务广告 */
    LOCAL("本地")
}
