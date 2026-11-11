package com.luudev.vocalens.presentation.register

import androidx.lifecycle.SavedStateHandle
import com.luudev.vocalens.domain.model.AuthError
import com.luudev.vocalens.domain.model.AuthException
import com.luudev.vocalens.domain.usecase.RegisterUseCase
import com.luudev.vocalens.testing.FakeAuthRepository
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private fun validForm(repository: FakeAuthRepository) =
        RegisterViewModel(SavedStateHandle(), RegisterUseCase(repository)).apply {
            onNameChange(" Learner ")
            onEmailChange(" learner@example.com ")
            onPasswordChange("123456")
            onConfirmPasswordChange("123456")
        }

    @Test fun invalidFormNeverCreatesAccount() = runTest {
        val repository = FakeAuthRepository()
        val vm = RegisterViewModel(SavedStateHandle(), RegisterUseCase(repository))
        vm.onCreateAccountClick()
        runCurrent()
        assertEquals(0, repository.registerCalls)
        assertFalse(vm.uiState.registered)
    }

    @Test fun registrationWaitsForServerAndClearsPasswordsAfterSuccess() = runTest {
        val repository = FakeAuthRepository().apply { gate = CompletableDeferred() }
        val vm = validForm(repository)
        vm.onCreateAccountClick()
        vm.onCreateAccountClick()
        runCurrent()
        assertTrue(vm.uiState.isLoading)
        assertFalse(vm.uiState.registered)
        assertEquals(1, repository.registerCalls)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertTrue(vm.uiState.registered)
        assertFalse(vm.uiState.isLoading)
        assertEquals("", vm.uiState.password)
        assertEquals("", vm.uiState.confirmPassword)
        assertEquals("Learner", repository.lastName)
        assertEquals("learner@example.com", repository.lastEmail)
    }

    @Test fun existingEmailShowsErrorWithoutNavigating() = runTest {
        val repository = FakeAuthRepository().apply { failure = AuthException(AuthError.EmailAlreadyInUse) }
        val vm = validForm(repository)
        vm.onCreateAccountClick()
        runCurrent()
        assertFalse(vm.uiState.registered)
        assertFalse(vm.uiState.isLoading)
        assertEquals(AuthError.EmailAlreadyInUse, vm.uiState.error)
    }
}
