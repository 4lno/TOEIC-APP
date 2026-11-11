package com.luudev.vocalens.domain.model

data class Dashboard(
    val learnerName: String,
    val initials: String,
    val streakDays: Int,
    val completedWords: Int,
    val targetWords: Int,
    val studyMinutes: Int,
    val pendingWords: Int,
    val courseProgress: Float,
    val recentActivityMinutes: Int,
    val earnedXp: Int
) {
    val dailyProgress: Float
        get() = if (targetWords > 0) (completedWords.toFloat() / targetWords).coerceIn(0f, 1f) else 0f
    val remainingWords: Int get() = (targetWords - completedWords).coerceAtLeast(0)
}
