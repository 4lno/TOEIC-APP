package com.luudev.vocalens.domain.usecase

data class LoginValidation(val invalidEmail: Boolean, val invalidPassword: Boolean) {
    val isValid: Boolean get() = !invalidEmail && !invalidPassword
}

class ValidateLoginInput {
    operator fun invoke(email: String, password: String) = LoginValidation(
        invalidEmail = !isEmailValid(email),
        invalidPassword = password.length < 6
    )

    fun isEmailValid(email: String): Boolean = EMAIL_PATTERN.matches(email.trim())

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
