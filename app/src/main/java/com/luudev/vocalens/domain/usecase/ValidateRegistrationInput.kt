package com.luudev.vocalens.domain.usecase

data class RegistrationValidation(
    val invalidName: Boolean,
    val invalidEmail: Boolean,
    val invalidPassword: Boolean,
    val invalidConfirmation: Boolean
) {
    val isValid: Boolean
        get() = !invalidName && !invalidEmail && !invalidPassword && !invalidConfirmation
}

class ValidateRegistrationInput {
    private val validateCredentials = ValidateLoginInput()

    operator fun invoke(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): RegistrationValidation {
        val credentials = validateCredentials(email, password)
        return RegistrationValidation(
            invalidName = fullName.isBlank(),
            invalidEmail = credentials.invalidEmail,
            invalidPassword = credentials.invalidPassword,
            invalidConfirmation = confirmPassword.isEmpty() || confirmPassword != password
        )
    }
}
