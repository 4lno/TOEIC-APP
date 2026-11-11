package com.luudev.vocalens.data.repository

import com.luudev.vocalens.domain.model.Dashboard
import com.luudev.vocalens.domain.repository.DashboardRepository

// Static prototype content, not progress recorded by an authenticated account.
class DemoDashboardRepositoryImpl : DashboardRepository {
    override fun getDashboard() = Dashboard(
        learnerName = "Minh Anh", initials = "MA", streakDays = 7,
        completedWords = 17, targetWords = 25, studyMinutes = 24,
        pendingWords = 12, courseProgress = 0.72f,
        recentActivityMinutes = 8, earnedXp = 20
    )
}
