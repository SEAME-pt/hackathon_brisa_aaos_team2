package com.example.viaverde.presentation.main.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.viaverde.R
import com.example.viaverde.core.security.SecurityUtils
import com.example.viaverde.core.utils.Constants
import com.example.viaverde.core.permission.PermissionManager
import com.example.viaverde.core.service.ServiceManager
import com.example.viaverde.domain.usecase.auth.LoginUseCase
import com.example.viaverde.presentation.auth.ui.LoginActivity
import com.example.viaverde.presentation.main.ui.HomeFragment
import com.example.viaverde.presentation.main.ui.SettingsFragment
import com.example.viaverde.presentation.main.ui.AccountFragment
import com.example.viaverde.service.LocationForegroundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity of the application
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var loginUseCase: LoginUseCase

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var serviceManager: ServiceManager

    private lateinit var homeTab: android.view.View
    private lateinit var settingsTab: android.view.View
    private lateinit var accountTab: android.view.View



    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_BASIC_LOCATION_PERMISSIONS = 1001
        private const val REQUEST_BACKGROUND_LOCATION_PERMISSION = 1002
        private const val REQUEST_NOTIFICATION_PERMISSION = 1003
        private const val REQUEST_FOREGROUND_SERVICE_PERMISSION = 1004
    }

    // Flag to track if we're waiting for background location permission
    private var isWaitingForBackgroundLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity created")

        setContentView(R.layout.activity_main)

        // Initialize UI components
        initializeUI()

        // Setup navigation
        setupCustomNavigation()

        // Check if user is logged in and start location service if needed
        lifecycleScope.launch {
            if (loginUseCase.isLoggedIn()) {
                Log.d(TAG, "onCreate: User logged in, checking permissions and starting service")
                checkAndRequestPermissions()
                startLocationServiceIfNeeded()
            } else {
                Log.d(TAG, "onCreate: User not logged in, redirecting to LoginActivity")
                LoginActivity.start(this@MainActivity)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity resumed")

        // Check if user returned from settings and continue permission flow
        checkAndRequestPermissions()

        // Check if service should be running
        lifecycleScope.launch {
            if (loginUseCase.isLoggedIn() && !serviceManager.isLocationServiceRunning()) {
                Log.d(TAG, "onResume: User logged in, service not running, starting service")
                startLocationServiceIfNeeded()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Activity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Activity destroyed")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_BASIC_LOCATION_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d(TAG, "onRequestPermissionsResult: Basic location permissions granted")
                    checkAndRequestPermissions() // Continue with next permission
                } else {
                    Log.w(TAG, "onRequestPermissionsResult: Basic location permissions denied")
                    Toast.makeText(this, "Location permission is required for the app to function properly", Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_BACKGROUND_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: Background location permission granted")
                    isWaitingForBackgroundLocation = false
                    checkAndRequestPermissions() // Continue with next permission
                } else {
                    Log.w(TAG, "onRequestPermissionsResult: Background location permission denied")
                    isWaitingForBackgroundLocation = false
                    Toast.makeText(this, "Background location permission is recommended for better functionality", Toast.LENGTH_LONG).show()
                    checkAndRequestPermissions() // Continue anyway
                }
            }
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: Notification permission granted")
                    checkAndRequestPermissions() // Continue with next permission
                } else {
                    Log.w(TAG, "onRequestPermissionsResult: Notification permission denied")
                    Toast.makeText(this, "Notification permission is recommended for important alerts", Toast.LENGTH_LONG).show()
                    checkAndRequestPermissions() // Continue anyway
                }
            }
            REQUEST_FOREGROUND_SERVICE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: Foreground service permission granted")
                    checkAndRequestPermissions() // Continue with next permission
                } else {
                    Log.w(TAG, "onRequestPermissionsResult: Foreground service permission denied")
                    Toast.makeText(this, "Foreground service permission is required for location services", Toast.LENGTH_LONG).show()
                    checkAndRequestPermissions() // Continue anyway
                }
            }
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // TODO: Get from use case
            val hasAskedForOverlayPermission = false

            if (!android.provider.Settings.canDrawOverlays(this) && !hasAskedForOverlayPermission) {
                Log.d(TAG, "checkOverlayPermission: Overlay permission not granted and not asked before, showing dialog")
                // TODO: Update overlay permission state
                showOverlayPermissionDialog()
            } else if (android.provider.Settings.canDrawOverlays(this)) {
                Log.d(TAG, "checkOverlayPermission: Overlay permission already granted, skipping dialog")
            } else {
                Log.d(TAG, "checkOverlayPermission: Overlay permission not granted but already asked before, skipping dialog")
            }
        } else {
            Log.d(TAG, "checkOverlayPermission: Android version too low for overlay permission")
        }
    }

    private fun showOverlayPermissionDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs overlay permission to show important information while driving. Would you like to grant this permission?")
            .setPositiveButton("Grant Permission") { _, _ ->
                Log.d(TAG, "showOverlayPermissionDialog: User chose to grant overlay permission")
                // Simplified: Just show a message to user to enable manually
                Toast.makeText(this, "Please enable overlay permission manually in Settings > Apps > ViaVerde > Permissions", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Skip") { _, _ ->
                Log.d(TAG, "showOverlayPermissionDialog: User chose to skip overlay permission")
                // TODO: Update overlay permission state
            }
            .setOnCancelListener {
                Log.d(TAG, "showOverlayPermissionDialog: User cancelled dialog")
                // TODO: Update overlay permission state
            }
            .setCancelable(true)
            .create()

        dialog.show()

        // Set green color for the buttons and remove purple ripple effect
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(resources.getColor(R.color.via_verde_green, null))
            background = null // Remove default background/ripple
            isClickable = true
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(resources.getColor(R.color.via_verde_green, null))
            background = null // Remove default background/ripple
            isClickable = true
        }
    }

    private fun checkAndRequestPermissions() {
        Log.d(TAG, "checkAndRequestPermissions: Checking required permissions")

        // Check basic location permissions
        if (!permissionManager.hasBasicLocationPermissions(this)) {
            Log.d(TAG, "checkAndRequestPermissions: Requesting basic location permissions")
            requestBasicLocationPermissions()
            return
        }

        // Check background location permission (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !permissionManager.hasBackgroundLocationPermission(this)) {
            // Reset flag if permission was granted while we were waiting
            if (isWaitingForBackgroundLocation) {
                isWaitingForBackgroundLocation = false
            }
            Log.d(TAG, "checkAndRequestPermissions: Requesting background location permission")
            requestBackgroundLocationPermission()
            return
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && permissionManager.hasBackgroundLocationPermission(this)) {
            // Reset flag if permission is now granted
            isWaitingForBackgroundLocation = false
        }

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionManager.hasNotificationPermission(this)) {
            Log.d(TAG, "checkAndRequestPermissions: Requesting notification permission")
            requestNotificationPermission()
            return
        }

        // Check foreground service permission (Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !permissionManager.hasForegroundServicePermission(this)) {
            Log.d(TAG, "checkAndRequestPermissions: Requesting foreground service permission")
            requestForegroundServicePermission()
            return
        }

        Log.d(TAG, "checkAndRequestPermissions: All permissions granted")
    }

    private fun requestBasicLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionRationaleDialog("Location permission is required to show your position on the map and provide location-based services.")
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_BASIC_LOCATION_PERMISSIONS)
        }
    }

    private fun requestBackgroundLocationPermission() {
        // Background location permission requires special handling on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!isWaitingForBackgroundLocation) {
                isWaitingForBackgroundLocation = true
                showBackgroundLocationDialog()
            }
        } else {
            // For older versions, use standard request
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), REQUEST_BACKGROUND_LOCATION_PERMISSION)
        }
    }

    private fun showBackgroundLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Background Location Permission")
            .setMessage("This app needs background location permission to continue tracking your location when the app is in the background.\n\nPlease follow these steps:\n1. Tap 'Open Settings'\n2. Tap 'Permissions'\n3. Tap 'Location'\n4. Select 'Allow all the time'")
            .setPositiveButton("Open Settings") { _, _ ->
                // Open app info page (most reliable approach)
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)

                // Show a toast to guide the user
                Toast.makeText(this, "Go to Permissions → Location → Allow all the time", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Skip") { _, _ ->
                // Continue without background location permission
                Log.d(TAG, "showBackgroundLocationDialog: User chose to skip background location permission")
                isWaitingForBackgroundLocation = false
                checkAndRequestPermissions() // Continue with next permission
            }
            .setCancelable(false)
            .show()
    }

    private fun requestNotificationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            showPermissionRationaleDialog("Notification permission is required to show important alerts and updates.")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_PERMISSION)
        }
    }

    private fun requestForegroundServicePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION)) {
            showPermissionRationaleDialog("Foreground service permission is required to provide location services.")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.FOREGROUND_SERVICE_LOCATION), REQUEST_FOREGROUND_SERVICE_PERMISSION)
        }
    }

    private fun showPermissionRationaleDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                // Re-request the permission
                checkAndRequestPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startLocationServiceIfNeeded() {
        Log.d(TAG, "startLocationServiceIfNeeded: Checking if service should start")

        // Check if all required permissions are granted
        if (!permissionManager.hasAllRequiredPermissions(this)) {
            Log.d(TAG, "startLocationServiceIfNeeded: Missing permissions, skipping service start")
            return
        }

        // Check if service is already running
        if (serviceManager.isLocationServiceRunning()) {
            Log.d(TAG, "startLocationServiceIfNeeded: Service already running, skipping")
            return
        }

        // Start the service (auto-start setting only affects boot behavior, not app startup)
        Log.d(TAG, "startLocationServiceIfNeeded: Starting location service")
        serviceManager.startLocationService(this)
    }

    private fun initializeUI() {
        // Initialize views
        homeTab = findViewById(R.id.home_tab)
        settingsTab = findViewById(R.id.settings_tab)
        accountTab = findViewById(R.id.account_tab)
    }

    private fun setupCustomNavigation() {
        // Set default fragment
        loadFragment(HomeFragment.newInstance())
        updateTabSelection(0) // Home is selected by default

        // Setup navigation listeners
        homeTab.setOnClickListener {
            loadFragment(HomeFragment.newInstance())
            updateTabSelection(0)
        }

        accountTab.setOnClickListener {
            loadFragment(AccountFragment.newInstance())
            updateTabSelection(1)
        }

        settingsTab.setOnClickListener {
            loadFragment(SettingsFragment.newInstance())
            updateTabSelection(2)
        }
    }

            private fun updateTabSelection(selectedTab: Int) {
        // Update home tab appearance (logo)
        val homeIcon = homeTab.findViewById<android.widget.ImageView>(R.id.home_icon)

        // Update settings tab appearance
        val settingsIcon = settingsTab.findViewById<android.widget.ImageView>(R.id.settings_icon)
        val settingsText = settingsTab.findViewById<android.widget.TextView>(R.id.settings_text)

        // Update account tab appearance
        val accountIcon = accountTab.findViewById<android.widget.ImageView>(R.id.account_icon)
        val accountText = accountTab.findViewById<android.widget.TextView>(R.id.account_text)

        // Reset all to grey
        settingsIcon.setColorFilter(resources.getColor(R.color.dark_grey, null))
        settingsText.setTextColor(resources.getColor(R.color.dark_grey, null))
        accountIcon.setColorFilter(resources.getColor(R.color.dark_grey, null))
        accountText.setTextColor(resources.getColor(R.color.dark_grey, null))

        // Set selected tab to green
        when (selectedTab) {
            0 -> { // Home - show color logo
                homeIcon.setImageResource(R.drawable.via_verde_logo)
            }
            1 -> { // Account - show black & white logo
                homeIcon.setImageResource(R.drawable.via_verde_logo_bw)
                accountIcon.setColorFilter(resources.getColor(R.color.via_verde_green, null))
                accountText.setTextColor(resources.getColor(R.color.via_verde_green, null))
            }
            2 -> { // Settings - show black & white logo
                homeIcon.setImageResource(R.drawable.via_verde_logo_bw)
                settingsIcon.setColorFilter(resources.getColor(R.color.via_verde_green, null))
                settingsText.setTextColor(resources.getColor(R.color.via_verde_green, null))
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}


