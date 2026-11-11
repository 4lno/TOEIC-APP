package com.luudev.vocalens.testing

import com.luudev.vocalens.domain.model.AuthException
import com.luudev.vocalens.domain.model.AuthUser
import com.luudev.vocalens.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred

class FakeAuthRepository : AuthRepository {
    val testUser = AuthUser("test-uid", "learner@example.com", "Learner")
    var user: AuthUser? = null
    var failure: AuthException? = null
    var gate: CompletableDeferred<Unit>? = null
    var loginCalls = 0
    var registerCalls = 0
    var resetCalls = 0
    var lastEmail = ""
    var lastName = ""
    override fun currentUser() = user
    override suspend fun login(email: String, password: String): AuthUser {
        loginCalls++
        lastEmail = email
        gate?.await()
        failure?.let { throw it }
        return testUser.also { user = it }
    }
    override suspend fun register(fullName: String, email: String, password: String): AuthUser {
        registerCalls++
        lastName = fullName
        lastEmail = email
        gate?.await()
        failure?.let { throw it }
        return testUser.also { user = it }
    }
    override suspend fun sendPasswordReset(email: String) {
        resetCalls++
        lastEmail = email
        failure?.let { throw it }
    }
    override fun logout() { user = null }
}
