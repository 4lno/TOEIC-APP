package com.luudev.vocalens.presentation.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.luudev.vocalens.domain.usecase.RegistrationValidation
import com.luudev.vocalens.domain.usecase.ValidateRegistrationInput

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val validation: RegistrationValidation = RegistrationValidation(false, false, false, false),
    val showServiceUnavailable: Boolean = false
)

class RegisterViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val validateRegistrationInput = ValidateRegistrationInput()
    // Passwords are kept in memory, never in SavedStateHandle.
    var uiState by mutableStateOf(RegisterUiState(
        fullName = savedStateHandle["register_name"] ?: "",
        email = savedStateHandle["register_email"] ?: ""
    ))
        private set

    fun onNameChange(fullName: String) {
        savedStateHandle["register_name"] = fullName
        uiState = uiState.copy(fullName = fullName,
            validation = uiState.validation.copy(invalidName = false))
    }

    fun onEmailChange(email: String) {
        savedStateHandle["register_email"] = email
        uiState = uiState.copy(email = email,
            validation = uiState.validation.copy(invalidEmail = false))
    }

    fun onPasswordChange(password: String) {
        uiState = uiState.copy(password = password,
            validation = uiState.validation.copy(
                invalidPassword = false,
                invalidConfirmation = uiState.validation.invalidConfirmation && password != uiState.confirmPassword
            ))
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        uiState = uiState.copy(confirmPassword = confirmPassword,
            validation = uiState.validation.copy(invalidConfirmation = false))
    }

    fun onTogglePassword() {
        uiState = uiState.copy(passwordVisible = !uiState.passwordVisible)
    }

    fun onCreateAccountClick() {
        val validation = validateRegistrationInput(
            uiState.fullName, uiState.email, uiState.password, uiState.confirmPassword
        )
        uiState = uiState.copy(validation = validation, showServiceUnavailable = validation.isValid)
    }

    fun onNoticeDismissed() {
        uiState = uiState.copy(showServiceUnavailable = false)
    }

    fun clearPasswords() {
        uiState = uiState.copy(password = "", confirmPassword = "", passwordVisible = false,
            validation = RegistrationValidation(false, false, false, false), showServiceUnavailable = false)
    }
}
