package com.luudev.vocalens.presentation.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luudev.vocalens.domain.model.AuthError
import com.luudev.vocalens.domain.model.AuthException
import com.luudev.vocalens.domain.usecase.RegisterUseCase
import kotlinx.coroutines.launch
import com.luudev.vocalens.domain.usecase.RegistrationValidation
import com.luudev.vocalens.domain.usecase.ValidateRegistrationInput

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val validation: RegistrationValidation = RegistrationValidation(false, false, false, false),
    val error: AuthError? = null,
    val isLoading: Boolean = false,
    val registered: Boolean = false
)

class RegisterViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val register: RegisterUseCase
) : ViewModel() {
    private val validateRegistrationInput = ValidateRegistrationInput()
    // Passwords are kept in memory, never in SavedStateHandle.
    var uiState by mutableStateOf(RegisterUiState(
        fullName = savedStateHandle["register_name"] ?: "",
        email = savedStateHandle["register_email"] ?: ""
    ))
        private set

    fun onNameChange(fullName: String) {
        if (uiState.isLoading) return
        savedStateHandle["register_name"] = fullName
        uiState = uiState.copy(fullName = fullName,
            validation = uiState.validation.copy(invalidName = false))
    }

    fun onEmailChange(email: String) {
        if (uiState.isLoading) return
        savedStateHandle["register_email"] = email
        uiState = uiState.copy(email = email,
            validation = uiState.validation.copy(invalidEmail = false))
    }

    fun onPasswordChange(password: String) {
        if (uiState.isLoading) return
        uiState = uiState.copy(password = password,
            validation = uiState.validation.copy(
                invalidPassword = false,
                invalidConfirmation = uiState.validation.invalidConfirmation && password != uiState.confirmPassword
            ))
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        if (uiState.isLoading) return
        uiState = uiState.copy(confirmPassword = confirmPassword,
            validation = uiState.validation.copy(invalidConfirmation = false))
    }

    fun onTogglePassword() {
        uiState = uiState.copy(passwordVisible = !uiState.passwordVisible)
    }

    fun onCreateAccountClick() {
        if (uiState.isLoading) return
        val validation = validateRegistrationInput(
            uiState.fullName, uiState.email, uiState.password, uiState.confirmPassword
        )
        uiState = uiState.copy(validation = validation, error = null)
        if (!validation.isValid) return
        val input = uiState
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            try {
                register(input.fullName, input.email, input.password)
                uiState = uiState.copy(registered = true, password = "", confirmPassword = "", passwordVisible = false)
            } catch (error: AuthException) {
                uiState = uiState.copy(error = error.error)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun onNoticeDismissed() {
        uiState = uiState.copy(error = null)
    }

    fun clearPasswords() {
        uiState = uiState.copy(password = "", confirmPassword = "", passwordVisible = false,
            validation = RegistrationValidation(false, false, false, false), error = null, registered = false)
    }
}
