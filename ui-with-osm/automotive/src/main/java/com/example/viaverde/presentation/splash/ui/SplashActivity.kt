package com.example.viaverde.presentation.splash.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.viaverde.R
import com.example.viaverde.presentation.main.ui.MainActivity
import android.os.Build

/**
 * Splash activity for app initialization
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val SPLASH_DELAY = 2000L // 2 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Log.d(TAG, "onCreate: SplashActivity starting")

        // Check if app is already running
        if (isAppAlreadyRunning()) {
            Log.d(TAG, "onCreate: App is already running, going directly to MainActivity with smooth transition")
            // App is already running, go directly to main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Use modern activity transitions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS)
                window.enterTransition = android.transition.Fade()
                window.exitTransition = android.transition.Fade()
            }
            finish()
        } else {
            Log.d(TAG, "onCreate: App not running, showing splash screen")
            // App is not running, show splash screen
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "onCreate: Navigating to MainActivity after splash delay")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                // Use modern activity transitions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS)
                    window.enterTransition = android.transition.Fade()
                    window.exitTransition = android.transition.Fade()
                }
                finish()
            }, SPLASH_DELAY)
        }
    }

    private fun isAppAlreadyRunning(): Boolean {
        Log.d(TAG, "isAppAlreadyRunning: Checking if app is already running")
        // Use a simpler approach instead of deprecated getRunningTasks
        // For now, we'll assume the app is not running if this method is called
        // In a real implementation, you might use UsageStatsManager or other alternatives
        Log.d(TAG, "isAppAlreadyRunning: Using simplified check - assuming app not running")
        return false
    }
}
