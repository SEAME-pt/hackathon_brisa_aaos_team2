package com.example.viaverde

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for Via Verde
 */
@HiltAndroidApp
class ViaVerdeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
