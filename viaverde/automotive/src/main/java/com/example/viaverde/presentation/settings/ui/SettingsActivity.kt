package com.example.viaverde.presentation.settings.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.viaverde.R
import com.example.viaverde.core.security.SecurityUtils
import com.example.viaverde.domain.usecase.auth.LoginUseCase
import com.example.viaverde.domain.usecase.auth.LogoutUseCase
import com.example.viaverde.presentation.auth.ui.LoginActivity
import com.example.viaverde.service.LocationForegroundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings activity for app configuration
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var loginUseCase: LoginUseCase

    @Inject
    lateinit var logoutUseCase: LogoutUseCase

    private lateinit var userEmailText: TextView
    private lateinit var logoutButton: Button
    private lateinit var goBackButton: Button
    private lateinit var autoStartToggle: Switch

    companion object {
        private const val TAG = "SettingsActivity"

        fun start(context: android.content.Context) {
            Log.d(TAG, "start: Starting SettingsActivity")
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        Log.d(TAG, "onCreate: SettingsActivity started")

        // Initialize views
        userEmailText = findViewById(R.id.user_email_text)
        logoutButton = findViewById(R.id.logout_button)
        goBackButton = findViewById(R.id.go_back_button)
        autoStartToggle = findViewById(R.id.auto_start_toggle)

        // Check if user is logged in and load user data
        lifecycleScope.launch {
            if (!loginUseCase.isLoggedIn()) {
                Log.w(TAG, "onCreate: No token found, redirecting to login")
                LoginActivity.start(this@SettingsActivity)
                finish()
                return@launch
            }

            // Display user email if available
            val user = loginUseCase.getCurrentUser()
            runOnUiThread {
                if (user != null) {
                    Log.d(TAG, "onCreate: User email found (masked: ${SecurityUtils.maskEmail(user.email)})")
                    userEmailText.text = "Logged in as: ${SecurityUtils.maskEmail(user.email)}"
                } else {
                    Log.d(TAG, "onCreate: No user email found")
                    userEmailText.text = "Not logged in"
                }
            }

            // Setup auto-start toggle
            setupAutoStartToggle()

            // Setup logout button
            logoutButton.setOnClickListener {
                Log.d(TAG, "Logout button clicked")
                performLogout()
            }

            // Setup go back button
            goBackButton.setOnClickListener {
                Log.d(TAG, "Go back button clicked")
                finish()
            }
        }
    }

    private fun performLogout() {
        // Stop location service before clearing token
        if (LocationForegroundService.isRunning()) {
            val intent = Intent(this, LocationForegroundService::class.java)
            stopService(intent)
        }

        // Perform logout using use case
        lifecycleScope.launch {
            try {
                val result = logoutUseCase()
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Logout successful")
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()

                            // Navigate back to login screen
                            val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Logout failed", exception)
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "Logout failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Logout failed", e)
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupAutoStartToggle() {
        // Load current auto-start setting
        // TODO: Get from use case
        val isAutoStartEnabled = true
        autoStartToggle.isChecked = isAutoStartEnabled
        Log.d(TAG, "setupAutoStartToggle: Auto-start is ${if (isAutoStartEnabled) "enabled" else "disabled"}")

        autoStartToggle.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "setupAutoStartToggle: Auto-start toggle changed to: $isChecked")
            // TODO: Update using use case

            val message = if (isChecked) {
                "Auto-start enabled - app will start automatically on boot"
            } else {
                "Auto-start disabled - app will not start automatically on boot"
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
