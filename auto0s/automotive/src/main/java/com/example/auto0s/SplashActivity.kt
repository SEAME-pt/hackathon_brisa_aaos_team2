package com.example.auto0s

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SplashActivity"
        private const val SPLASH_DELAY = 2000L // 2 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: SplashActivity starting")

        // Check if app is already running
        if (isAppAlreadyRunning()) {
            Log.d(TAG, "onCreate: App is already running, going directly to MainActivity with smooth transition")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Add smooth fade-out transition
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
            return
        }

        Log.d(TAG, "onCreate: App not running, showing splash screen")

        // Use splash theme
        setTheme(R.style.Theme_Auto0s_Splash)
        setContentView(R.layout.activity_splash)

        // Delay before navigating to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "onCreate: Navigating to MainActivity after splash delay")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Add smooth fade transition
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }, SPLASH_DELAY)
    }

    private fun isAppAlreadyRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)

        if (runningTasks.isNotEmpty()) {
            val topActivity = runningTasks[0].topActivity
            Log.d(TAG, "isAppAlreadyRunning: Top activity: ${topActivity?.className}")

            // Check if any of our activities are in the task
            return topActivity?.packageName == packageName
        }

        return false
    }
}
