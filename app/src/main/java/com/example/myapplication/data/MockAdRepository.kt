package com.example.myapplication.data

import com.example.myapplication.model.AdCardType
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem

/**
 * Mock 广告数据仓库
 * 提供本地模拟数据，用于第一阶段开发测试
 */
object MockAdRepository {

    /** 内部可变广告列表 */
    private val _ads = mutableListOf<AdItem>()

    /** 不可变广告列表（外部访问） */
    val ads: List<AdItem> get() = _ads.toList()

    init {
        // 初始化 18 条 Mock 数据（3 个频道 × 6 条）
        _ads.addAll(createFeaturedAds())
        _ads.addAll(createEcommerceAds())
        _ads.addAll(createLocalAds())
    }

    /**
     * 根据频道筛选广告
     */
    fun getAdsByChannel(channel: AdChannel): List<AdItem> {
        return _ads.filter { it.channel == channel }
    }

    /**
     * 根据 ID 获取广告（返回最新状态）
     */
    fun getAdById(id: String): AdItem? {
        return _ads.find { it.id == id }
    }

    /**
     * 切换点赞状态
     */
    fun toggleLike(adId: String) {
        val index = _ads.indexOfFirst { it.id == adId }
        if (index != -1) {
            val ad = _ads[index]
            val newLiked = !ad.liked
            _ads[index] = ad.copy(
                liked = newLiked,
                likeCount = if (newLiked) ad.likeCount + 1 else ad.likeCount - 1
            )
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(adId: String) {
        val index = _ads.indexOfFirst { it.id == adId }
        if (index != -1) {
            val ad = _ads[index]
            _ads[index] = ad.copy(favorited = !ad.favorited)
        }
    }

    // ==================== 精选频道数据 ====================

    private fun createFeaturedAds(): List<AdItem> = listOf(
        AdItem(
            id = "featured_001",
            title = "夏季清凉特惠节",
            subtitle = "全场商品低至3折",
            channel = AdChannel.FEATURED,
            cardType = AdCardType.LARGE_IMAGE,
            imageUrl = "https://example.com/featured_001.jpg",
            videoUrl = "",
            tags = listOf("限时特惠", "夏季促销", "热销"),
            summary = "炎炎夏日，清凉一夏！精选商品低至3折，数量有限，先到先得。涵盖服饰、家居、数码等多个品类。",
            liked = false,
            favorited = false,
            likeCount = 1234,
            exposureCount = 56789,
            clickCount = 2345
        ),
        AdItem(
            id = "featured_002",
            title = "新品手机首发预约",
            subtitle = "旗舰芯片 超长续航",
            channel = AdChannel.FEATURED,
            cardType = AdCardType.VIDEO,
            imageUrl = "https://example.com/featured_002.jpg",
            videoUrl = "https://example.com/featured_002.mp4",
            tags = listOf("新品首发", "数码", "手机"),
            summary = "全新旗舰手机震撼发布！搭载最新处理器，120Hz高刷屏幕，5000mAh大电池，预约即享多重好礼。",
            liked = true,
            favorited = false,
            likeCount = 8901,
            exposureCount = 45678,
            clickCount = 3456
        ),
        AdItem(
            id = "featured_003",
            title = "在线教育课程特惠",
            subtitle = "名师辅导 提升技能",
            channel = AdChannel.FEATURED,
            cardType = AdCardType.SMALL_IMAGE,
            imageUrl = "https://example.com/featured_003.jpg",
            videoUrl = "",
            tags = listOf("教育", "在线课程", "技能提升"),
            summary = "海量优质课程限时特惠，涵盖编程、设计、语言、职场等多个领域，助力你的职业发展。",
            liked = false,
            favorited = true,
            likeCount = 567,
            exposureCount = 23456,
            clickCount = 1234
        ),
        AdItem(
            id = "featured_004",
            title = "智能家居焕新季",
            subtitle = "让生活更智能",
            channel = AdChannel.FEATURED,
            cardType = AdCardType.LARGE_IMAGE,
            imageUrl = "https://example.com/featured_004.jpg",
            videoUrl = "",
            tags = listOf("智能家居", "科技", "生活"),
            summary = "智能家居产品大促，智能音箱、扫地机器人、智能灯具等多款爆品，让你的家更智能更舒适。",
            liked = false,
            favorited = false,
            likeCount = 678,
            exposureCount = 34567,
            clickCount = 1890
        ),
        AdItem(
            id = "featured_005",
            title = "健康生活好物推荐",
            subtitle = "品质生活从健康开始",
            channel = AdChannel.FEATURED,
            cardType = AdCardType.VIDEO,
            imageUrl = "https://example.com/featured_005.jpg",
            videoUrl = "https://example.com/featured_005.mp4",
            tags = listOf("健康", "养生", "好物推荐"),
            summary = "精选健康好物，包括运动器材、保健品、健康食品等，让你轻松拥有健康生活方式。",
            liked = false,
            favorited = false,
            likeCount = 456,
            exposureCount = 12345,
            clickCount = 890
        ),
        AdItem(
            id = "featured_006",
            title = "旅行出行装备精选",
            subtitle = "说走就走的旅行",
            channel = AdChannel.FEATURED,
            cardType = AdCardType.SMALL_IMAGE,
            imageUrl = "https://example.com/featured_006.jpg",
            videoUrl = "",
            tags = listOf("旅行", "出行", "装备"),
            summary = "精选旅行出行装备，行李箱、背包、旅行配件等多款好物，让你的旅途更轻松愉快。",
            liked = true,
            favorited = true,
            likeCount = 789,
            exposureCount = 45678,
            clickCount = 2345
        )
    )

    // ==================== 电商频道数据 ====================

    private fun createEcommerceAds(): List<AdItem> = listOf(
        AdItem(
            id = "ecommerce_001",
            title = "时尚女装新品上市",
            subtitle = "引领潮流风尚",
            channel = AdChannel.ECOMMERCE,
            cardType = AdCardType.LARGE_IMAGE,
            imageUrl = "https://example.com/ecommerce_001.jpg",
            videoUrl = "",
            tags = listOf("女装", "时尚", "新品"),
            summary = "最新时尚女装系列，精选优质面料，独特设计，让你成为人群中的焦点。新品上市，限时优惠。",
            liked = false,
            favorited = false,
            likeCount = 2345,
            exposureCount = 67890,
            clickCount = 4567
        ),
        AdItem(
            id = "ecommerce_002",
            title = "美妆护肤专场",
            subtitle = "大牌美妆 低至5折",
            channel = AdChannel.ECOMMERCE,
            cardType = AdCardType.VIDEO,
            imageUrl = "https://example.com/ecommerce_002.jpg",
            videoUrl = "https://example.com/ecommerce_002.mp4",
            tags = listOf("美妆", "护肤", "大牌"),
            summary = "国际大牌美妆护肤产品专场，涵盖口红、粉底、精华、面膜等多品类，正品保障，限时特惠。",
            liked = true,
            favorited = false,
            likeCount = 5678,
            exposureCount = 89012,
            clickCount = 6789
        ),
        AdItem(
            id = "ecommerce_003",
            title = "运动户外装备",
            subtitle = "专业装备 助力运动",
            channel = AdChannel.ECOMMERCE,
            cardType = AdCardType.SMALL_IMAGE,
            imageUrl = "https://example.com/ecommerce_003.jpg",
            videoUrl = "",
            tags = listOf("运动", "户外", "装备"),
            summary = "专业运动户外装备，跑鞋、运动服、健身器材等，品质保证，让你的运动更专业更舒适。",
            liked = false,
            favorited = true,
            likeCount = 1234,
            exposureCount = 34567,
            clickCount = 2345
        ),
        AdItem(
            id = "ecommerce_004",
            title = "母婴用品精选",
            subtitle = "给宝宝最好的",
            channel = AdChannel.ECOMMERCE,
            cardType = AdCardType.LARGE_IMAGE,
            imageUrl = "https://example.com/ecommerce_004.jpg",
            videoUrl = "",
            tags = listOf("母婴", "婴儿用品", "精选"),
            summary = "精选母婴用品，奶粉、纸尿裤、婴儿服饰、玩具等，品质安全有保障，给宝宝最好的呵护。",
            liked = false,
            favorited = false,
            likeCount = 3456,
            exposureCount = 56789,
            clickCount = 3456
        ),
        AdItem(
            id = "ecommerce_005",
            title = "数码配件特卖",
            subtitle = "品质配件 超值价格",
            channel = AdChannel.ECOMMERCE,
            cardType = AdCardType.VIDEO,
            imageUrl = "https://example.com/ecommerce_005.jpg",
            videoUrl = "https://example.com/ecommerce_005.mp4",
            tags = listOf("数码", "配件", "特卖"),
            summary = "数码配件特卖会，耳机、充电器、数据线、手机壳等，品质保证，价格实惠，数量有限。",
            liked = false,
            favorited = false,
            likeCount = 890,
            exposureCount = 23456,
            clickCount = 1567
        ),
        AdItem(
            id = "ecommerce_006",
            title = "家居家纺大促",
            subtitle = "舒适生活 从家开始",
            channel = AdChannel.ECOMMERCE,
            cardType = AdCardType.SMALL_IMAGE,
            imageUrl = "https://example.com/ecommerce_006.jpg",
            videoUrl = "",
            tags = listOf("家居", "家纺", "大促"),
            summary = "家居家纺大促活动，床品四件套、毛巾浴巾、窗帘地毯等，品质优良，让你的家更温馨舒适。",
            liked = true,
            favorited = true,
            likeCount = 2345,
            exposureCount = 45678,
            clickCount = 2890
        )
    )

    // ==================== 本地频道数据 ====================

    private fun createLocalAds(): List<AdItem> = listOf(
        AdItem(
            id = "local_001",
            title = "周边美食探店",
            subtitle = "发现身边的好味道",
            channel = AdChannel.LOCAL,
            cardType = AdCardType.LARGE_IMAGE,
            imageUrl = "https://example.com/local_001.jpg",
            videoUrl = "",
            tags = listOf("美食", "探店", "本地生活"),
            summary = "发现周边美食好去处，精选本地特色餐厅，新用户专享优惠，让你的味蕾尽享美味。",
            liked = false,
            favorited = false,
            likeCount = 4567,
            exposureCount = 78901,
            clickCount = 5678
        ),
        AdItem(
            id = "local_002",
            title = "家政服务预约",
            subtitle = "专业服务 放心到家",
            channel = AdChannel.LOCAL,
            cardType = AdCardType.VIDEO,
            imageUrl = "https://example.com/local_002.jpg",
            videoUrl = "https://example.com/local_002.mp4",
            tags = listOf("家政", "服务", "预约"),
            summary = "专业家政服务，保洁、保姆、月嫂、维修等，持证上岗，服务保障，让你省心更放心。",
            liked = true,
            favorited = false,
            likeCount = 2345,
            exposureCount = 45678,
            clickCount = 3456
        ),
        AdItem(
            id = "local_003",
            title = "健身瑜伽课程",
            subtitle = "遇见更好的自己",
            channel = AdChannel.LOCAL,
            cardType = AdCardType.SMALL_IMAGE,
            imageUrl = "https://example.com/local_003.jpg",
            videoUrl = "",
            tags = listOf("健身", "瑜伽", "课程"),
            summary = "本地健身房瑜伽课程特惠，专业教练指导，小班教学，新会员体验课免费预约。",
            liked = false,
            favorited = true,
            likeCount = 1890,
            exposureCount = 34567,
            clickCount = 2345
        ),
        AdItem(
            id = "local_004",
            title = "汽车保养维修",
            subtitle = "爱车养护 专业可靠",
            channel = AdChannel.LOCAL,
            cardType = AdCardType.LARGE_IMAGE,
            imageUrl = "https://example.com/local_004.jpg",
            videoUrl = "",
            tags = listOf("汽车", "保养", "维修"),
            summary = "专业汽车保养维修服务，原厂配件，技师持证上岗，保养套餐限时特惠，让你的爱车焕然一新。",
            liked = false,
            favorited = false,
            likeCount = 1567,
            exposureCount = 23456,
            clickCount = 1890
        ),
        AdItem(
            id = "local_005",
            title = "宠物服务专区",
            subtitle = "给毛孩最好的关爱",
            channel = AdChannel.LOCAL,
            cardType = AdCardType.VIDEO,
            imageUrl = "https://example.com/local_005.jpg",
            videoUrl = "https://example.com/local_005.mp4",
            tags = listOf("宠物", "服务", "关爱"),
            summary = "宠物洗澡美容、寄养、医疗等一站式服务，专业团队，爱心呵护，让你的毛孩健康快乐。",
            liked = false,
            favorited = false,
            likeCount = 2890,
            exposureCount = 56789,
            clickCount = 3456
        ),
        AdItem(
            id = "local_006",
            title = "鲜花速递服务",
            subtitle = "传递心意 送达美好",
            channel = AdChannel.LOCAL,
            cardType = AdCardType.SMALL_IMAGE,
            imageUrl = "https://example.com/local_006.jpg",
            videoUrl = "",
            tags = listOf("鲜花", "速递", "礼物"),
            summary = "本地鲜花速递服务，精选新鲜花材，专业花艺师设计，2小时急速送达，让爱意及时传递。",
            liked = true,
            favorited = true,
            likeCount = 3456,
            exposureCount = 67890,
            clickCount = 4567
        )
    )
}
