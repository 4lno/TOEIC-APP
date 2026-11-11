package com.luudev.vocalens.presentation.components

import com.luudev.vocalens.R
import com.luudev.vocalens.domain.model.AuthError

fun AuthError.messageResource(): Int = when (this) {
    AuthError.InvalidCredentials -> R.string.auth_invalid_credentials
    AuthError.EmailAlreadyInUse -> R.string.auth_email_in_use
    AuthError.WeakPassword -> R.string.auth_weak_password
    AuthError.Network -> R.string.auth_network_error
    AuthError.TooManyRequests -> R.string.auth_too_many_requests
    AuthError.OperationNotAllowed -> R.string.auth_operation_not_allowed
    AuthError.ProfileUpdateFailed -> R.string.auth_profile_update_failed
    AuthError.Unknown -> R.string.auth_unknown_error
}
