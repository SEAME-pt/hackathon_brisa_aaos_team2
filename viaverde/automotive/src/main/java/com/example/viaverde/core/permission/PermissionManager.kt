package com.example.viaverde.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager {

    companion object {
        private const val TAG = "PermissionManager"
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasBasicLocationPermissions(context) &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || hasBackgroundLocationPermission(context)) &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotificationPermission(context)) &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || hasForegroundServicePermission(context))
    }

    /**
     * Check if basic location permissions are granted
     */
    fun hasBasicLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if background location permission is granted
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if foreground service permission is granted
     */
    fun hasForegroundServicePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get the next permission that needs to be requested
     */
    fun getNextRequiredPermission(context: Context): String? {
        if (!hasBasicLocationPermissions(context)) {
            return Manifest.permission.ACCESS_FINE_LOCATION
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission(context)) {
            return Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            return Manifest.permission.POST_NOTIFICATIONS
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !hasForegroundServicePermission(context)) {
            return Manifest.permission.FOREGROUND_SERVICE_LOCATION
        }

        return null // All permissions granted
    }

    /**
     * Get permission display name
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> "Location"
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Background Location"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            Manifest.permission.FOREGROUND_SERVICE_LOCATION -> "Foreground Service"
            else -> "Unknown Permission"
        }
    }
}
