package com.bdbshs.crest.ui.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for LoginUiState and LoginResult sealed class.
 * Tests UI state management for the login flow.
 */
class LoginViewModelTest {

    @Test
    fun `initial LoginUiState has correct defaults`() {
        // Given/When
        val state = LoginUiState()

        // Then
        assertFalse("isLoading should be false initially", state.isLoading)
        assertNull("error should be null initially", state.error)
    }

    @Test
    fun `LoginUiState copy updates loading state`() {
        // Given
        val initialState = LoginUiState(isLoading = false, error = null)

        // When
        val updatedState = initialState.copy(isLoading = true)

        // Then
        assertTrue("isLoading should be updated", updatedState.isLoading)
        assertNull("error should be preserved as null", updatedState.error)
    }

    @Test
    fun `LoginUiState copy updates error`() {
        // Given
        val initialState = LoginUiState(isLoading = false, error = null)

        // When
        val updatedState = initialState.copy(error = "Network error")

        // Then
        assertFalse("isLoading should be preserved", updatedState.isLoading)
        assertEquals("error should be updated", "Network error", updatedState.error)
    }

    @Test
    fun `LoginResult NavigateToHome is singleton`() {
        // Given/When
        val result1 = LoginResult.NavigateToHome
        val result2 = LoginResult.NavigateToHome

        // Then
        assertTrue("Should be same instance", result1 === result2)
    }

    @Test
    fun `LoginResult NavigateToSignUpDetails is singleton`() {
        // Given/When
        val result1 = LoginResult.NavigateToSignUpDetails
        val result2 = LoginResult.NavigateToSignUpDetails

        // Then
        assertTrue("Should be same instance", result1 === result2)
    }

    @Test
    fun `LoginResult NavigateToPendingApproval is singleton`() {
        // Given/When
        val result1 = LoginResult.NavigateToPendingApproval
        val result2 = LoginResult.NavigateToPendingApproval

        // Then
        assertTrue("Should be same instance", result1 === result2)
    }

    @Test
    fun `UserType enum has correct values`() {
        // Then
        assertEquals(2, UserType.values().size)
        assertEquals("STUDENT", UserType.STUDENT.name)
        assertEquals("TEACHER", UserType.TEACHER.name)
    }

    @Test
    fun `UserType valueOf works correctly`() {
        // Given/When
        val student = UserType.valueOf("STUDENT")
        val teacher = UserType.valueOf("TEACHER")

        // Then
        assertEquals(UserType.STUDENT, student)
        assertEquals(UserType.TEACHER, teacher)
    }

    @Test
    fun `LoginUiState with loading and error`() {
        // Given
        val state = LoginUiState(
            isLoading = true,
            error = "Connection failed"
        )

        // Then
        assertTrue("isLoading should be true", state.isLoading)
        assertEquals("Connection failed", state.error)
    }

    @Test
    fun `clearing error preserves loading state`() {
        // Given
        val state = LoginUiState(isLoading = true, error = "Error message")

        // When
        val clearedState = state.copy(error = null)

        // Then
        assertTrue("isLoading should be preserved", clearedState.isLoading)
        assertNull("error should be cleared", clearedState.error)
    }
}
