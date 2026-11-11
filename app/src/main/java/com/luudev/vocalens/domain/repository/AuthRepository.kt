package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.AuthUser

interface AuthRepository {
    fun currentUser(): AuthUser?
    suspend fun login(email: String, password: String): AuthUser
    suspend fun register(fullName: String, email: String, password: String): AuthUser
    suspend fun sendPasswordReset(email: String)
    fun logout()
}
