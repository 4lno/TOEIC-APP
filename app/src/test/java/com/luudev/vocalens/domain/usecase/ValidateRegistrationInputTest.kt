package com.luudev.vocalens.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateRegistrationInputTest {
    private val validate = ValidateRegistrationInput()

    @Test
    fun acceptsVietnameseNamesAndMatchingPasswords() {
        assertTrue(validate("Nguyễn Minh Anh", " minh.anh@example.com ", "secret", "secret").isValid)
    }

    @Test
    fun rejectsWhitespaceOnlyName() {
        assertTrue(validate("   ", "a@example.com", "secret", "secret").invalidName)
    }

    @Test
    fun reportsAllEmptyFields() {
        val result = validate("", "", "", "")
        assertTrue(result.invalidName)
        assertTrue(result.invalidEmail)
        assertTrue(result.invalidPassword)
        assertTrue(result.invalidConfirmation)
        assertFalse(result.isValid)
    }

    @Test
    fun rejectsShortPasswordEvenWhenConfirmationMatches() {
        val result = validate("Minh Anh", "a@example.com", "12345", "12345")
        assertTrue(result.invalidPassword)
        assertFalse(result.invalidConfirmation)
        assertFalse(result.isValid)
    }

    @Test
    fun confirmationMustMatchExactlyIncludingCaseAndSpaces() {
        listOf("Secret", "secret ", "").forEach { confirmation ->
            assertTrue(validate("Minh Anh", "a@example.com", "secret", confirmation).invalidConfirmation)
        }
    }

    @Test
    fun rejectsInvalidEmailWithoutMarkingOtherFields() {
        val result = validate("Minh Anh", "invalid-email", "secret", "secret")
        assertTrue(result.invalidEmail)
        assertFalse(result.invalidName)
        assertFalse(result.invalidPassword)
        assertFalse(result.invalidConfirmation)
    }
}
