package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.AuthRepository

class GetCurrentUserUseCase(private val repository: AuthRepository) {
    operator fun invoke() = repository.currentUser()
}
