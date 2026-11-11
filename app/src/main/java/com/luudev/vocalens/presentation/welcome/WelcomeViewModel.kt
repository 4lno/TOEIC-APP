package com.luudev.vocalens.presentation.welcome

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

data class WelcomeUiState(val showLogin: Boolean = false)

// Presentation state only. Camera and AI use cases will belong in the domain layer.
class WelcomeViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    var uiState by mutableStateOf(WelcomeUiState(savedStateHandle[LOGIN_VISIBLE] ?: false))
        private set

    fun onStartClick() = setLoginVisible(true)
    fun onBackToWelcome() = setLoginVisible(false)

    private fun setLoginVisible(visible: Boolean) {
        savedStateHandle[LOGIN_VISIBLE] = visible
        uiState = uiState.copy(showLogin = visible)
    }

    private companion object {
        const val LOGIN_VISIBLE = "welcome_login_visible"
    }
}
