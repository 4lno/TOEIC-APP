package com.luudev.vocalens.domain.model

data class AuthUser(val uid: String, val email: String, val displayName: String)

enum class AuthError {
    InvalidCredentials, EmailAlreadyInUse, WeakPassword, Network, TooManyRequests,
    OperationNotAllowed, ProfileUpdateFailed, Unknown
}

class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(error.name, cause)
