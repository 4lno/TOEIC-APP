package com.luudev.vocalens.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardTest {
    private fun dashboard(completed: Int, target: Int) = Dashboard(
        "Minh Anh", "MA", 7, completed, target, 24, 12, 0.72f, 8, 20
    )

    @Test
    fun computesProgressAndRemainingWords() {
        val dashboard = dashboard(17, 25)
        assertEquals(0.68f, dashboard.dailyProgress, 0.001f)
        assertEquals(8, dashboard.remainingWords)
    }

    @Test
    fun progressStaysCompleteWhenLearnerExceedsGoal() {
        val dashboard = dashboard(30, 25)
        assertEquals(1f, dashboard.dailyProgress, 0f)
        assertEquals(0, dashboard.remainingWords)
    }

    @Test
    fun handlesMissingGoalWithoutInvalidProgress() {
        assertEquals(0f, dashboard(17, 0).dailyProgress, 0f)
    }

    @Test
    fun negativeCountsCannotProduceNegativeProgress() {
        assertEquals(0f, dashboard(-1, 25).dailyProgress, 0f)
    }
}
