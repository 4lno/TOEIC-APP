package com.luudev.vocalens.domain.usecase

/** XP for a completed flashcard session, regardless of self-assessed ratings. */
class CalculateFlashcardXp {
    operator fun invoke(reviewedCardCount: Int, completed: Boolean): Int {
        require(reviewedCardCount >= 0)
        return if (completed) Math.multiplyExact(reviewedCardCount, XP_PER_CARD) else 0
    }

    private companion object {
        const val XP_PER_CARD = 5
    }
}
