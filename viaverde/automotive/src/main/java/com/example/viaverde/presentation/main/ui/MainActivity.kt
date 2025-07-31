package com.example.viaverde.presentation.main.ui

import android.content.Intent
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

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Activity starting")

        // Check if this is from notification
        val isFromNotification = intent?.getBooleanExtra("from_notification", false) ?: false
        Log.d(TAG, "onCreate: Is from notification: $isFromNotification")

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

            // Start location service if logged in
            if (loginUseCase.isLoggedIn()) {
                Log.d(TAG, "onCreate: Token exists, starting location service")
                ensureLocationServiceRunning()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity resumed")

        // Check overlay permission status
        val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else {
            true
        }
        Log.d(TAG, "onResume: Overlay permission status: $hasOverlayPermission")

        if (hasOverlayPermission) {
            Log.d(TAG, "onResume: Overlay permission granted, ensuring service is running")
            ensureLocationServiceRunning()
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

    private fun ensureLocationServiceRunning() {
        Log.d(TAG, "ensureLocationServiceRunning: Checking if service should start")

        lifecycleScope.launch {
            // Check if we have a valid token
            val hasToken = loginUseCase.isLoggedIn()

            if (hasToken && !LocationForegroundService.isRunning()) {
                Log.d(TAG, "ensureLocationServiceRunning: Starting service - token present")
                val intent = Intent(this@MainActivity, LocationForegroundService::class.java)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    Log.d(TAG, "ensureLocationServiceRunning: Service start intent sent")
                } catch (e: Exception) {
                    Log.e(TAG, "ensureLocationServiceRunning: Error starting service", e)
                }
            } else {
                Log.d(TAG, "ensureLocationServiceRunning: Service not started - hasToken: $hasToken, isRunning: ${LocationForegroundService.isRunning()}")
            }
        }
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
