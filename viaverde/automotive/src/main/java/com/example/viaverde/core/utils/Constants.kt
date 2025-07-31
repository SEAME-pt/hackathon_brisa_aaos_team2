package com.example.viaverde.core.utils

/**
 * Application constants
 */
object Constants {

    // API Endpoints
    object Api {
        const val BASE_URL = "https://dev.a-to-be.com"
        const val LOGIN_ENDPOINT = "/mtolling/services/mtolling/login"
        const val LOCATION_ENDPOINT = "/mtolling/services/mtolling/location"
    }

    // SharedPreferences
    object Preferences {
        const val SECURE_PREFS_NAME = "secure_via_verde_prefs"
        const val LEGACY_PREFS_NAME = "via_verde_prefs"

        // Keys
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_AUTO_START_ENABLED = "auto_start_enabled"
        const val KEY_OVERLAY_PERMISSION_ASKED = "overlay_permission_asked"
    }

    // Location
    object Location {
        const val UPDATE_INTERVAL = 5000L // 5 seconds
        const val MIN_DISTANCE = 10f // 10 meters
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_service_channel"
        const val CHANNEL_NAME = "Via Verde"
        const val CHANNEL_DESCRIPTION = "Service for tracking vehicle location in real-time"
    }

    // Permissions
    object Permissions {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1003
    }

    // Timeouts
    object Timeouts {
        const val NETWORK_TIMEOUT_SECONDS = 30L
        const val SPLASH_DELAY_MS = 2000L
    }
}
