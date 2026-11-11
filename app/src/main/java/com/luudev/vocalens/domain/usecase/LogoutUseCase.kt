package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.AuthRepository

class LogoutUseCase(private val repository: AuthRepository) {
    operator fun invoke() = repository.logout()
}
