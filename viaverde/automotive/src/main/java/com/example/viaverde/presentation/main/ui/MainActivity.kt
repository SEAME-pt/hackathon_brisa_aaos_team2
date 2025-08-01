package com.example.viaverde.presentation.main.ui

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.viaverde.R
import com.example.viaverde.core.security.SecurityUtils
import com.example.viaverde.core.utils.Constants
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

    private lateinit var homeTab: android.view.View
    private lateinit var settingsTab: android.view.View
    private lateinit var accountTab: android.view.View

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Overlay permission result received - resultCode: ${result.resultCode}")
        Log.d(TAG, "Overlay permission result - data: ${result.data}")

        // Add a small delay to ensure the activity is fully resumed
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Processing overlay permission result after delay")

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (android.provider.Settings.canDrawOverlays(this)) {
                        Log.d(TAG, "Overlay permission granted")
                        // TODO: Update overlay permission state
                    } else {
                        Log.d(TAG, "Overlay permission denied")
                    }
                }
                // Ensure the service is running after permission flow
                Log.d(TAG, "Ensuring location service is running after overlay permission")
                ensureLocationServiceRunning()
            } catch (e: Exception) {
                Log.e(TAG, "Error in overlay permission result", e)
            }
            Log.d(TAG, "Overlay permission flow completed")
        }, 500) // 500ms delay
    }

    private val foregroundServicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Foreground service location permission result: $isGranted")
        if (isGranted) {
            Log.d(TAG, "Foreground service location permission granted")
            savePermissionStates()
            checkAndStartLocationService()
        } else {
            Log.w(TAG, "Foreground service location permission denied")
            Toast.makeText(this, "Location service requires foreground service permission", Toast.LENGTH_LONG).show()
        }
    }

                    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Location permissions result: $permissions")
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            Log.d(TAG, "Location permissions granted")
            savePermissionStates()
            checkAndStartLocationService()
        } else {
            Log.w(TAG, "Location permissions denied")
            Toast.makeText(this, "Location permissions are required for the app to function", Toast.LENGTH_LONG).show()
        }
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Background location permission result: $isGranted")
        if (isGranted) {
            Log.d(TAG, "Background location permission granted")
            savePermissionStates()
            Toast.makeText(this, "Location tracking enabled for all times", Toast.LENGTH_LONG).show()
        } else {
            Log.w(TAG, "Background location permission denied")
            Toast.makeText(this, "Background location is recommended for continuous tracking", Toast.LENGTH_LONG).show()
            // If background location is denied, request basic location permissions
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return@registerForActivityResult
        }
        checkAndStartLocationService()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Notification permission result: $isGranted")
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
            savePermissionStates()
            checkAndStartLocationService()
        } else {
            Log.w(TAG, "Notification permission denied")
            Toast.makeText(this, "Notification permission is required to show service status", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Activity starting")

        // Check if this is from notification
        val isFromNotification = intent?.getBooleanExtra("from_notification", false) ?: false
        Log.d(TAG, "onCreate: Is from notification: $isFromNotification")

        // Check if permissions should be checked after login
        val checkPermissionsAfterLogin = intent?.getBooleanExtra("check_permissions_after_login", false) ?: false
        Log.d(TAG, "onCreate: Check permissions after login: $checkPermissionsAfterLogin")

        // Check if app is already running
        val isAppAlreadyRunning = isAppRunning()
        Log.d(TAG, "onCreate: Is app already running: $isAppAlreadyRunning")

        // Set theme based on app state
        if (!isAppAlreadyRunning) {
            Log.d(TAG, "onCreate: App not running, using splash theme")
            // Use splash theme for new app launches
            setTheme(R.style.Theme_Auto0s_Splash)
        } else {
            Log.d(TAG, "onCreate: App already running, using main theme immediately")
            // Use main theme immediately if app is already running
            setTheme(R.style.Theme_Auto0s)
        }

        setContentView(R.layout.activity_main)

        // Add fade-in animation for smooth appearance
        if (isFromNotification) {
            Log.d(TAG, "onCreate: Adding fade-in animation for notification launch")
            // Use modern activity transitions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS)
                window.enterTransition = android.transition.Fade()
                window.exitTransition = android.transition.Fade()
            }
        }

        // Initialize views
        homeTab = findViewById(R.id.home_tab)
        settingsTab = findViewById(R.id.settings_tab)
        accountTab = findViewById(R.id.account_tab)

        // Check if user is logged in
        lifecycleScope.launch {
            if (!loginUseCase.isLoggedIn()) {
                Log.d(TAG, "onCreate: No token found, redirecting to LoginActivity")
                LoginActivity.start(this@MainActivity)
                finish()
                return@launch
            }

            runOnUiThread {
                // Setup custom navigation
                setupCustomNavigation()

                // Check overlay permission
                // TODO: Get from use case
                val hasAskedForOverlayPermission = false
                Log.d(TAG, "onCreate: Has asked for overlay permission before: $hasAskedForOverlayPermission")

                Log.d(TAG, "onCreate: No entrance animations - showing UI normally")
            }

            // Check all permissions and start location service if logged in
            if (loginUseCase.isLoggedIn()) {
                Log.d(TAG, "onCreate: Token exists, checking all permissions and starting location service")
                if (checkPermissionsAfterLogin) {
                    Log.d(TAG, "onCreate: Checking permissions immediately after login")
                    checkAndStartLocationService()
                } else {
                    Log.d(TAG, "onCreate: Normal permission check flow")
                    checkAndStartLocationService()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity resumed")

        // Only check permissions if user is logged in and service is not running
        lifecycleScope.launch {
            if (loginUseCase.isLoggedIn() && !LocationForegroundService.isRunning()) {
                Log.d(TAG, "onResume: User logged in but service not running, checking permissions")
                checkAndStartLocationService()
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
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs overlay permission to show important information while driving. Would you like to grant this permission?")
            .setPositiveButton("Grant Permission") { _, _ ->
                Log.d(TAG, "showOverlayPermissionDialog: User chose to grant overlay permission")
                requestOverlayPermission()
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
            .show()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "requestOverlayPermission: Starting overlay permission request")
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            Log.d(TAG, "requestOverlayPermission: Launching intent: $intent")

            try {
                overlayPermissionLauncher.launch(intent)
                Log.d(TAG, "requestOverlayPermission: Intent launched successfully")
            } catch (e: Exception) {
                Log.e(TAG, "requestOverlayPermission: Error launching intent", e)
            }
        } else {
            Log.d(TAG, "requestOverlayPermission: Android version too low for overlay permission")
        }
    }

        private fun checkAndStartLocationService() {
        Log.d(TAG, "checkAndStartLocationService: Checking permissions before starting service")

        lifecycleScope.launch {
            // Check if we have a valid token
            val hasToken = loginUseCase.isLoggedIn()

            if (hasToken && !LocationForegroundService.isRunning()) {
                Log.d(TAG, "checkAndStartLocationService: Token present, checking permissions")

                // Check for FOREGROUND_SERVICE_LOCATION permission on Android 14+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "checkAndStartLocationService: Requesting FOREGROUND_SERVICE_LOCATION permission")
                        foregroundServicePermissionLauncher.launch(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                        return@launch
                    }
                }

                // Check for basic location permissions first
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "checkAndStartLocationService: Requesting basic location permissions first")
                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                    return@launch
                }

                // Check for background location permission after basic location permissions on Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "checkAndStartLocationService: Requesting background location permission after basic permissions")
                        showBackgroundLocationDialog()
                        return@launch
                    }
                }

                // Check for notification permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "checkAndStartLocationService: Requesting notification permission")
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        return@launch
                    }
                }

                // All permissions granted, save permission states and start the service
                savePermissionStates()
                startLocationService()
            } else {
                Log.d(TAG, "checkAndStartLocationService: Service not started - hasToken: $hasToken, isRunning: ${LocationForegroundService.isRunning()}")
            }
        }
    }

        private fun savePermissionStates() {
        Log.d(TAG, "savePermissionStates: Saving current permission states")

        val sharedPrefs = getSharedPreferences("permissions", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Save location permissions
        editor.putBoolean("location_fine", checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        editor.putBoolean("location_coarse", checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)

        // Save background location permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            editor.putBoolean("location_background", checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)
        }

        // Save notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            editor.putBoolean("notifications", checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        }

        // Save foreground service permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            editor.putBoolean("foreground_service_location", checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        }

        editor.apply()
        Log.d(TAG, "savePermissionStates: Permission states saved")
    }

    private fun showBackgroundLocationDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Background Location Access")
            .setMessage("Via Verde needs background location access to track your vehicle for toll payments even when the app is not actively being used. This ensures continuous tracking for accurate toll billing.\n\nGranting this permission will also automatically grant basic location permissions.")
            .setPositiveButton("Allow") { _, _ ->
                Log.d(TAG, "showBackgroundLocationDialog: User chose to allow background location")
                backgroundLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton("Skip") { _, _ ->
                Log.d(TAG, "showBackgroundLocationDialog: User chose to skip background location")
                Toast.makeText(this, "Background location is recommended for continuous tracking", Toast.LENGTH_LONG).show()
                checkAndStartLocationService()
            }
            .setOnCancelListener {
                Log.d(TAG, "showBackgroundLocationDialog: User cancelled dialog")
                Toast.makeText(this, "Background location is recommended for continuous tracking", Toast.LENGTH_LONG).show()
                checkAndStartLocationService()
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

    private fun startLocationService() {
        Log.d(TAG, "startLocationService: Starting location service")
                val intent = Intent(this@MainActivity, LocationForegroundService::class.java)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
            Log.d(TAG, "startLocationService: Service start intent sent")
                } catch (e: Exception) {
            Log.e(TAG, "startLocationService: Error starting service", e)
                }
    }

    private fun ensureLocationServiceRunning() {
        Log.d(TAG, "ensureLocationServiceRunning: Checking if service should start")
        checkAndStartLocationService()
    }

    private fun isAppRunning(): Boolean {
        Log.d(TAG, "isAppRunning: Checking if app is already running")
        // Use a simpler approach instead of deprecated getRunningTasks
        // For now, we'll assume the app is not running if this method is called
        // In a real implementation, you might use UsageStatsManager or other alternatives
        Log.d(TAG, "isAppRunning: Using simplified check - assuming app not running")
        return false
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
