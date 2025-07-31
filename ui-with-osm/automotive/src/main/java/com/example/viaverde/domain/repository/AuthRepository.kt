package com.example.viaverde.domain.repository

import com.example.viaverde.data.model.AuthToken
import com.example.viaverde.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<AuthToken>

    /**
     * Logout the current user
     */
    suspend fun logout(): Result<Unit>

    /**
     * Get the current authentication token
     */
    suspend fun getAuthToken(): AuthToken?

    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean

    /**
     * Get the current user
     */
    suspend fun getCurrentUser(): User?

    /**
     * Save user information
     */
    suspend fun saveUser(user: User): Result<Unit>

    /**
     * Clear all authentication data
     */
    suspend fun clearAuthData(): Result<Unit>

    /**
     * Observe authentication state changes
     */
    fun observeAuthState(): Flow<Boolean>
}
