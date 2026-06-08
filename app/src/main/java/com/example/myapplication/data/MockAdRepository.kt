package com.example.myapplication.data

import com.example.myapplication.model.AdCardType
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem

/**
 * Mock 广告数据仓库
 * 提供本地模拟数据，用于开发测试
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

    /**
     * 重置所有数据到初始状态
     */
    fun reset() {
        _ads.clear()
        _ads.addAll(createFeaturedAds())
        _ads.addAll(createEcommerceAds())
        _ads.addAll(createLocalAds())
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
            clickCount = 2345,
            brandName = "天猫商城",
            ctaText = "立即抢购",
            aiSummary = "基于您的购物偏好和历史浏览记录，为您推荐夏季清凉特惠活动。本次活动涵盖多个品类，折扣力度大，适合有换季购物需求的用户。",
            recommendationReason = "您最近浏览了多款夏季服饰，本次活动有大量相关商品优惠。",
            targetAudience = listOf("网购爱好者", "追求性价比", "换季购物需求"),
            isAd = true
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
            clickCount = 3456,
            brandName = "小米科技",
            ctaText = "预约抢购",
            aiSummary = "根据您对数码产品的关注度，为您推荐这款新发布的旗舰手机。该机型在性能、续航、拍照等方面均有显著提升。",
            recommendationReason = "您是数码产品爱好者，对新款手机有较高关注度。",
            targetAudience = listOf("数码发烧友", "游戏玩家", "摄影爱好者"),
            isAd = true
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
            clickCount = 1234,
            brandName = "网易云课堂",
            ctaText = "立即学习",
            aiSummary = "基于您的学习兴趣和职业发展方向，为您推荐优质在线课程。课程由行业专家授课，实战性强。",
            recommendationReason = "您对技能提升有需求，这些课程能帮助您职业发展。",
            targetAudience = listOf("职场人士", "学生党", "终身学习者"),
            isAd = true
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
            clickCount = 1890,
            brandName = "京东智能",
            ctaText = "探索更多",
            aiSummary = "智能家居正在改变生活方式。根据您的家居环境和使用习惯，为您推荐这几款高性价比的智能产品。",
            recommendationReason = "您对科技产品有兴趣，智能家居能让生活更便捷。",
            targetAudience = listOf("科技爱好者", "品质生活追求者", "新房装修"),
            isAd = true
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
            clickCount = 890,
            brandName = "Keep",
            ctaText = "开始健康",
            aiSummary = "健康是最大的财富。基于您的运动数据和健康目标，为您推荐适合的健康产品和运动装备。",
            recommendationReason = "您有运动健身的习惯，这些产品能提升您的运动体验。",
            targetAudience = listOf("健身爱好者", "养生达人", "亚健康人群"),
            isAd = true
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
            clickCount = 2345,
            brandName = "携程旅行",
            ctaText = "规划行程",
            aiSummary = "旅行是最好的放松方式。根据您的出行记录和偏好，为您推荐实用的旅行装备和目的地。",
            recommendationReason = "您热爱旅行，这些装备能让您的旅途更舒适。",
            targetAudience = listOf("旅行爱好者", "户外运动者", "商务出差"),
            isAd = true
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
            clickCount = 4567,
            brandName = "ZARA",
            ctaText = "立即选购",
            aiSummary = "时尚是一种态度。根据您的穿搭风格和浏览记录，为您推荐最新上市的女装系列。",
            recommendationReason = "您关注时尚穿搭，这些新品符合您的审美。",
            targetAudience = listOf("时尚女性", "职场白领", "学生党"),
            isAd = true
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
            clickCount = 6789,
            brandName = "丝芙兰",
            ctaText = "美妆选购",
            aiSummary = "美丽从护肤开始。基于您的肤质和美妆偏好，为您精选大牌美妆产品，正品保障，价格优惠。",
            recommendationReason = "您对美妆护肤有需求，这些产品口碑好，性价比高。",
            targetAudience = listOf("美妆爱好者", "护肤达人", "送礼需求"),
            isAd = true
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
            clickCount = 2345,
            brandName = "耐克",
            ctaText = "运动选购",
            aiSummary = "运动让生活更美好。根据您的运动类型和频率，为您推荐专业的运动装备，提升运动表现。",
            recommendationReason = "您有运动习惯，专业装备能保护身体，提升效果。",
            targetAudience = listOf("运动爱好者", "健身达人", "户外运动者"),
            isAd = true
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
            clickCount = 3456,
            brandName = "孩子王",
            ctaText = "母婴选购",
            aiSummary = "宝宝的健康是父母最大的心愿。根据宝宝年龄和需求，为您推荐安全、优质的母婴产品。",
            recommendationReason = "您有育儿需求，这些产品安全可靠，口碑好。",
            targetAudience = listOf("新手爸妈", "准父母", "送礼需求"),
            isAd = true
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
            clickCount = 1567,
            brandName = "品胜",
            ctaText = "配件选购",
            aiSummary = "好的配件提升使用体验。根据您使用的设备型号，为您推荐兼容、高品质的数码配件。",
            recommendationReason = "您有数码设备，这些配件能提升使用体验。",
            targetAudience = listOf("数码用户", "学生党", "办公人士"),
            isAd = true
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
            clickCount = 2890,
            brandName = "MUJI",
            ctaText = "家居选购",
            aiSummary = "家是心灵的港湾。根据您的家居风格和需求，为您推荐舒适、美观的家居家纺产品。",
            recommendationReason = "您注重生活品质，这些产品能提升居家舒适度。",
            targetAudience = listOf("品质生活追求者", "新房装修", "换季需求"),
            isAd = true
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
            clickCount = 5678,
            brandName = "大众点评",
            ctaText = "查看餐厅",
            aiSummary = "美食是最好的慰藉。根据您的口味偏好和位置，为您推荐周边高评分餐厅，新用户有专属优惠。",
            recommendationReason = "您喜欢美食探索，这些餐厅评分高，口碑好。",
            targetAudience = listOf("美食爱好者", "吃货", "聚餐需求"),
            isAd = true
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
            clickCount = 3456,
            brandName = "天鹅到家",
            ctaText = "预约服务",
            aiSummary = "专业的事交给专业的人。根据您的家庭需求和服务评价，为您推荐可靠的家政服务人员。",
            recommendationReason = "您有家政服务需求，这些服务人员经过认证，服务有保障。",
            targetAudience = listOf("双职工家庭", "老年人家庭", "新生儿家庭"),
            isAd = true
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
            clickCount = 2345,
            brandName = "超级猩猩",
            ctaText = "预约体验",
            aiSummary = "运动是最好的投资。根据您的健身目标和时间安排，为您推荐适合的瑜伽课程。",
            recommendationReason = "您有健身需求，瑜伽能帮助您放松身心，塑造体形。",
            targetAudience = listOf("上班族", "健身爱好者", "减压需求"),
            isAd = true
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
            clickCount = 1890,
            brandName = "途虎养车",
            ctaText = "预约保养",
            aiSummary = "爱车需要定期保养。根据您的车型和行驶里程，为您推荐适合的保养套餐和维修服务。",
            recommendationReason = "您有汽车，定期保养能延长车辆寿命，保障行车安全。",
            targetAudience = listOf("车主", "新手司机", "二手车车主"),
            isAd = true
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
            clickCount = 3456,
            brandName = "萌宠之家",
            ctaText = "宠物服务",
            aiSummary = "宠物是家人。根据您的宠物种类和需求，为您推荐专业的宠物服务，让毛孩享受最好的照顾。",
            recommendationReason = "您有宠物，这些服务能让您的毛孩更健康快乐。",
            targetAudience = listOf("宠物主人", "爱宠人士", "出差寄养需求"),
            isAd = true
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
            clickCount = 4567,
            brandName = "花加",
            ctaText = "送花表达",
            aiSummary = "鲜花是最好的礼物。根据送花场景和对象，为您推荐合适的花束，快速送达，传递心意。",
            recommendationReason = "您有送花需求，这些花束设计精美，配送快速。",
            targetAudience = listOf("情侣", "送礼需求", "节日庆祝"),
            isAd = true
        )
    )
}
