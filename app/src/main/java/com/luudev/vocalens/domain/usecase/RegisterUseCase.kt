package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.AuthRepository

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(fullName: String, email: String, password: String) =
        repository.register(fullName.trim(), email.trim(), password)
}
