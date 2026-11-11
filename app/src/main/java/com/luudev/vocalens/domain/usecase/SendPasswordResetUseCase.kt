package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.AuthRepository

class SendPasswordResetUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String) = repository.sendPasswordReset(email.trim())
}
