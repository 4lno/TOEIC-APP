package com.luudev.vocalens.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.luudev.vocalens.domain.usecase.ValidateLoginInput

enum class LoginNotice { PasswordResetUnavailable }

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val invalidEmail: Boolean = false,
    val invalidPassword: Boolean = false,
    val notice: LoginNotice? = null,
    val showRegistration: Boolean = false,
    val showHome: Boolean = false
)

class LoginViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val validateLoginInput = ValidateLoginInput()
    // Restore email only; passwords stay in memory and out of saved state.
    var uiState by mutableStateOf(LoginUiState(
        email = savedStateHandle["login_email"] ?: "",
        showRegistration = savedStateHandle["login_show_registration"] ?: false,
        showHome = savedStateHandle["login_show_home"] ?: false
    ))
        private set

    fun onEmailChange(email: String) {
        savedStateHandle["login_email"] = email
        uiState = uiState.copy(email = email, invalidEmail = false)
    }

    fun onPasswordChange(password: String) {
        uiState = uiState.copy(password = password, invalidPassword = false)
    }

    fun onTogglePassword() {
        uiState = uiState.copy(passwordVisible = !uiState.passwordVisible)
    }

    fun onLoginClick() {
        val validation = validateLoginInput(uiState.email, uiState.password)
        // Prototype navigation only. Replace this gate with a successful auth use case when connected.
        savedStateHandle["login_show_home"] = validation.isValid
        uiState = uiState.copy(
            invalidEmail = validation.invalidEmail,
            invalidPassword = validation.invalidPassword,
            notice = null,
            showHome = validation.isValid,
            password = if (validation.isValid) "" else uiState.password,
            passwordVisible = false
        )
    }

    fun onForgotPasswordClick() {
        uiState = uiState.copy(notice = LoginNotice.PasswordResetUnavailable)
    }

    fun onRegisterClick() {
        savedStateHandle["login_show_registration"] = true
        uiState = uiState.copy(showRegistration = true, notice = null)
    }

    fun onBackFromRegistration() {
        savedStateHandle["login_show_registration"] = false
        uiState = uiState.copy(showRegistration = false)
    }

    fun onNoticeDismissed() {
        uiState = uiState.copy(notice = null)
    }

    fun onLogout() {
        savedStateHandle["login_show_home"] = false
        savedStateHandle["login_show_registration"] = false
        uiState = LoginUiState(email = uiState.email)
    }
}
