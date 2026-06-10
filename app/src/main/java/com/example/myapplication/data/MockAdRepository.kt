package com.example.myapplication.data

import android.content.Context
import android.util.Log
import com.example.myapplication.model.AdCardType
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem
import com.example.myapplication.model.AdStatsOverview
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mock 广告数据仓库
 * 从 assets/mock_ads.json 读取本地模拟数据，并合并本机持久化的互动增量。
 */
object MockAdRepository {

    private const val TAG = "MockAdRepository"
    private const val ASSET_FILE_NAME = "mock_ads.json"

    /** 内部可变广告列表，作为点赞/收藏等当前状态源。 */
    private val _ads = mutableListOf<AdItem>()

    /** 本次 App 进程内已计曝光的广告，避免上下滑动重复累计。 */
    private val sessionExposedAdIds = mutableSetOf<String>()

    /** 本机持久化统计，mock_ads.json 仍保持只读。 */
    private var analyticsStore: LocalAnalyticsStore? = null

    /** 不可变广告列表（外部访问） */
    val ads: List<AdItem> get() = _ads.toList()

    private var initialized = false
    private var loadedFromAssets = false

    /**
     * 使用 Application Context 初始化数据。若 assets 读取或解析失败，会回退到少量内置广告。
     */
    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return

        analyticsStore = LocalAnalyticsStore(context)
        val parsedAds = loadAdsFromAssets(context)
        replaceAds(parsedAds.ifEmpty { defaultAds() }, fromAssets = parsedAds.isNotEmpty())
        initialized = true
    }

    /**
     * 当前是否成功使用 assets JSON 作为主数据源。
     */
    fun isLoadedFromAssets(): Boolean = loadedFromAssets

    /**
     * 根据频道分页获取广告。分页直接从完整数据切片，不动态生成重复广告。
     */
    fun getPagedAds(
        channel: AdChannel,
        page: Int,
        pageSize: Int
    ): PagedAds {
        ensureInitialized()

        val channelAds = _ads.filter { it.channel == channel }
        val safePage = page.coerceAtLeast(1)
        val fromIndex = (safePage - 1) * pageSize
        if (fromIndex >= channelAds.size) {
            return PagedAds(
                ads = emptyList(),
                currentPage = safePage,
                hasMore = false,
                totalCount = channelAds.size
            )
        }

        val toIndex = (fromIndex + pageSize).coerceAtMost(channelAds.size)
        return PagedAds(
            ads = channelAds.subList(fromIndex, toIndex),
            currentPage = safePage,
            hasMore = toIndex < channelAds.size,
            totalCount = channelAds.size
        )
    }

    /**
     * 兼容旧调用：默认返回第一页。
     */
    fun getAdsByChannel(
        channel: AdChannel,
        page: Int = 1,
        pageSize: Int = 6
    ): List<AdItem> {
        return getPagedAds(channel, page, pageSize).ads
    }

    /**
     * 根据 ID 获取广告（返回最新状态）。
     */
    fun getAdById(id: String): AdItem? {
        ensureInitialized()
        return _ads.find { it.id == id }
    }

    /**
     * 记录曝光。
     *
     * 简化口径：广告第一次进入 LazyColumn 可见范围即计 1 次曝光；
     * 本 App 会话内同一广告只计一次，不做 50% 可见和 1 秒停留判断。
     */
    fun recordExposure(adId: String): AdItem? {
        ensureInitialized()
        if (adId in sessionExposedAdIds) return null

        val updatedAd = updateAd(adId) { ad ->
            ad.copy(exposureCount = ad.exposureCount + 1)
        }
        if (updatedAd != null) {
            sessionExposedAdIds.add(adId)
            analyticsStore?.incrementExposure(adId)
        }
        return updatedAd
    }

    /**
     * 记录点击。
     */
    fun recordClick(adId: String): AdItem? {
        ensureInitialized()
        val updatedAd = updateAd(adId) { ad ->
            ad.copy(clickCount = ad.clickCount + 1)
        }
        if (updatedAd != null) {
            analyticsStore?.incrementClick(adId)
        }
        return updatedAd
    }

    /**
     * 记录分享。
     */
    fun recordShare(adId: String): AdItem? {
        ensureInitialized()
        val updatedAd = updateAd(adId) { ad ->
            ad.copy(shareCount = ad.shareCount + 1)
        }
        if (updatedAd != null) {
            analyticsStore?.incrementShare(adId)
        }
        return updatedAd
    }

    /**
     * 切换点赞状态。
     */
    fun toggleLike(adId: String) {
        ensureInitialized()
        var likeDeltaChange = 0
        val updatedAd = updateAd(adId) { ad ->
            val newLiked = !ad.liked
            likeDeltaChange = if (newLiked) {
                1
            } else if (ad.likeCount > 0) {
                -1
            } else {
                0
            }
            ad.copy(
                liked = newLiked,
                likeCount = (ad.likeCount + likeDeltaChange).coerceAtLeast(0)
            )
        }
        if (updatedAd != null) {
            analyticsStore?.updateLike(adId, updatedAd.liked, likeDeltaChange)
        }
    }

    /**
     * 切换收藏状态。
     */
    fun toggleFavorite(adId: String) {
        ensureInitialized()
        val updatedAd = updateAd(adId) { ad ->
            ad.copy(favorited = !ad.favorited)
        }
        if (updatedAd != null) {
            analyticsStore?.updateFavorite(adId, updatedAd.favorited)
        }
    }

    /**
     * 全局本地统计总览，用于隐藏的 Demo 控制面板。
     */
    fun getStatsOverview(): AdStatsOverview {
        ensureInitialized()
        return AdStatsOverview(
            totalExposureCount = _ads.sumOf { it.exposureCount },
            totalClickCount = _ads.sumOf { it.clickCount },
            totalLikeCount = _ads.sumOf { it.likeCount },
            totalFavoriteCount = _ads.count { it.favorited },
            totalShareCount = _ads.sumOf { it.shareCount }
        )
    }

    /**
     * 重置到初始数据。传入 context 时优先重新读取 assets，否则使用 fallback。
     */
    @Synchronized
    fun reset(context: Context? = null) {
        context?.let { analyticsStore = LocalAnalyticsStore(it) }
        val parsedAds = context?.let { loadAdsFromAssets(it) } ?: emptyList()
        replaceAds(parsedAds.ifEmpty { defaultAds() }, fromAssets = parsedAds.isNotEmpty())
        initialized = true
    }

    /**
     * 清空本机持久化统计，并恢复到 mock_ads.json 的初始统计与状态。
     */
    @Synchronized
    fun clearLocalAnalytics(context: Context) {
        analyticsStore = LocalAnalyticsStore(context).also { it.clear() }
        val parsedAds = loadAdsFromAssets(context)
        replaceAds(
            newAds = parsedAds.ifEmpty { defaultAds() },
            fromAssets = parsedAds.isNotEmpty(),
            mergeLocalStats = false
        )
        initialized = true
    }

    fun getChannelCount(channel: AdChannel): Int {
        ensureInitialized()
        return _ads.count { it.channel == channel }
    }

    /**
     * 本地规则版自然语言搜索。
     * 规则优先级：标签命中 > 频道词 > 标题/品牌/摘要等字段 contains 命中。
     */
    fun searchAds(query: String): List<AdItem> {
        ensureInitialized()

        val normalizedQuery = query.normalizeForSearch()
        if (normalizedQuery.isBlank()) return emptyList()

        val matchedTags = TAG_POOL.filter { tag ->
            normalizedQuery.contains(tag.normalizeForSearch())
        }
        val matchedChannel = CHANNEL_KEYWORDS.firstNotNullOfOrNull { (channel, keywords) ->
            channel.takeIf {
                keywords.any { keyword -> normalizedQuery.contains(keyword.normalizeForSearch()) }
            }
        }
        val freeKeywords = extractFreeKeywords(normalizedQuery, matchedTags, matchedChannel)

        return _ads
            .mapNotNull { ad ->
                val score = ad.searchScore(
                    matchedTags = matchedTags,
                    matchedChannel = matchedChannel,
                    freeKeywords = freeKeywords
                )
                if (score > 0) ad to score else null
            }
            .sortedWith(
                compareByDescending<Pair<AdItem, Int>> { it.second }
                    .thenByDescending { it.first.likeCount }
                    .thenBy { it.first.id }
            )
            .map { it.first }
    }

    private fun ensureInitialized() {
        if (!initialized) {
            replaceAds(defaultAds(), fromAssets = false)
            initialized = true
        }
    }

    private fun replaceAds(
        newAds: List<AdItem>,
        fromAssets: Boolean,
        mergeLocalStats: Boolean = true
    ) {
        val adsWithLocalStats = if (mergeLocalStats) {
            analyticsStore?.let { store ->
                newAds.map { ad -> store.applyTo(ad) }
            } ?: newAds
        } else {
            newAds
        }

        _ads.clear()
        _ads.addAll(adsWithLocalStats)
        sessionExposedAdIds.clear()
        loadedFromAssets = fromAssets

        val counts = AdChannel.entries.joinToString { channel ->
            "${channel.name}=${_ads.count { it.channel == channel }}"
        }
        Log.i(TAG, "Loaded ${_ads.size} ads from ${if (fromAssets) "assets JSON" else "fallback"}: $counts")
    }

    private fun loadAdsFromAssets(context: Context): List<AdItem> {
        return try {
            val json = context.assets.open(ASSET_FILE_NAME)
                .bufferedReader()
                .use { it.readText() }
            parseAds(JSONArray(json))
        } catch (error: Exception) {
            Log.e(TAG, "Failed to load $ASSET_FILE_NAME, using fallback ads.", error)
            emptyList()
        }
    }

    private fun parseAds(array: JSONArray): List<AdItem> {
        val parsedAds = mutableListOf<AdItem>()
        val seenIds = mutableSetOf<String>()

        for (index in 0 until array.length()) {
            val jsonObject = array.optJSONObject(index) ?: continue
            val ad = runCatching { jsonObject.toAdItem() }
                .onFailure { Log.w(TAG, "Skip invalid ad at index $index.", it) }
                .getOrNull()
                ?: continue

            if (ad.id.isBlank() || !seenIds.add(ad.id)) {
                Log.w(TAG, "Skip ad with blank or duplicated id: ${ad.id}")
                continue
            }

            parsedAds.add(ad)
        }

        return parsedAds
    }

    private fun JSONObject.toAdItem(): AdItem {
        val cardType = enumValueOrDefault(
            value = optString("cardType"),
            defaultValue = AdCardType.LARGE_IMAGE
        )
        val videoDuration = optString("videoDuration")
        val aiSummary = optString("aiSummary")
        val description = optString("description")
        val scene = optString("scene")
        val targetAudience = optString("targetAudience")

        return AdItem(
            id = optString("id"),
            title = optString("title"),
            subtitle = optString("subtitle"),
            channel = enumValueOrDefault(
                value = optString("channel"),
                defaultValue = AdChannel.FEATURED
            ),
            cardType = cardType,
            imageUrl = optString("imageUrl"),
            imageAsset = optString("imageAsset"),
            videoUrl = optString(
                "videoUrl",
                if (cardType == AdCardType.VIDEO && videoDuration.isNotBlank()) {
                    "asset://mock/${optString("id")}.mp4"
                } else {
                    ""
                }
            ),
            videoAsset = optString("videoAsset"),
            imageType = optString("imageType"),
            videoDuration = videoDuration,
            tags = optStringArray("tags"),
            summary = description,
            liked = optBoolean("liked", false),
            favorited = optBoolean("favorited", optBoolean("favorite", false)),
            likeCount = optInt("likeCount", 0),
            exposureCount = optInt("exposureCount", 0),
            clickCount = optInt("clickCount", 0),
            shareCount = optInt("shareCount", 0),
            brandName = optString("brand", optString("brandName")),
            ctaText = optString("ctaText", "了解详情"),
            aiSummary = aiSummary,
            category = optString("category"),
            scene = scene,
            recommendationReason = optString(
                "recommendationReason",
                buildRecommendationReason(aiSummary, scene, targetAudience)
            ),
            targetAudience = targetAudience,
            isAd = optBoolean("isAd", true)
        )
    }

    private fun JSONObject.optStringArray(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return List(array.length()) { index -> array.optString(index) }
            .filter { it.isNotBlank() }
    }

    private fun updateAd(adId: String, transform: (AdItem) -> AdItem): AdItem? {
        val index = _ads.indexOfFirst { it.id == adId }
        if (index == -1) return null

        val updatedAd = transform(_ads[index])
        _ads[index] = updatedAd
        return updatedAd
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String,
        defaultValue: T
    ): T {
        return runCatching { enumValueOf<T>(value.trim().uppercase()) }
            .getOrDefault(defaultValue)
    }

    private fun buildRecommendationReason(
        aiSummary: String,
        scene: String,
        targetAudience: String
    ): String {
        val reasonParts = listOf(aiSummary, scene, targetAudience)
            .filter { it.isNotBlank() }
        return if (reasonParts.isEmpty()) {
            ""
        } else {
            reasonParts.joinToString("，") + "，适合作为本次推荐内容。"
        }
    }

    private fun AdItem.searchScore(
        matchedTags: List<String>,
        matchedChannel: AdChannel?,
        freeKeywords: List<String>
    ): Int {
        var score = 0

        if (matchedChannel == channel) {
            score += 5
        }

        matchedTags.forEach { tag ->
            if (tag in tags) {
                score += 10
            } else if (category.contains(tag) || aiSummary.contains(tag) || summary.contains(tag)) {
                score += 3
            }
        }

        freeKeywords.forEach { keyword ->
            score += when {
                brandName.contains(keyword) || title.contains(keyword) -> 6
                tags.any { it.contains(keyword) || keyword.contains(it) } -> 5
                subtitle.contains(keyword) ||
                    aiSummary.contains(keyword) ||
                    category.contains(keyword) ||
                    scene.contains(keyword) ||
                    targetAudience.contains(keyword) -> 4
                summary.contains(keyword) || recommendationReason.contains(keyword) -> 2
                else -> 0
            }
        }

        return score
    }

    private fun extractFreeKeywords(
        normalizedQuery: String,
        matchedTags: List<String>,
        matchedChannel: AdChannel?
    ): List<String> {
        var remaining = normalizedQuery
        matchedTags.forEach { tag ->
            remaining = remaining.replace(tag.normalizeForSearch(), " ")
        }
        matchedChannel?.let { channel ->
            CHANNEL_KEYWORDS[channel]?.forEach { keyword ->
                remaining = remaining.replace(keyword.normalizeForSearch(), " ")
            }
        }
        SEARCH_STOP_WORDS.forEach { stopWord ->
            remaining = remaining.replace(stopWord, " ")
        }

        return remaining
            .split(Regex("[\\s,，。.!！?？、；;：:]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
    }

    private fun String.normalizeForSearch(): String {
        return trim().lowercase()
    }

    private fun defaultAds(): List<AdItem> = listOf(
        AdItem(
            id = "fallback_featured_001",
            title = "精选好物限时推荐",
            subtitle = "本地 fallback 数据",
            channel = AdChannel.FEATURED,
            cardType = AdCardType.LARGE_IMAGE,
            imageUrl = "",
            videoUrl = "",
            tags = listOf("优惠", "性价比", "效率"),
            summary = "当 assets 数据不可用时展示的精选广告，避免应用启动后出现空白页面。",
            liked = false,
            favorited = false,
            likeCount = 128,
            exposureCount = 5200,
            clickCount = 320,
            brandName = "Mock 推荐",
            ctaText = "了解详情",
            aiSummary = "兜底精选广告，保证演示可继续进行。",
            category = "综合推荐",
            scene = "演示兜底",
            recommendationReason = "assets 数据不可用时提供基础内容展示。",
            targetAudience = "演示用户",
            isAd = true
        ),
        AdItem(
            id = "fallback_ecommerce_001",
            title = "电商频道默认广告",
            subtitle = "本地 fallback 数据",
            channel = AdChannel.ECOMMERCE,
            cardType = AdCardType.SMALL_IMAGE,
            imageUrl = "",
            videoUrl = "",
            tags = listOf("数码", "优惠", "学生党"),
            summary = "当 JSON 加载失败时展示的电商频道广告。",
            liked = false,
            favorited = false,
            likeCount = 96,
            exposureCount = 4300,
            clickCount = 210,
            brandName = "Mock 电商",
            ctaText = "立即查看",
            aiSummary = "兜底电商广告，保持频道内容可见。",
            category = "电商",
            scene = "兜底展示",
            recommendationReason = "用于 JSON 不可用时保持电商频道可演示。",
            targetAudience = "电商用户",
            isAd = true
        ),
        AdItem(
            id = "fallback_local_001",
            title = "本地生活默认广告",
            subtitle = "本地 fallback 数据",
            channel = AdChannel.LOCAL,
            cardType = AdCardType.VIDEO,
            imageUrl = "",
            videoUrl = "",
            videoAsset = "ad_videos/video_home_01.mp4",
            imageType = "gradient",
            videoDuration = "00:30",
            tags = listOf("本地生活", "优惠", "效率"),
            summary = "当 JSON 加载失败时展示的本地生活频道广告。",
            liked = false,
            favorited = false,
            likeCount = 88,
            exposureCount = 3900,
            clickCount = 180,
            brandName = "Mock 本地",
            ctaText = "预约服务",
            aiSummary = "兜底本地生活广告，保证视频卡可见。",
            category = "本地生活",
            scene = "兜底展示",
            recommendationReason = "用于 JSON 不可用时保持本地频道可演示。",
            targetAudience = "本地用户",
            isAd = true
        )
    )

    data class PagedAds(
        val ads: List<AdItem>,
        val currentPage: Int,
        val hasMore: Boolean,
        val totalCount: Int
    )

    private val TAG_POOL = listOf(
        "运动",
        "学生党",
        "性价比",
        "数码",
        "美妆",
        "通勤",
        "外卖",
        "家居",
        "游戏",
        "旅游",
        "亲子",
        "本地生活",
        "优惠",
        "轻奢",
        "效率"
    )

    private val CHANNEL_KEYWORDS = mapOf(
        AdChannel.FEATURED to listOf("精选", "推荐"),
        AdChannel.ECOMMERCE to listOf("电商", "购物", "商城"),
        AdChannel.LOCAL to listOf("本地", "附近", "到店", "周边", "本地生活")
    )

    private val SEARCH_STOP_WORDS = listOf(
        "我想看",
        "我想找",
        "有没有",
        "推荐",
        "一些",
        "适合",
        "想看",
        "想找",
        "看看",
        "广告",
        "一个",
        "一下",
        "有",
        "的",
        "和",
        "或",
        "高"
    )
}
