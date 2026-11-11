package com.luudev.vocalens.presentation.login

import androidx.lifecycle.SavedStateHandle
import com.luudev.vocalens.domain.model.AuthError
import com.luudev.vocalens.domain.model.AuthException
import com.luudev.vocalens.domain.usecase.*
import com.luudev.vocalens.testing.FakeAuthRepository
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private fun viewModel(repository: FakeAuthRepository, handle: SavedStateHandle = SavedStateHandle()) =
        LoginViewModel(handle, LoginUseCase(repository), SendPasswordResetUseCase(repository),
            LogoutUseCase(repository), GetCurrentUserUseCase(repository))

    @Test fun invalidFormDoesNotCallAuthentication() = runTest {
        val repository = FakeAuthRepository()
        val vm = viewModel(repository)
        vm.onLoginClick()
        runCurrent()
        assertFalse(vm.uiState.showHome)
        assertTrue(vm.uiState.invalidEmail)
        assertTrue(vm.uiState.invalidPassword)
        assertEquals(0, repository.loginCalls)
    }

    @Test fun homeOpensOnlyAfterAuthenticationAndDuplicateSubmissionsAreIgnored() = runTest {
        val repository = FakeAuthRepository().apply { gate = CompletableDeferred() }
        val vm = viewModel(repository)
        vm.onEmailChange(" learner@example.com ")
        vm.onPasswordChange("123456")
        vm.onLoginClick()
        vm.onLoginClick()
        runCurrent()
        assertTrue(vm.uiState.isLoading)
        assertFalse(vm.uiState.showHome)
        assertEquals(1, repository.loginCalls)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertTrue(vm.uiState.showHome)
        assertFalse(vm.uiState.isLoading)
        assertEquals("", vm.uiState.password)
        assertEquals("learner@example.com", repository.lastEmail)
    }

    @Test fun failedAuthenticationStaysOnLogin() = runTest {
        val repository = FakeAuthRepository().apply { failure = AuthException(AuthError.InvalidCredentials) }
        val vm = viewModel(repository)
        vm.onEmailChange("learner@example.com")
        vm.onPasswordChange("123456")
        vm.onLoginClick()
        runCurrent()
        assertFalse(vm.uiState.showHome)
        assertFalse(vm.uiState.isLoading)
        assertEquals(AuthError.InvalidCredentials, vm.uiState.error)
    }

    @Test fun restoredHomeFlagCannotBypassFirebaseSessionAndLogoutClearsSession() = runTest {
        val repository = FakeAuthRepository()
        val handle = SavedStateHandle(mapOf("login_show_home" to true))
        assertFalse(viewModel(repository, handle).uiState.showHome)
        repository.user = repository.testUser
        val vm = viewModel(repository, handle)
        assertTrue(vm.uiState.showHome)
        vm.onLogout()
        assertNull(repository.user)
        assertFalse(viewModel(repository, handle).uiState.showHome)
    }

    @Test fun passwordResetRequiresEmailAndDoesNotRequirePassword() = runTest {
        val repository = FakeAuthRepository()
        val vm = viewModel(repository)
        vm.onForgotPasswordClick()
        runCurrent()
        assertEquals(0, repository.resetCalls)
        vm.onEmailChange("learner@example.com")
        vm.onForgotPasswordClick()
        runCurrent()
        assertEquals(1, repository.resetCalls)
        assertEquals(LoginNotice.PasswordResetSent, vm.uiState.notice)
        assertFalse(vm.uiState.showHome)
    }
}
