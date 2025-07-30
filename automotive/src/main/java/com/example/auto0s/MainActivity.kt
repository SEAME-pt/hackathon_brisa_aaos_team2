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

class MainActivity : AppCompatActivity() {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Switch to main theme after splash screen
        setTheme(R.style.Theme_Auto0s)

        val prefs = getSharedPreferences("via_verde_prefs", MODE_PRIVATE)
        val token = prefs.getString("token", null)
        if (token == null) {
            RegisterActivity.start(this)
            finish()
            return
        }
                setContentView(R.layout.activity_main)

        // Start location service if token exists
        Log.d("MainActivity", "onCreate: Token exists, starting location service")
        if (!LocationForegroundService.isRunning()) {
            val intent = Intent(this, LocationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        // Setup settings button
        val settingsButton = findViewById<Button>(R.id.settings_button)
        settingsButton.setOnClickListener {
            Log.d("MainActivity", "Settings button clicked")
            SettingsActivity.start(this)
        }

        // Start permission flow
        checkNotificationPermission()
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
                startLocationForegroundService()
            }
        } else {
            startLocationForegroundService()
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

        private fun startLocationForegroundService() {
        Log.d("MainActivity", "startLocationForegroundService: Checking if service should start")

        // Check if we have a valid token
        val prefs = getSharedPreferences("via_verde_prefs", MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val hasToken = token != null && token.isNotEmpty()

        if (hasToken && !LocationForegroundService.isRunning()) {
            Log.d("MainActivity", "startLocationForegroundService: Starting service - token present")
            val intent = Intent(this, LocationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            Log.d("MainActivity", "startLocationForegroundService: Service not started - hasToken: $hasToken, isRunning: ${LocationForegroundService.isRunning()}")
        }
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
                    startLocationForegroundService()
                } else {
                    Toast.makeText(this, "Background location permission denied. Vehicle tracking will not work properly.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
