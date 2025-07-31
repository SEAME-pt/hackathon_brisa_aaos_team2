package com.example.viaverde.data.datasource.local

import com.example.viaverde.core.security.SecureStorageManager
import com.example.viaverde.data.model.AuthToken
import com.example.viaverde.data.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for secure preferences storage
 */
@Singleton
class SecurePreferencesDataSource @Inject constructor(
    private val secureStorage: SecureStorageManager
) {

    /**
     * Store authentication token
     */
    suspend fun storeAuthToken(token: String) {
        secureStorage.storeAuthToken(token)
    }

    /**
     * Get authentication token
     */
    suspend fun getAuthToken(): String? {
        return secureStorage.getAuthToken()
    }

    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return secureStorage.hasValidAuthToken()
    }

    /**
     * Store user email
     */
    suspend fun storeUserEmail(email: String) {
        secureStorage.storeUserEmail(email)
    }

    /**
     * Get user email
     */
    suspend fun getUserEmail(): String? {
        return secureStorage.getUserEmail()
    }

    /**
     * Store auto-start preference
     */
    suspend fun setAutoStartEnabled(enabled: Boolean) {
        secureStorage.setAutoStartEnabled(enabled)
    }

    /**
     * Get auto-start preference
     */
    suspend fun isAutoStartEnabled(): Boolean {
        return secureStorage.isAutoStartEnabled()
    }

    /**
     * Store overlay permission asked flag
     */
    suspend fun setOverlayPermissionAsked(asked: Boolean) {
        secureStorage.setOverlayPermissionAsked(asked)
    }

    /**
     * Get overlay permission asked flag
     */
    suspend fun hasAskedForOverlayPermission(): Boolean {
        return secureStorage.hasAskedForOverlayPermission()
    }

    /**
     * Clear all data
     */
    suspend fun clearAllData() {
        secureStorage.clearAllData()
    }

    /**
     * Clear authentication data only
     */
    suspend fun clearAuthData() {
        secureStorage.clearAuthData()
    }

    /**
     * Observe authentication state changes
     */
    fun observeAuthState(): Flow<Boolean> {
        return secureStorage.authState
    }
}
