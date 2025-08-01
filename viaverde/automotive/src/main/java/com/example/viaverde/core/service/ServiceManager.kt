package com.example.viaverde.core.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.viaverde.service.LocationForegroundService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceManager {

    companion object {
        private const val TAG = "ServiceManager"
    }

    /**
     * Start the location foreground service
     */
    fun startLocationService(context: Context) {
        Log.d(TAG, "startLocationService: Starting location service")

        try {
            val intent = Intent(context, LocationForegroundService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            Log.d(TAG, "startLocationService: Service start intent sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "startLocationService: Error starting service", e)
        }
    }

    /**
     * Stop the location foreground service
     */
    fun stopLocationService(context: Context) {
        Log.d(TAG, "stopLocationService: Stopping location service")

        try {
            val intent = Intent(context, LocationForegroundService::class.java)
            context.stopService(intent)
            Log.d(TAG, "stopLocationService: Service stop intent sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "stopLocationService: Error stopping service", e)
        }
    }

    /**
     * Check if the location service is running
     */
    fun isLocationServiceRunning(): Boolean {
        return LocationForegroundService.isRunning()
    }

    /**
     * Start service from boot (with boot action)
     */
    fun startLocationServiceFromBoot(context: Context) {
        Log.d(TAG, "startLocationServiceFromBoot: Starting service from boot")

        try {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = "com.example.viaverde.START_SERVICE_FROM_BOOT"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            Log.d(TAG, "startLocationServiceFromBoot: Service start intent sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "startLocationServiceFromBoot: Error starting service", e)
        }
    }
}
