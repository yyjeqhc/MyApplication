package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.AdItem

class LocalAnalyticsStore(context: Context) {

    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun applyTo(ad: AdItem): AdItem {
        val stats = getStats(ad.id)
        return ad.copy(
            exposureCount = (ad.exposureCount + stats.exposureDelta).coerceAtLeast(0),
            clickCount = (ad.clickCount + stats.clickDelta).coerceAtLeast(0),
            likeCount = (ad.likeCount + stats.likeDelta).coerceAtLeast(0),
            shareCount = (ad.shareCount + stats.shareDelta).coerceAtLeast(0),
            liked = stats.liked ?: ad.liked,
            favorited = stats.favorited ?: ad.favorited
        )
    }

    fun incrementExposure(adId: String) {
        incrementInt(keyFor(adId, KEY_EXPOSURE_DELTA), 1)
    }

    fun incrementClick(adId: String) {
        incrementInt(keyFor(adId, KEY_CLICK_DELTA), 1)
    }

    fun incrementShare(adId: String) {
        incrementInt(keyFor(adId, KEY_SHARE_DELTA), 1)
    }

    fun updateLike(adId: String, liked: Boolean, likeDeltaChange: Int) {
        preferences.edit()
            .putBoolean(keyFor(adId, KEY_LIKED), liked)
            .putInt(
                keyFor(adId, KEY_LIKE_DELTA),
                getInt(adId, KEY_LIKE_DELTA) + likeDeltaChange
            )
            .apply()
    }

    fun updateFavorite(adId: String, favorited: Boolean) {
        preferences.edit()
            .putBoolean(keyFor(adId, KEY_FAVORITED), favorited)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().commit()
    }

    private fun getStats(adId: String): LocalAnalyticsStats {
        return LocalAnalyticsStats(
            exposureDelta = getInt(adId, KEY_EXPOSURE_DELTA),
            clickDelta = getInt(adId, KEY_CLICK_DELTA),
            likeDelta = getInt(adId, KEY_LIKE_DELTA),
            shareDelta = getInt(adId, KEY_SHARE_DELTA),
            liked = getOptionalBoolean(adId, KEY_LIKED),
            favorited = getOptionalBoolean(adId, KEY_FAVORITED)
        )
    }

    private fun incrementInt(key: String, amount: Int) {
        preferences.edit()
            .putInt(key, preferences.getInt(key, 0) + amount)
            .apply()
    }

    private fun getInt(adId: String, suffix: String): Int {
        return preferences.getInt(keyFor(adId, suffix), 0)
    }

    private fun getOptionalBoolean(adId: String, suffix: String): Boolean? {
        val key = keyFor(adId, suffix)
        return if (preferences.contains(key)) preferences.getBoolean(key, false) else null
    }

    private fun keyFor(adId: String, suffix: String): String = "$adId.$suffix"

    private data class LocalAnalyticsStats(
        val exposureDelta: Int = 0,
        val clickDelta: Int = 0,
        val likeDelta: Int = 0,
        val shareDelta: Int = 0,
        val liked: Boolean? = null,
        val favorited: Boolean? = null
    )

    private companion object {
        const val PREF_NAME = "local_analytics_stats"
        const val KEY_EXPOSURE_DELTA = "exposure_delta"
        const val KEY_CLICK_DELTA = "click_delta"
        const val KEY_LIKE_DELTA = "like_delta"
        const val KEY_SHARE_DELTA = "share_delta"
        const val KEY_LIKED = "liked"
        const val KEY_FAVORITED = "favorited"
    }
}
