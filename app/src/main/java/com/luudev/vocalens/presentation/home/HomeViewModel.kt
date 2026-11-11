package com.luudev.vocalens.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.luudev.vocalens.domain.model.Dashboard
import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.domain.usecase.GetDashboard

enum class HomeAction { Notifications, Profile, Progress, Flashcards, Camera, Listening, Pronunciation, Courses, Vocabulary, Practice }

data class HomeUiState(val dashboard: Dashboard, val selectedAction: HomeAction? = null)

class HomeViewModel(getDashboard: GetDashboard) : ViewModel() {
    var uiState by mutableStateOf(HomeUiState(getDashboard()))
        private set

    fun onAction(action: HomeAction) {
        uiState = uiState.copy(selectedAction = action)
    }

    fun onDismissAction() {
        uiState = uiState.copy(selectedAction = null)
    }

    fun onProfileUpdated(profile: UserProfile) {
        uiState = uiState.copy(dashboard = uiState.dashboard.copy(
            learnerName = profile.shortName, initials = profile.initials, targetWords = profile.dailyGoal
        ))
    }
}
