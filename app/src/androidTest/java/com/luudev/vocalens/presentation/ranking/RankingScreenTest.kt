package com.luudev.vocalens.presentation.ranking

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.luudev.vocalens.domain.model.RankingEntry
import com.luudev.vocalens.domain.model.RankingSnapshot
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RankingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsConfirmedXpCurrentUserAndPendingSessions() {
        compose.setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                RankingScreen(RankingUiState(userId = "A", ranking = RankingSnapshot(
                    entries = listOf(RankingEntry("A", "An", 50, 1)), myTotalXp = 50,
                    pendingSessions = 2)), {}, {})
            }
        }
        compose.onNodeWithText("An (Bạn)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("#1").assertIsDisplayed()
        compose.onNodeWithText("2 lượt ôn đang chờ đồng bộ XP.").performScrollTo().assertIsDisplayed()
    }

    @Test fun failureOffersRetryAndBack() {
        var retry = false
        var back = false
        compose.setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                RankingScreen(RankingUiState(error = true), { retry = true }, { back = true })
            }
        }
        compose.onNodeWithText("Thử lại / Đồng bộ").performClick()
        compose.onNodeWithText("Quay lại").performClick()
        assertTrue(retry)
        assertTrue(back)
    }
}
