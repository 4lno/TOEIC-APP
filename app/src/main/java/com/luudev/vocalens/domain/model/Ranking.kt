package com.luudev.vocalens.domain.model

data class RankingEntry(val userId: String, val displayName: String, val totalXp: Long, val rank: Int = 0)

data class RankingSnapshot(
    val entries: List<RankingEntry> = emptyList(),
    val myTotalXp: Long = 0,
    val fromCache: Boolean = false,
    val pendingSessions: Int = 0,
    val rejectedSessions: Int = 0
)

data class CompletedFlashcardSession(
    val sessionId: String, val startedAt: Long, val endedAt: Long,
    val activeDurationMs: Long, val reviews: List<CompletedFlashcardReview>
)

data class CompletedFlashcardReview(val wordId: String, val rating: FlashcardRating, val reviewedAt: Long)

class XpSubmissionRejected(val code: String) : Exception(code)
