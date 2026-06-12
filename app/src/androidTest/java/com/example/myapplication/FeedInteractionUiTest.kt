package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.myapplication.data.LocalAnalyticsStore
import com.example.myapplication.data.MockAdRepository
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedInteractionUiTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        clearInteractionState()
    }

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
        clearInteractionState()
    }

    @Test
    fun feedLikeAndFavoriteRemainAfterRecreate() {
        val baseAd = firstFeaturedPageAd()
        val expectedLiked = !baseAd.liked
        val expectedLikeCount = if (expectedLiked) {
            baseAd.likeCount + 1
        } else {
            (baseAd.likeCount - 1).coerceAtLeast(0)
        }
        val expectedFavorited = !baseAd.favorited

        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForFeedAd(baseAd.id)

        composeRule.onNodeWithTag("feed_list")
            .performScrollToNode(hasTestTag("ad_card_${baseAd.id}"))
        composeRule.onNodeWithTag("like_button_${baseAd.id}").performClick()
        composeRule.waitUntilAd(baseAd.id) { ad ->
            ad.liked == expectedLiked && ad.likeCount == expectedLikeCount
        }

        composeRule.onNodeWithTag("favorite_button_${baseAd.id}").performClick()
        composeRule.waitUntilAd(baseAd.id) { ad -> ad.favorited == expectedFavorited }

        composeRule.onNodeWithTag("share_button_${baseAd.id}").performClick()
        composeRule.waitUntilAd(baseAd.id) { ad -> ad.shareCount == baseAd.shareCount + 1 }

        composeRule.onNodeWithTag("ad_card_${baseAd.id}").performClick()
        composeRule.waitUntilAd(baseAd.id) { ad -> ad.clickCount == baseAd.clickCount + 1 }
        composeRule.onNodeWithContentDescription("返回").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").performClick()

        scenario?.recreate()
        composeRule.waitForFeedAd(baseAd.id)

        val persistedAd = requireNotNull(MockAdRepository.getAdById(baseAd.id))
        assertEquals(expectedLiked, persistedAd.liked)
        assertEquals(expectedLikeCount, persistedAd.likeCount)
        assertEquals(expectedFavorited, persistedAd.favorited)
        assertEquals(baseAd.shareCount + 1, persistedAd.shareCount)
        assertEquals(baseAd.clickCount + 1, persistedAd.clickCount)
        composeRule.onNodeWithTag("like_button_${baseAd.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("favorite_button_${baseAd.id}").assertIsDisplayed()
    }

    private fun clearInteractionState() {
        LocalAnalyticsStore(context).clear()
        MockAdRepository.clearLocalAnalytics(context)
    }

    private fun firstFeaturedPageAd(): AdItem {
        val ad = MockAdRepository
            .getAdsByChannel(AdChannel.FEATURED)
            .firstOrNull { !it.liked && !it.favorited }
            ?: MockAdRepository.getAdsByChannel(AdChannel.FEATURED).firstOrNull()
        assertNotNull("Expected featured mock ads to be loaded", ad)
        return requireNotNull(ad)
    }

    private fun ComposeTestRule.waitForFeedAd(adId: String) {
        waitUntil(timeoutMillis = 10_000) {
            onAllNodesWithTag("ad_card_$adId")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun ComposeTestRule.waitUntilAd(
        adId: String,
        predicate: (AdItem) -> Boolean
    ) {
        waitUntil(timeoutMillis = 5_000) {
            MockAdRepository.getAdById(adId)?.let(predicate) == true
        }
        assertTrue(predicate(requireNotNull(MockAdRepository.getAdById(adId))))
    }
}
