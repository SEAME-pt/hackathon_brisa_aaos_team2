package com.example.auto0s

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BootReceiver: Received intent action: ${intent.action}")

                if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
            "android.intent.action.QUICKBOOT_POWERON" == intent.action) {

            Log.d(TAG, "BootReceiver: Boot completed, checking for token and starting service")

            // Check if user has a valid token and auto-start is enabled
            val prefs = context.getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)
            val hasToken = token != null && token.isNotEmpty()
            val isAutoStartEnabled = prefs.getBoolean("auto_start_enabled", true) // Default to true

            Log.d(TAG, "BootReceiver: Token present: $hasToken, Auto-start enabled: $isAutoStartEnabled")

            if (hasToken && isAutoStartEnabled) {
                // Start the location service directly instead of the activity
                Log.d(TAG, "BootReceiver: Starting LocationForegroundService")
                val serviceIntent = Intent(context, LocationForegroundService::class.java)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d(TAG, "BootReceiver: Service start intent sent successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "BootReceiver: Error starting service", e)
                }
            } else {
                if (!hasToken) {
                    Log.d(TAG, "BootReceiver: No token found, not starting service")
                } else {
                    Log.d(TAG, "BootReceiver: Auto-start is disabled, not starting service")
                }
            }
        }
    }
}
