package com.luudev.vocalens.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateLoginInputTest {
    private val validate = ValidateLoginInput()

    @Test
    fun acceptsValidEmailWithSurroundingWhitespace() {
        assertTrue(validate("  minh.anh+study@example.com  ", "secret").isValid)
    }

    @Test
    fun rejectsMalformedEmails() {
        listOf("", "minhanh", "a@", "@example.com", "a@@example.com", "a b@example.com")
            .forEach { email -> assertTrue("Expected invalid email: $email", validate(email, "secret").invalidEmail) }
    }

    @Test
    fun requiresAtLeastSixPasswordCharacters() {
        assertTrue(validate("a@example.com", "12345").invalidPassword)
        assertFalse(validate("a@example.com", "123456").invalidPassword)
    }

    @Test
    fun reportsBothErrorsForEmptyForm() {
        val result = validate("", "")
        assertTrue(result.invalidEmail)
        assertTrue(result.invalidPassword)
        assertFalse(result.isValid)
    }
}
