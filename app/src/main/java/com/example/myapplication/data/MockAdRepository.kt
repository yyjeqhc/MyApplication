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
            tags = listOf("优惠", "性价比", "家居"),
            summary = "炎炎夏日，清凉一夏！精选商品低至3折，数量有限，先到先得。涵盖服饰、家居、数码等多个品类。",
            liked = false,
            favorited = false,
            likeCount = 1234,
            exposureCount = 56789,
            clickCount = 2345,
            brandName = "天猫商城",
            ctaText = "立即抢购",
            aiSummary = "多品类夏日折扣，适合集中补货和换季采购。",
            category = "综合电商",
            scene = "夏季换新",
            recommendationReason = "您最近浏览了多款夏季服饰，本次活动有大量相关商品优惠。",
            targetAudience = "关注折扣、准备换季采购的家庭用户",
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
            tags = listOf("数码", "游戏", "效率", "轻奢"),
            summary = "全新旗舰手机震撼发布！搭载最新处理器，120Hz高刷屏幕，5000mAh大电池，预约即享多重好礼。",
            liked = true,
            favorited = false,
            likeCount = 8901,
            exposureCount = 45678,
            clickCount = 3456,
            brandName = "小米科技",
            ctaText = "预约抢购",
            aiSummary = "旗舰性能和长续航，适合游戏、拍照和高频办公。",
            category = "数码手机",
            scene = "新品首发预约",
            recommendationReason = "您是数码产品爱好者，对新款手机有较高关注度。",
            targetAudience = "数码爱好者、手游玩家和重度手机用户",
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
            tags = listOf("学生党", "效率", "性价比"),
            summary = "海量优质课程限时特惠，涵盖编程、设计、语言、职场等多个领域，助力你的职业发展。",
            liked = false,
            favorited = true,
            likeCount = 567,
            exposureCount = 23456,
            clickCount = 1234,
            brandName = "网易云课堂",
            ctaText = "立即学习",
            aiSummary = "名师课程限时优惠，适合系统补技能和备考。",
            category = "在线教育",
            scene = "学习提升",
            recommendationReason = "您对技能提升有需求，这些课程能帮助您职业发展。",
            targetAudience = "学生党、职场新人和想提升效率的人群",
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
            tags = listOf("家居", "数码", "效率", "轻奢"),
            summary = "智能家居产品大促，智能音箱、扫地机器人、智能灯具等多款爆品，让你的家更智能更舒适。",
            liked = false,
            favorited = false,
            likeCount = 678,
            exposureCount = 34567,
            clickCount = 1890,
            brandName = "京东智能",
            ctaText = "探索更多",
            aiSummary = "智能设备组合优惠，适合打造省心的居家场景。",
            category = "智能家居",
            scene = "家庭焕新",
            recommendationReason = "您对科技产品有兴趣，智能家居能让生活更便捷。",
            targetAudience = "新房装修、科技爱好者和品质生活用户",
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
            tags = listOf("运动", "性价比", "效率"),
            summary = "精选健康好物，包括运动器材、保健品、健康食品等，让你轻松拥有健康生活方式。",
            liked = false,
            favorited = false,
            likeCount = 456,
            exposureCount = 12345,
            clickCount = 890,
            brandName = "Keep",
            ctaText = "开始健康",
            aiSummary = "运动健康好物合集，适合低门槛开始自律生活。",
            category = "运动健康",
            scene = "居家健身",
            recommendationReason = "您有运动健身的习惯，这些产品能提升您的运动体验。",
            targetAudience = "健身入门者、轻运动用户和健康管理人群",
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
            tags = listOf("旅游", "通勤", "轻奢"),
            summary = "精选旅行出行装备，行李箱、背包、旅行配件等多款好物，让你的旅途更轻松愉快。",
            liked = true,
            favorited = true,
            likeCount = 789,
            exposureCount = 45678,
            clickCount = 2345,
            brandName = "携程旅行",
            ctaText = "规划行程",
            aiSummary = "轻便出行装备清单，适合短途旅行和商务差旅。",
            category = "旅行出行",
            scene = "周末出游",
            recommendationReason = "您热爱旅行，这些装备能让您的旅途更舒适。",
            targetAudience = "周末旅行者、通勤族和商务出差人群",
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
            tags = listOf("轻奢", "通勤", "优惠"),
            summary = "最新时尚女装系列，精选优质面料，独特设计，让你成为人群中的焦点。新品上市，限时优惠。",
            liked = false,
            favorited = false,
            likeCount = 2345,
            exposureCount = 67890,
            clickCount = 4567,
            brandName = "ZARA",
            ctaText = "立即选购",
            aiSummary = "通勤新品折扣，适合升级日常穿搭质感。",
            category = "服饰鞋包",
            scene = "职场通勤",
            recommendationReason = "您关注时尚穿搭，这些新品符合您的审美。",
            targetAudience = "职场白领、学生党和关注穿搭的人群",
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
            tags = listOf("美妆", "轻奢", "优惠"),
            summary = "国际大牌美妆护肤产品专场，涵盖口红、粉底、精华、面膜等多品类，正品保障，限时特惠。",
            liked = true,
            favorited = false,
            likeCount = 5678,
            exposureCount = 89012,
            clickCount = 6789,
            brandName = "丝芙兰",
            ctaText = "美妆选购",
            aiSummary = "大牌护肤彩妆降价，适合补齐日常美妆清单。",
            category = "美妆护肤",
            scene = "日常护肤",
            recommendationReason = "您对美妆护肤有需求，这些产品口碑好，性价比高。",
            targetAudience = "美妆爱好者、护肤新手和送礼用户",
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
            tags = listOf("运动", "性价比", "通勤"),
            summary = "专业运动户外装备，跑鞋、运动服、健身器材等，品质保证，让你的运动更专业更舒适。",
            liked = false,
            favorited = true,
            likeCount = 1234,
            exposureCount = 34567,
            clickCount = 2345,
            brandName = "耐克",
            ctaText = "运动选购",
            aiSummary = "跑鞋与训练装备组合，兼顾日常通勤和运动表现。",
            category = "运动户外",
            scene = "训练装备",
            recommendationReason = "您有运动习惯，专业装备能保护身体，提升效果。",
            targetAudience = "运动爱好者、健身人群和日常通勤用户",
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
            tags = listOf("亲子", "家居", "优惠"),
            summary = "精选母婴用品，奶粉、纸尿裤、婴儿服饰、玩具等，品质安全有保障，给宝宝最好的呵护。",
            liked = false,
            favorited = false,
            likeCount = 3456,
            exposureCount = 56789,
            clickCount = 3456,
            brandName = "孩子王",
            ctaText = "母婴选购",
            aiSummary = "母婴刚需用品一站购，适合家庭囤货和送礼。",
            category = "母婴亲子",
            scene = "家庭囤货",
            recommendationReason = "您有育儿需求，这些产品安全可靠，口碑好。",
            targetAudience = "新手爸妈、准父母和亲子家庭",
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
            tags = listOf("数码", "学生党", "性价比", "效率"),
            summary = "数码配件特卖会，耳机、充电器、数据线、手机壳等，品质保证，价格实惠，数量有限。",
            liked = false,
            favorited = false,
            likeCount = 890,
            exposureCount = 23456,
            clickCount = 1567,
            brandName = "品胜",
            ctaText = "配件选购",
            aiSummary = "高频数码配件低价补齐，适合学习和办公场景。",
            category = "数码配件",
            scene = "学习办公",
            recommendationReason = "您有数码设备，这些配件能提升使用体验。",
            targetAudience = "学生党、办公人士和多设备用户",
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
            tags = listOf("家居", "性价比", "优惠"),
            summary = "家居家纺大促活动，床品四件套、毛巾浴巾、窗帘地毯等，品质优良，让你的家更温馨舒适。",
            liked = true,
            favorited = true,
            likeCount = 2345,
            exposureCount = 45678,
            clickCount = 2890,
            brandName = "MUJI",
            ctaText = "家居选购",
            aiSummary = "家纺套装大促，适合换季整理和新家布置。",
            category = "家居家纺",
            scene = "居家焕新",
            recommendationReason = "您注重生活品质，这些产品能提升居家舒适度。",
            targetAudience = "新房装修、租房焕新和品质生活用户",
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
            tags = listOf("本地生活", "外卖", "优惠"),
            summary = "发现周边美食好去处，精选本地特色餐厅，新用户专享优惠，让你的味蕾尽享美味。",
            liked = false,
            favorited = false,
            likeCount = 4567,
            exposureCount = 78901,
            clickCount = 5678,
            brandName = "大众点评",
            ctaText = "查看餐厅",
            aiSummary = "周边高分餐厅优惠，适合聚餐、探店和外卖尝鲜。",
            category = "餐饮美食",
            scene = "周边探店",
            recommendationReason = "您喜欢美食探索，这些餐厅评分高，口碑好。",
            targetAudience = "本地生活用户、聚餐人群和外卖高频用户",
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
            tags = listOf("本地生活", "家居", "效率"),
            summary = "专业家政服务，保洁、保姆、月嫂、维修等，持证上岗，服务保障，让你省心更放心。",
            liked = true,
            favorited = false,
            likeCount = 2345,
            exposureCount = 45678,
            clickCount = 3456,
            brandName = "天鹅到家",
            ctaText = "预约服务",
            aiSummary = "家政上门预约，适合快速解决保洁和维修需求。",
            category = "家政服务",
            scene = "上门服务",
            recommendationReason = "您有家政服务需求，这些服务人员经过认证，服务有保障。",
            targetAudience = "双职工家庭、新手爸妈和独居租房人群",
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
            tags = listOf("运动", "本地生活", "优惠"),
            summary = "本地健身房瑜伽课程特惠，专业教练指导，小班教学，新会员体验课免费预约。",
            liked = false,
            favorited = true,
            likeCount = 1890,
            exposureCount = 34567,
            clickCount = 2345,
            brandName = "超级猩猩",
            ctaText = "预约体验",
            aiSummary = "附近瑜伽体验课优惠，适合下班后放松训练。",
            category = "运动健身",
            scene = "下班减压",
            recommendationReason = "您有健身需求，瑜伽能帮助您放松身心，塑造体形。",
            targetAudience = "通勤上班族、健身新手和减压需求用户",
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
            tags = listOf("本地生活", "通勤", "效率"),
            summary = "专业汽车保养维修服务，原厂配件，技师持证上岗，保养套餐限时特惠，让你的爱车焕然一新。",
            liked = false,
            favorited = false,
            likeCount = 1567,
            exposureCount = 23456,
            clickCount = 1890,
            brandName = "途虎养车",
            ctaText = "预约保养",
            aiSummary = "附近门店保养套餐，适合通勤车主省时预约。",
            category = "汽车服务",
            scene = "车辆保养",
            recommendationReason = "您有汽车，定期保养能延长车辆寿命，保障行车安全。",
            targetAudience = "通勤车主、新手司机和二手车用户",
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
            tags = listOf("本地生活", "家居", "亲子"),
            summary = "宠物洗澡美容、寄养、医疗等一站式服务，专业团队，爱心呵护，让你的毛孩健康快乐。",
            liked = false,
            favorited = false,
            likeCount = 2890,
            exposureCount = 56789,
            clickCount = 3456,
            brandName = "萌宠之家",
            ctaText = "宠物服务",
            aiSummary = "附近宠物护理服务，适合洗护、寄养和临时照看。",
            category = "宠物服务",
            scene = "宠物照护",
            recommendationReason = "您有宠物，这些服务能让您的毛孩更健康快乐。",
            targetAudience = "宠物家庭、出差用户和亲子家庭",
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
            tags = listOf("本地生活", "轻奢", "效率"),
            summary = "本地鲜花速递服务，精选新鲜花材，专业花艺师设计，2小时急速送达，让爱意及时传递。",
            liked = true,
            favorited = true,
            likeCount = 3456,
            exposureCount = 67890,
            clickCount = 4567,
            brandName = "花加",
            ctaText = "送花表达",
            aiSummary = "同城鲜花快速送达，适合纪念日和临时送礼。",
            category = "鲜花礼品",
            scene = "即时送礼",
            recommendationReason = "您有送花需求，这些花束设计精美，配送快速。",
            targetAudience = "情侣、商务送礼和节日庆祝用户",
            isAd = true
        )
    )
}
