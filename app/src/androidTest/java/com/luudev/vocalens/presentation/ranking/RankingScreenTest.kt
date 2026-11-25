package com.luudev.vocalens.presentation.ranking

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import com.luudev.vocalens.domain.model.RankingEntry
import com.luudev.vocalens.domain.model.RankingSnapshot
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RankingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun floatingTabsSwitchBetweenRankingAndPersonalXpWithoutRequestingSync() {
        var syncs = 0
        val state = RankingUiState(userId = "me", ranking = RankingSnapshot(
            myTotalXp = 1250, entries = listOf(
                RankingEntry("a", "Minh Anh", 2400, 1),
                RankingEntry("b", "Hoàng Nam", 1800, 2),
                RankingEntry("me", "Trương Lưu", 1250, 3),
                RankingEntry("c", "Khánh Linh", 980, 4)
            )
        ))
        compose.setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                RankingScreen(state, { syncs++ }, {})
            }
        }
        compose.onNodeWithTag("ranking_tab_leaderboard").assertIsSelected()
        compose.onNodeWithTag("ranking_tab_personal").performClick().assertIsSelected()
        compose.onNodeWithText("XP của bạn").assertIsDisplayed()
        compose.onNodeWithText("1250 XP").assertIsDisplayed()
        compose.onNodeWithTag("ranking_tab_leaderboard").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(0, syncs) }
    }

    @Test fun showsConfirmedXpCurrentUserAndPendingSessions() {
        compose.setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                RankingScreen(RankingUiState(userId = "A", ranking = RankingSnapshot(
                    entries = listOf(RankingEntry("A", "An", 50, 1)), myTotalXp = 50,
                    pendingSessions = 2)), {}, {})
            }
        }
        compose.onNodeWithText("2 lượt ôn đang chờ đồng bộ XP.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ranking_tab_personal").performClick()
        compose.onNodeWithText("50 XP").assertIsDisplayed()
        compose.onNodeWithText("Bạn đang ở hạng #1 · Top 50").assertIsDisplayed()
    }

    @Test fun failureOffersRetryAndBack() {
        var retries = 0
        var backs = 0
        compose.setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                RankingScreen(RankingUiState(error = true), { retries++ }, { backs++ })
            }
        }
        compose.onNodeWithText("Đồng bộ").performClick()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            assertEquals(1, backs)
        }
    }
}
