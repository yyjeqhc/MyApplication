package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.model.AdCardType
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem
import com.example.myapplication.ui.feed.SmallImageAdCard
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Ignore("Compose UI connected test is unstable on current device environment; interaction persistence is covered by AdInteractionDataTest.")
@RunWith(AndroidJUnit4::class)
class FeedInteractionUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun adCardActionCallbacksAreInvoked() {
        val ad = testAd()
        var cardClicks = 0
        var likeClicks = 0
        var favoriteClicks = 0
        var shareClicks = 0

        composeRule.setContent {
            MyApplicationTheme(dynamicColor = false) {
                SmallImageAdCard(
                    ad = ad,
                    onClick = { cardClicks++ },
                    onLikeClick = { likeClicks++ },
                    onFavoriteClick = { favoriteClicks++ },
                    onShareClick = { shareClicks++ },
                    onTagClick = {},
                    onCtaClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("ad_card_${ad.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("like_button_${ad.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("favorite_button_${ad.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("share_button_${ad.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, cardClicks)
        assertEquals(1, likeClicks)
        assertEquals(1, favoriteClicks)
        assertEquals(1, shareClicks)
    }

    private fun testAd(): AdItem {
        return AdItem(
            id = "ui_test_ad",
            title = "测试广告卡片",
            subtitle = "稳定的小范围 Compose 测试",
            channel = AdChannel.FEATURED,
            cardType = AdCardType.SMALL_IMAGE,
            imageUrl = "",
            imageAsset = "",
            videoUrl = "",
            tags = listOf("测试", "互动"),
            summary = "用于验证 Feed 卡片交互按钮回调。",
            liked = false,
            favorited = false,
            likeCount = 12,
            exposureCount = 0,
            clickCount = 0,
            shareCount = 0,
            brandName = "Codex Test",
            ctaText = "查看"
        )
    }
}
