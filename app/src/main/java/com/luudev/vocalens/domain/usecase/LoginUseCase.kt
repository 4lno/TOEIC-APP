package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) = repository.login(email.trim(), password)
}
