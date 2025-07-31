package com.example.viaverde.data.model

/**
 * Data model representing an authentication token
 */
data class AuthToken(
    val token: String,
    val type: String = "Bearer",
    val expiresAt: Long? = null,
    val issuedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if the token is expired
     */
    fun isExpired(): Boolean {
        return expiresAt != null && System.currentTimeMillis() > expiresAt
    }

    /**
     * Check if the token is valid
     */
    fun isValid(): Boolean {
        return token.isNotEmpty() && !isExpired()
    }
}
