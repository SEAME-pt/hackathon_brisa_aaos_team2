package com.example.viaverde.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.viaverde.service.TripMonitorService

/**
 * Broadcast receiver for handling device boot events
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: Boot completed received - action: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.LOCKED_BOOT_COMPLETED" -> {
                Log.d(TAG, "onReceive: Processing boot completed event")
                handleBootCompleted(context)
            }
            else -> {
                Log.d(TAG, "onReceive: Ignoring action: ${intent.action}")
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "handleBootCompleted: Starting boot process")

        try {
            // Check if auto-start is enabled in settings
            val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val isAutoStartEnabled = sharedPrefs.getBoolean("auto_start_enabled", false)

            Log.d(TAG, "handleBootCompleted: Auto-start setting: $isAutoStartEnabled")

            if (!isAutoStartEnabled) {
                Log.d(TAG, "handleBootCompleted: Auto-start disabled, skipping service start")
                return
            }

            // Start location service directly without delay - the service will handle its own startup logic
            Log.d(TAG, "handleBootCompleted: Auto-start enabled, starting services directly")

            try {
                val locationServiceIntent = Intent(context, LocationForegroundService::class.java).apply {
                    action = "com.example.viaverde.START_SERVICE_FROM_BOOT"
                }
                context.startService(locationServiceIntent)
                Log.d(TAG, "handleBootCompleted: Location service start intent sent successfully")

                // Check if trip monitoring is enabled
                val isTripMonitoringEnabled = sharedPrefs.getBoolean("trip_monitoring_enabled", false)
                if (isTripMonitoringEnabled) {
                    // Start trip monitor service
                    val tripMonitorServiceIntent = Intent(context, TripMonitorService::class.java)
                    context.startService(tripMonitorServiceIntent)
                    Log.d(TAG, "handleBootCompleted: Trip monitor service start intent sent successfully")
                } else {
                    Log.d(TAG, "handleBootCompleted: Trip monitoring disabled, skipping trip monitor service")
                }
            } catch (e: Exception) {
                Log.e(TAG, "handleBootCompleted: Error starting services directly", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "handleBootCompleted: Error during boot process", e)
        }
    }
}
