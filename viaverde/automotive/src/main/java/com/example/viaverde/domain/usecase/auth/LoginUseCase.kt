package com.example.viaverde.domain.usecase.auth

import com.example.viaverde.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user login
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    /**
     * Execute login with email and password
     */
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        // Validate input
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty"))
        }

        if (!isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        // Attempt login
        return authRepository.login(email, password).map { token ->
            // Login successful, token is automatically saved by repository
        }
    }

    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    /**
     * Get current user
     */
    suspend fun getCurrentUser() = authRepository.getCurrentUser()

    /**
     * Validate email format
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
