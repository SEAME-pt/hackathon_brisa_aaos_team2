package com.example.auto0s

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SettingsActivity"

        fun start(context: Context) {
            Log.d(TAG, "start: Starting SettingsActivity")
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var logoutButton: Button
    private lateinit var goBackButton: Button
    private lateinit var userEmailText: TextView
    private lateinit var autoStartToggle: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: SettingsActivity started")
        setContentView(R.layout.activity_settings)

        logoutButton = findViewById(R.id.logout_button)
        goBackButton = findViewById(R.id.go_back_button)
        userEmailText = findViewById(R.id.user_email_text)
        autoStartToggle = findViewById(R.id.auto_start_toggle)

        // Check if user is logged in
        val token = getToken()
        if (token == null) {
            Log.w(TAG, "onCreate: No token found, redirecting to login")
            RegisterActivity.start(this)
            finish()
            return
        }

        // Display user email if available
        val email = getStoredEmail()
        if (email != null) {
            userEmailText.text = "Logged in as: $email"
        } else {
            userEmailText.text = "Logged in"
        }

        // Setup auto-start toggle
        setupAutoStartToggle()

        goBackButton.setOnClickListener {
            Log.d(TAG, "Go back button clicked")
            finish()
        }

        logoutButton.setOnClickListener {
            Log.d(TAG, "Logout button clicked")
            performLogout()
        }
    }

    private fun performLogout() {
        // Stop location service before clearing token
        if (LocationForegroundService.isRunning()) {
            val intent = Intent(this, LocationForegroundService::class.java)
            stopService(intent)
        }

        // Clear all stored data
        clearToken()
        clearStoredEmail()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Navigate back to login screen
        RegisterActivity.start(this)
        finish()
    }

    private fun getToken(): String? {
        val prefs = getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        Log.d(TAG, "getToken: Token present: ${token != null}")
        return token
    }

    private fun getStoredEmail(): String? {
        val prefs = getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
        return prefs.getString("user_email", null)
    }

    private fun clearToken() {
        Log.d(TAG, "clearToken: Removing token from SharedPreferences")
        val prefs = getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("token").apply()
    }

    private fun clearStoredEmail() {
        Log.d(TAG, "clearStoredEmail: Removing email from SharedPreferences")
        val prefs = getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("user_email").apply()
    }

    private fun setupAutoStartToggle() {
        // Load current auto-start setting
        val isAutoStartEnabled = isAutoStartEnabled()
        autoStartToggle.isChecked = isAutoStartEnabled
        Log.d(TAG, "setupAutoStartToggle: Auto-start is ${if (isAutoStartEnabled) "enabled" else "disabled"}")

        // Setup toggle listener
        autoStartToggle.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "setupAutoStartToggle: Auto-start toggle changed to: $isChecked")
            setAutoStartEnabled(isChecked)

            val message = if (isChecked) {
                "Auto-start enabled - location tracking will start on device boot"
            } else {
                "Auto-start disabled - location tracking will not start automatically"
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun isAutoStartEnabled(): Boolean {
        val prefs = getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_start_enabled", true) // Default to true
    }

    private fun setAutoStartEnabled(enabled: Boolean) {
        Log.d(TAG, "setAutoStartEnabled: Setting auto-start to: $enabled")
        val prefs = getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_start_enabled", enabled).apply()
    }
}
