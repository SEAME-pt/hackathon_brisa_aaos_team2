package com.example.auto0s

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.widget.Toast
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.widget.Button
import com.example.auto0s.SettingsActivity
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1003
    }

    // Flag to remember if overlay permission has been asked
    private var hasAskedForOverlayPermission = false

    private fun hasAskedForOverlayPermissionBefore(): Boolean {
        val prefs = getSharedPreferences("via_verde_prefs", MODE_PRIVATE)
        return prefs.getBoolean("has_asked_overlay_permission", false)
    }

    private fun setHasAskedForOverlayPermission() {
        val prefs = getSharedPreferences("via_verde_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("has_asked_overlay_permission", true).apply()
        hasAskedForOverlayPermission = true
    }

    // Modern Activity Result API for overlay permission
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "Overlay permission result received - resultCode: ${result.resultCode}")
        Log.d("MainActivity", "Overlay permission result - data: ${result.data}")

        // Add a small delay to ensure the activity is fully resumed
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                Log.d("MainActivity", "Processing overlay permission result after delay")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        Log.d("MainActivity", "Overlay permission granted")
                        Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("MainActivity", "Overlay permission denied")
                        Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
                // Ensure the service is running after permission flow
                Log.d("MainActivity", "Ensuring location service is running after overlay permission")
                ensureLocationServiceRunning()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in overlay permission result", e)
            }
            Log.d("MainActivity", "Overlay permission flow completed")
        }, 500) // 500ms delay
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate: Activity starting")

        // Check if this is from notification click
        val isFromNotification = intent?.flags?.and(Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        Log.d("MainActivity", "onCreate: Is from notification: $isFromNotification")

        // Check if app is already running
        val isAppAlreadyRunning = isAppRunning()
        Log.d("MainActivity", "onCreate: Is app already running: $isAppAlreadyRunning")

        // Set theme based on app state
        if (!isAppAlreadyRunning) {
            Log.d("MainActivity", "onCreate: App not running, using splash theme")
            // Use splash theme for new app launches
            setTheme(R.style.Theme_Auto0s_Splash)
        } else {
            Log.d("MainActivity", "onCreate: App already running, using main theme immediately")
            // Use main theme immediately if app is already running
            setTheme(R.style.Theme_Auto0s)
        }



        // Add fade-in animation for smooth appearance
        if (isFromNotification) {
            Log.d("MainActivity", "onCreate: Adding fade-in animation for notification launch")
            // Window animations will be handled by the entrance animations
        }

        val prefs = getSharedPreferences("via_verde_prefs", MODE_PRIVATE)
        val token = prefs.getString("token", null)
        if (token == null) {
            Log.d("MainActivity", "onCreate: No token found, redirecting to RegisterActivity")
            RegisterActivity.start(this)
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        // Load overlay permission flag from SharedPreferences
        hasAskedForOverlayPermission = hasAskedForOverlayPermissionBefore()
        Log.d("MainActivity", "onCreate: Has asked for overlay permission before: $hasAskedForOverlayPermission")

        // Start location service if token exists
        Log.d("MainActivity", "onCreate: Token exists, starting location service")
        ensureLocationServiceRunning()

        // Setup settings button
        val settingsButton = findViewById<Button>(R.id.settings_button)
        settingsButton.setOnClickListener {
            Log.d("MainActivity", "Settings button clicked")
            SettingsActivity.start(this)
        }



        // No entrance animations - just show the UI normally
        Log.d("MainActivity", "onCreate: No entrance animations - showing UI normally")

        // Start permission flow
        checkNotificationPermission()
    }

        override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume: Activity resumed")

        // Check if we're returning from overlay permission settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasOverlayPermission = Settings.canDrawOverlays(this)
            Log.d("MainActivity", "onResume: Overlay permission status: $hasOverlayPermission")

            // If we have overlay permission now, ensure the service is running
            if (hasOverlayPermission) {
                Log.d("MainActivity", "onResume: Overlay permission granted, ensuring service is running")
                ensureLocationServiceRunning()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause: Activity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy: Activity destroyed")
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE)
            } else {
                checkLocationPermission()
            }
        } else {
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            checkBackgroundLocationPermission()
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

                showBackgroundLocationPermissionDialog()
            } else {
                checkOverlayPermission()
            }
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this) && !hasAskedForOverlayPermission) {
                Log.d("MainActivity", "checkOverlayPermission: Overlay permission not granted and not asked before, showing dialog")
                setHasAskedForOverlayPermission()
                showOverlayPermissionDialog()
            } else if (Settings.canDrawOverlays(this)) {
                Log.d("MainActivity", "checkOverlayPermission: Overlay permission already granted, skipping dialog")
            } else {
                Log.d("MainActivity", "checkOverlayPermission: Overlay permission not granted but already asked before, skipping dialog")
            }
        } else {
            Log.d("MainActivity", "checkOverlayPermission: Android version too low for overlay permission")
        }
    }

    private fun showBackgroundLocationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Background Location Permission")
            .setMessage("This app needs background location access to track your vehicle's location in real-time. Please grant this permission in the next screen.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestBackgroundLocationPermission()
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "Background location permission is required for vehicle tracking", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Overlay Permission (Optional)")
            .setMessage("This app can display vehicle tracking information over other apps while driving. This is optional but provides a better user experience.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton("Skip") { _, _ ->
                Log.d("MainActivity", "showOverlayPermissionDialog: User chose to skip overlay permission")
                setHasAskedForOverlayPermission()
            }
            .setOnCancelListener {
                Log.d("MainActivity", "showOverlayPermissionDialog: User cancelled dialog")
                setHasAskedForOverlayPermission()
            }
            .setCancelable(true)
            .show()
    }

    private fun requestOverlayPermission() {
        Log.d("MainActivity", "requestOverlayPermission: Starting overlay permission request")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
                Log.d("MainActivity", "requestOverlayPermission: Launching intent: $intent")
                overlayPermissionLauncher.launch(intent)
                Log.d("MainActivity", "requestOverlayPermission: Intent launched successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "requestOverlayPermission: Error launching intent", e)
                Toast.makeText(this, "Error opening overlay permission settings", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("MainActivity", "requestOverlayPermission: Android version too low for overlay permission")
        }
    }

    private fun ensureLocationServiceRunning() {
        Log.d("MainActivity", "ensureLocationServiceRunning: Checking if service should start")

        // Check if we have a valid token
        val prefs = getSharedPreferences("via_verde_prefs", MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val hasToken = token != null && token.isNotEmpty()

        if (hasToken && !LocationForegroundService.isRunning()) {
            Log.d("MainActivity", "ensureLocationServiceRunning: Starting service - token present")
            try {
                val intent = Intent(this, LocationForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                Log.d("MainActivity", "ensureLocationServiceRunning: Service start intent sent")
            } catch (e: Exception) {
                Log.e("MainActivity", "ensureLocationServiceRunning: Error starting service", e)
            }
        } else {
            Log.d("MainActivity", "ensureLocationServiceRunning: Service not started - hasToken: $hasToken, isRunning: ${LocationForegroundService.isRunning()}")
        }
    }

    private fun startLocationForegroundService() {
        ensureLocationServiceRunning()
    }

    private fun isAppRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningTasks = activityManager.getRunningTasks(2) // Check top 2 tasks

        for (task in runningTasks) {
            if (task.topActivity?.packageName == packageName) {
                Log.d("MainActivity", "isAppRunning: Found app in running tasks: ${task.topActivity?.className}")
                return true
            }
        }

        Log.d("MainActivity", "isAppRunning: App not found in running tasks")
        return false
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkLocationPermission()
                } else {
                    Toast.makeText(this, "Notification permission denied. App may not work properly.", Toast.LENGTH_LONG).show()
                    checkLocationPermission()
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkBackgroundLocationPermission()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkOverlayPermission()
                } else {
                    Toast.makeText(this, "Background location permission denied. Vehicle tracking will not work properly.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


}
