package com.example.viaverde.core.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.viaverde.service.LocationForegroundService
import com.example.viaverde.service.TripMonitorService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceManager @Inject constructor() {

    companion object {
        private const val TAG = "ServiceManager"
    }

    /**
     * Start location foreground service
     */
    fun startLocationService(context: Context) {
        Log.d(TAG, "startLocationService: Starting location service")

        try {
            val serviceIntent = Intent(context, LocationForegroundService::class.java)
            context.startService(serviceIntent)
            Log.d(TAG, "startLocationService: Location service start intent sent")
        } catch (e: Exception) {
            Log.e(TAG, "startLocationService: Error starting location service", e)
        }
    }

    /**
     * Stop location foreground service
     */
    fun stopLocationService(context: Context) {
        Log.d(TAG, "stopLocationService: Stopping location service")

        try {
            val serviceIntent = Intent(context, LocationForegroundService::class.java)
            context.stopService(serviceIntent)
            Log.d(TAG, "stopLocationService: Location service stop intent sent")
        } catch (e: Exception) {
            Log.e(TAG, "stopLocationService: Error stopping location service", e)
        }
    }

    /**
     * Check if location service is running
     */
    fun isLocationServiceRunning(): Boolean {
        return LocationForegroundService.isRunning()
    }

    /**
     * Start location service from boot
     */
    fun startLocationServiceFromBoot(context: Context) {
        Log.d(TAG, "startLocationServiceFromBoot: Starting location service from boot")

        try {
            val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
                action = "com.example.viaverde.START_SERVICE_FROM_BOOT"
            }
            context.startService(serviceIntent)
            Log.d(TAG, "startLocationServiceFromBoot: Location service start intent sent")
        } catch (e: Exception) {
            Log.e(TAG, "startLocationServiceFromBoot: Error starting location service", e)
        }
    }

    /**
     * Start trip monitor service
     */
    fun startTripMonitorService(context: Context) {
        Log.d(TAG, "startTripMonitorService: Starting trip monitor service")

        try {
            val serviceIntent = Intent(context, TripMonitorService::class.java)
            context.startService(serviceIntent)
            Log.d(TAG, "startTripMonitorService: Trip monitor service start intent sent")
        } catch (e: Exception) {
            Log.e(TAG, "startTripMonitorService: Error starting trip monitor service", e)
        }
    }

    /**
     * Stop trip monitor service
     */
    fun stopTripMonitorService(context: Context) {
        Log.d(TAG, "stopTripMonitorService: Stopping trip monitor service")

        try {
            val serviceIntent = Intent(context, TripMonitorService::class.java)
            context.stopService(serviceIntent)
            Log.d(TAG, "stopTripMonitorService: Trip monitor service stop intent sent")
        } catch (e: Exception) {
            Log.e(TAG, "stopTripMonitorService: Error stopping trip monitor service", e)
        }
    }

    /**
     * Check if trip monitor service is running
     */
    fun isTripMonitorServiceRunning(): Boolean {
        return TripMonitorService.isRunning()
    }
}
