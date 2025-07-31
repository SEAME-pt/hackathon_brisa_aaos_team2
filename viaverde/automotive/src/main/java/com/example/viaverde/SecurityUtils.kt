package com.example.viaverde

import android.util.Log

/**
 * Utility class for secure logging that prevents sensitive information from being exposed
 */
object SecurityUtils {

    /**
     * Masks an email address for secure logging
     * Example: "user@example.com" -> "us***@example.com"
     */
    fun maskEmail(email: String): String {
        return if (email.contains("@")) {
            val parts = email.split("@")
            val username = parts[0]
            val domain = parts[1]
            val maskedUsername = if (username.length <= 2) username else username.take(2) + "***"
            "$maskedUsername@$domain"
        } else {
            "***@***"
        }
    }

    /**
     * Masks a token for secure logging
     * Example: "eyJhbGciOiJIUzI1NiJ9..." -> "eyJhbGciOiJIUzI1NiJ9*** (length: 123)"
     */
    fun maskToken(token: String): String {
        return if (token.length > 20) {
            "${token.take(20)}*** (length: ${token.length})"
        } else {
            "*** (length: ${token.length})"
        }
    }

    /**
     * Masks GPS coordinates for privacy
     * Example: (40.7128, -74.0060) -> "Lat: 40.71, Lon: -74.01"
     */
    fun maskCoordinates(latitude: Double, longitude: Double): String {
        val maskedLat = String.format("%.2f", latitude)
        val maskedLon = String.format("%.2f", longitude)
        return "Lat: $maskedLat, Lon: $maskedLon"
    }

    /**
     * Masks a password for secure logging
     * Always returns "***" regardless of input
     */
    fun maskPassword(password: String): String {
        return "*** (length: ${password.length})"
    }

    /**
     * Secure logging for sensitive data
     */
    fun logSecure(tag: String, message: String, sensitiveData: String? = null) {
        if (sensitiveData != null) {
            Log.d(tag, "$message - [SENSITIVE DATA MASKED]")
        } else {
            Log.d(tag, message)
        }
    }

    /**
     * Logs token presence without exposing the actual token
     */
    fun logTokenStatus(tag: String, hasToken: Boolean, tokenLength: Int? = null) {
        if (hasToken && tokenLength != null) {
            Log.d(tag, "Token present: true (length: $tokenLength)")
        } else {
            Log.d(tag, "Token present: $hasToken")
        }
    }
}
