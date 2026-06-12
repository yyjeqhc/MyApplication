package com.example.myapplication

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.LocalAnalyticsStore
import com.example.myapplication.data.MockAdRepository
import com.example.myapplication.model.AdCardType
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdInteractionDataTest {

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        clearInteractionState()
    }

    @After
    fun tearDown() {
        clearInteractionState()
    }

    @Test
    fun interactionStatsPersistAfterStoreRecreate() {
        val baseAd = testAd()
        val store = LocalAnalyticsStore(context)

        store.updateLike(baseAd.id, liked = true, likeDeltaChange = 1)
        store.updateFavorite(baseAd.id, favorited = true)
        store.incrementShare(baseAd.id)
        store.incrementClick(baseAd.id)
        store.incrementExposure(baseAd.id)

        val recreatedStore = LocalAnalyticsStore(context)
        val updatedAd = recreatedStore.applyTo(baseAd)

        assertTrue(updatedAd.liked)
        assertTrue(updatedAd.favorited)
        assertEquals(baseAd.likeCount + 1, updatedAd.likeCount)
        assertEquals(baseAd.shareCount + 1, updatedAd.shareCount)
        assertEquals(baseAd.clickCount + 1, updatedAd.clickCount)
        assertEquals(baseAd.exposureCount + 1, updatedAd.exposureCount)
    }

    @Test
    fun repositoryReturnsUpdatedAdAfterInteraction() {
        val baseAd = firstMutableAd()
        val expectedLiked = !baseAd.liked
        val expectedLikeCount = if (expectedLiked) {
            baseAd.likeCount + 1
        } else {
            (baseAd.likeCount - 1).coerceAtLeast(0)
        }
        val expectedFavorited = !baseAd.favorited

        MockAdRepository.toggleLike(baseAd.id)
        MockAdRepository.toggleFavorite(baseAd.id)
        MockAdRepository.recordShare(baseAd.id)
        MockAdRepository.recordClick(baseAd.id)
        MockAdRepository.recordExposure(baseAd.id)
        MockAdRepository.recordExposure(baseAd.id)

        val updatedAd = requireNotNull(MockAdRepository.getAdById(baseAd.id))
        assertEquals(expectedLiked, updatedAd.liked)
        assertEquals(expectedLikeCount, updatedAd.likeCount)
        assertEquals(expectedFavorited, updatedAd.favorited)
        assertEquals(baseAd.shareCount + 1, updatedAd.shareCount)
        assertEquals(baseAd.clickCount + 1, updatedAd.clickCount)
        assertEquals(baseAd.exposureCount + 1, updatedAd.exposureCount)

        MockAdRepository.reset(context)
        val reloadedAd = requireNotNull(MockAdRepository.getAdById(baseAd.id))
        assertEquals(expectedLiked, reloadedAd.liked)
        assertEquals(expectedLikeCount, reloadedAd.likeCount)
        assertEquals(expectedFavorited, reloadedAd.favorited)
        assertEquals(baseAd.shareCount + 1, reloadedAd.shareCount)
        assertEquals(baseAd.clickCount + 1, reloadedAd.clickCount)
        assertEquals(baseAd.exposureCount + 1, reloadedAd.exposureCount)
    }

    private fun clearInteractionState() {
        LocalAnalyticsStore(context).clear()
        MockAdRepository.clearLocalAnalytics(context)
    }

    private fun firstMutableAd(): AdItem {
        val ad = MockAdRepository.ads.firstOrNull { !it.liked && !it.favorited }
            ?: MockAdRepository.ads.firstOrNull()
        assertNotNull("Expected mock ads to be loaded", ad)
        return requireNotNull(ad)
    }

    private fun testAd(): AdItem {
        return AdItem(
            id = "android_test_store_ad",
            title = "Test Ad",
            subtitle = "Test subtitle",
            channel = AdChannel.FEATURED,
            cardType = AdCardType.SMALL_IMAGE,
            imageUrl = "",
            videoUrl = "",
            tags = listOf("测试"),
            summary = "Test summary",
            liked = false,
            favorited = false,
            likeCount = 10,
            exposureCount = 20,
            clickCount = 3,
            shareCount = 4
        )
    }
}
