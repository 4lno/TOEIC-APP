package com.luudev.vocalens.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luudev.vocalens.domain.model.AuthError
import com.luudev.vocalens.domain.model.AuthException
import com.luudev.vocalens.domain.usecase.GetCurrentUserUseCase
import com.luudev.vocalens.domain.usecase.LoginUseCase
import com.luudev.vocalens.domain.usecase.LogoutUseCase
import com.luudev.vocalens.domain.usecase.SendPasswordResetUseCase
import com.luudev.vocalens.domain.usecase.ValidateLoginInput
import kotlinx.coroutines.launch

enum class LoginNotice { PasswordResetSent }

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val invalidEmail: Boolean = false,
    val invalidPassword: Boolean = false,
    val notice: LoginNotice? = null,
    val error: AuthError? = null,
    val isLoading: Boolean = false,
    val showRegistration: Boolean = false,
    val showHome: Boolean = false
)

class LoginViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val login: LoginUseCase,
    private val sendPasswordReset: SendPasswordResetUseCase,
    private val logout: LogoutUseCase,
    private val getCurrentUser: GetCurrentUserUseCase
) : ViewModel() {
    private val validateLoginInput = ValidateLoginInput()
    // Restore email only; passwords stay in memory and out of saved state.
    var uiState by mutableStateOf(LoginUiState(
        email = savedStateHandle["login_email"] ?: "",
        showRegistration = savedStateHandle["login_show_registration"] ?: false,
        showHome = getCurrentUser() != null
    ))
        private set

    fun onEmailChange(email: String) {
        if (uiState.isLoading) return
        savedStateHandle["login_email"] = email
        uiState = uiState.copy(email = email, invalidEmail = false)
    }

    fun onPasswordChange(password: String) {
        if (uiState.isLoading) return
        uiState = uiState.copy(password = password, invalidPassword = false)
    }

    fun onTogglePassword() {
        uiState = uiState.copy(passwordVisible = !uiState.passwordVisible)
    }

    fun onLoginClick() {
        if (uiState.isLoading) return
        val validation = validateLoginInput(uiState.email, uiState.password)
        uiState = uiState.copy(
            invalidEmail = validation.invalidEmail,
            invalidPassword = validation.invalidPassword,
            notice = null,
            error = null,
            passwordVisible = false
        )
        if (!validation.isValid) return
        val email = uiState.email
        val password = uiState.password
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            try {
                login(email, password)
                uiState = uiState.copy(showHome = true, password = "")
            } catch (error: AuthException) {
                uiState = uiState.copy(error = error.error)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun onForgotPasswordClick() {
        if (uiState.isLoading) return
        if (!validateLoginInput.isEmailValid(uiState.email)) {
            uiState = uiState.copy(invalidEmail = true)
            return
        }
        val email = uiState.email
        uiState = uiState.copy(isLoading = true, notice = null, error = null)
        viewModelScope.launch {
            try {
                sendPasswordReset(email)
                uiState = uiState.copy(notice = LoginNotice.PasswordResetSent)
            } catch (error: AuthException) {
                uiState = uiState.copy(error = error.error)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun onRegisterClick() {
        if (uiState.isLoading) return
        savedStateHandle["login_show_registration"] = true
        uiState = uiState.copy(showRegistration = true, notice = null, error = null, password = "")
    }

    fun onBackFromRegistration() {
        savedStateHandle["login_show_registration"] = false
        uiState = uiState.copy(showRegistration = false)
    }

    fun onNoticeDismissed() {
        uiState = uiState.copy(notice = null, error = null)
    }

    fun onRegistrationSucceeded() {
        savedStateHandle["login_show_registration"] = false
        val user = getCurrentUser()
        savedStateHandle["login_email"] = user?.email.orEmpty()
        uiState = LoginUiState(email = user?.email.orEmpty(), showHome = user != null)
    }

    fun onLogout() {
        logout()
        savedStateHandle["login_show_registration"] = false
        uiState = LoginUiState(email = uiState.email)
    }
}
