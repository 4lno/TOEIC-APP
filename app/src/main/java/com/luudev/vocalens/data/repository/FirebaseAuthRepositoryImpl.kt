package com.luudev.vocalens.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.luudev.vocalens.domain.model.AuthError
import com.luudev.vocalens.domain.model.AuthException
import com.luudev.vocalens.domain.model.AuthUser
import com.luudev.vocalens.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepositoryImpl(private val auth: FirebaseAuth) : AuthRepository {
    override fun currentUser(): AuthUser? = auth.currentUser?.toDomain()

    override suspend fun login(email: String, password: String): AuthUser = firebaseCall {
        val user = auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw AuthException(AuthError.Unknown)
        user.toDomain()
    }

    override suspend fun register(fullName: String, email: String, password: String): AuthUser = firebaseCall {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw AuthException(AuthError.Unknown)
        try {
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(fullName).build()).await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // The account already exists; report this explicitly so the user can sign in.
            auth.signOut()
            throw AuthException(AuthError.ProfileUpdateFailed, error)
        }
        user.toDomain()
    }

    override suspend fun sendPasswordReset(email: String): Unit = firebaseCall {
        auth.sendPasswordResetEmail(email).await()
    }

    override fun logout() = auth.signOut()

    private fun FirebaseUser.toDomain() = AuthUser(uid, email.orEmpty(), displayName.orEmpty())

    private suspend fun <T> firebaseCall(block: suspend () -> T): T = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: AuthException) {
        throw error
    } catch (error: Exception) {
        val reason = when (error) {
            is FirebaseNetworkException -> AuthError.Network
            is FirebaseTooManyRequestsException -> AuthError.TooManyRequests
            is FirebaseAuthException -> when (error.errorCode) {
                "ERROR_INVALID_EMAIL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND",
                "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS", "ERROR_USER_DISABLED" -> AuthError.InvalidCredentials
                "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EmailAlreadyInUse
                "ERROR_WEAK_PASSWORD", "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS" -> AuthError.WeakPassword
                "ERROR_OPERATION_NOT_ALLOWED" -> AuthError.OperationNotAllowed
                else -> AuthError.Unknown
            }
            else -> AuthError.Unknown
        }
        throw AuthException(reason, error)
    }
}
