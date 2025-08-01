package com.example.viaverde.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

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

                                    // Start service directly without delay - the service will handle its own startup logic
            Log.d(TAG, "handleBootCompleted: Auto-start enabled, starting service directly")

            try {
                val serviceIntent = Intent(context, LocationForegroundService::class.java).apply {
                    action = "com.example.viaverde.START_SERVICE_FROM_BOOT"
                }
                context.startService(serviceIntent)
                Log.d(TAG, "handleBootCompleted: Service start intent sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "handleBootCompleted: Error starting service directly", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "handleBootCompleted: Error during boot process", e)
        }
    }
}
