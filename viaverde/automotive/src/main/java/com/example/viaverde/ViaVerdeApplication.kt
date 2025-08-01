package com.example.viaverde

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.viaverde.service.LocationForegroundService
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for Via Verde
 */
@HiltAndroidApp
class ViaVerdeApplication : Application() {

    companion object {
        private const val TAG = "ViaVerdeApplication"
    }

        override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Application starting")

        // Note: Cannot start foreground service from Application.onCreate() on Android 14+
        // Service will be started from MainActivity instead
    }
}
