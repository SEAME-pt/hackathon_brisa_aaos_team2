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
            "android.intent.action.QUICKBOOT_POWERON" -> {
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
            // Note: We can't use suspend functions in BroadcastReceiver
            // For now, we'll start the service and let it handle its own validation
            Log.d(TAG, "handleBootCompleted: Starting location service")

            // Start the location service directly
            val serviceIntent = Intent(context, LocationForegroundService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            Log.d(TAG, "handleBootCompleted: Location service start intent sent")
        } catch (e: Exception) {
            Log.e(TAG, "handleBootCompleted: Error during boot process", e)
        }
    }
}
