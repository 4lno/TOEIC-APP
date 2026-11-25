package com.luudev.vocalens.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateFlashcardXpTest {
    private val calculateFlashcardXp = CalculateFlashcardXp()

    @Test fun completedSessionAwardsFiveXpPerReviewedCard() {
        assertEquals(50, calculateFlashcardXp(10, completed = true))
        assertEquals(0, calculateFlashcardXp(0, completed = true))
    }

    @Test fun unfinishedSessionDoesNotAwardXpEvenWithReviews() {
        assertEquals(0, calculateFlashcardXp(10, completed = false))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeReviewCountIsRejected() {
        calculateFlashcardXp(-1, completed = true)
    }

    @Test(expected = ArithmeticException::class)
    fun overflowingXpCannotBecomeANegativeScore() {
        calculateFlashcardXp(Int.MAX_VALUE, completed = true)
    }
}
