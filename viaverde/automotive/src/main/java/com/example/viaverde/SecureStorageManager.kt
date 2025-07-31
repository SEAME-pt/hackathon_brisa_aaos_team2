package com.example.viaverde

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage manager using EncryptedSharedPreferences for sensitive data
 */
class SecureStorageManager(context: Context) {

    companion object {
        private const val TAG = "SecureStorageManager"
        private const val SECURE_PREFS_NAME = "secure_via_verde_prefs"

        // Keys for different types of data
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_AUTO_START_ENABLED = "auto_start_enabled"
        private const val KEY_OVERLAY_PERMISSION_ASKED = "overlay_permission_asked"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        SECURE_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Store authentication token securely
     */
    fun storeAuthToken(token: String) {
        try {
            securePrefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
            Log.d(TAG, "Auth token stored securely (length: ${token.length})")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing auth token", e)
        }
    }

    /**
     * Retrieve authentication token
     */
    fun getAuthToken(): String? {
        return try {
            securePrefs.getString(KEY_AUTH_TOKEN, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving auth token", e)
            null
        }
    }

    /**
     * Check if auth token exists and is valid
     */
    fun hasValidAuthToken(): Boolean {
        val token = getAuthToken()
        val isValid = token != null && token.isNotEmpty()
        SecurityUtils.logTokenStatus(TAG, isValid, token?.length)
        return isValid
    }

    /**
     * Store user email securely
     */
    fun storeUserEmail(email: String) {
        try {
            securePrefs.edit().putString(KEY_USER_EMAIL, email).apply()
            Log.d(TAG, "User email stored securely (masked: ${SecurityUtils.maskEmail(email)})")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing user email", e)
        }
    }

    /**
     * Retrieve user email
     */
    fun getUserEmail(): String? {
        return try {
            securePrefs.getString(KEY_USER_EMAIL, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving user email", e)
            null
        }
    }

    /**
     * Store auto-start preference
     */
    fun setAutoStartEnabled(enabled: Boolean) {
        try {
            securePrefs.edit().putBoolean(KEY_AUTO_START_ENABLED, enabled).apply()
            Log.d(TAG, "Auto-start preference stored: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing auto-start preference", e)
        }
    }

    /**
     * Get auto-start preference (defaults to true)
     */
    fun isAutoStartEnabled(): Boolean {
        return try {
            securePrefs.getBoolean(KEY_AUTO_START_ENABLED, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving auto-start preference", e)
            true // Default to enabled
        }
    }

    /**
     * Store overlay permission asked flag
     */
    fun setOverlayPermissionAsked(asked: Boolean) {
        try {
            securePrefs.edit().putBoolean(KEY_OVERLAY_PERMISSION_ASKED, asked).apply()
            Log.d(TAG, "Overlay permission asked flag stored: $asked")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing overlay permission flag", e)
        }
    }

    /**
     * Get overlay permission asked flag
     */
    fun hasAskedForOverlayPermission(): Boolean {
        return try {
            securePrefs.getBoolean(KEY_OVERLAY_PERMISSION_ASKED, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving overlay permission flag", e)
            false
        }
    }

    /**
     * Clear all stored data (logout)
     */
    fun clearAllData() {
        try {
            securePrefs.edit().clear().apply()
            Log.d(TAG, "All secure data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing secure data", e)
        }
    }

    /**
     * Clear only authentication data (keep preferences)
     */
    fun clearAuthData() {
        try {
            securePrefs.edit()
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_USER_EMAIL)
                .apply()
            Log.d(TAG, "Authentication data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing authentication data", e)
        }
    }

    /**
     * Migrate data from old SharedPreferences to secure storage
     * This should be called once during app initialization
     */
    fun migrateFromLegacyStorage(context: Context) {
        try {
            val legacyPrefs = context.getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)

            // Check if migration is needed
            if (legacyPrefs.contains("token") || legacyPrefs.contains("user_email")) {
                Log.d(TAG, "Migration: Found legacy data, starting migration")

                // Migrate auth token
                val legacyToken = legacyPrefs.getString("token", null)
                if (legacyToken != null && !securePrefs.contains(KEY_AUTH_TOKEN)) {
                    storeAuthToken(legacyToken)
                    Log.d(TAG, "Migration: Auth token migrated")
                }

                // Migrate user email
                val legacyEmail = legacyPrefs.getString("user_email", null)
                if (legacyEmail != null && !securePrefs.contains(KEY_USER_EMAIL)) {
                    storeUserEmail(legacyEmail)
                    Log.d(TAG, "Migration: User email migrated")
                }

                // Migrate auto-start preference
                val legacyAutoStart = legacyPrefs.getBoolean("auto_start_enabled", true)
                if (!securePrefs.contains(KEY_AUTO_START_ENABLED)) {
                    setAutoStartEnabled(legacyAutoStart)
                    Log.d(TAG, "Migration: Auto-start preference migrated")
                }

                // Migrate overlay permission flag
                val legacyOverlayAsked = legacyPrefs.getBoolean("has_asked_overlay_permission", false)
                if (!securePrefs.contains(KEY_OVERLAY_PERMISSION_ASKED)) {
                    setOverlayPermissionAsked(legacyOverlayAsked)
                    Log.d(TAG, "Migration: Overlay permission flag migrated")
                }

                // Clear legacy data after successful migration
                legacyPrefs.edit().clear().apply()
                Log.d(TAG, "Migration: Legacy data cleared after successful migration")
            } else {
                Log.d(TAG, "Migration: No legacy data found, migration not needed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Migration: Error during migration", e)
        }
    }
}
