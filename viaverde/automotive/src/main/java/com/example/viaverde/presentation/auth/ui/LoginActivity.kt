package com.example.viaverde.presentation.auth.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.viaverde.R
import com.example.viaverde.core.security.SecurityUtils
import com.example.viaverde.domain.usecase.auth.LoginUseCase
import com.example.viaverde.presentation.main.ui.MainActivity
import com.example.viaverde.service.LocationForegroundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Login activity for user authentication
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var loginUseCase: LoginUseCase

    private lateinit var emailInput: EditText
    private lateinit var loginButton: Button
    private lateinit var passwordInput: EditText
    private lateinit var passwordToggle: ImageButton
    private var isPasswordVisible = false

    companion object {
        private const val TAG = "LoginActivity"

        fun start(context: android.content.Context) {
            Log.d(TAG, "start: Starting LoginActivity")
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register) // Using existing layout

        Log.d(TAG, "onCreate: Activity started")

        // Initialize views
        emailInput = findViewById(R.id.email_input)
        loginButton = findViewById(R.id.register_button)
        passwordInput = findViewById(R.id.password_input)
        passwordToggle = findViewById(R.id.password_toggle)

        Log.d(TAG, "onCreate: Views initialized")

        // Check if user is already logged in
        if (isLoggedIn()) {
            Log.d(TAG, "onCreate: User is already logged in")
            // User is already logged in, go to main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        } else {
            Log.d(TAG, "onCreate: User is not logged in")
        }

        // Password visibility toggle
        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            Log.d(TAG, "Password visibility toggled: $isPasswordVisible")

            if (isPasswordVisible) {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle.setImageResource(android.R.drawable.ic_menu_view)
            }

            // Keep cursor position
            passwordInput.setSelection(passwordInput.text.length)
        }

        // Login button click handler
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            Log.d(TAG, "Login button clicked - Email: ${SecurityUtils.maskEmail(email)}, Password length: ${password.length}")

            if (email.isEmpty() || password.isEmpty()) {
                Log.w(TAG, "Login attempt failed: Empty email or password")
                Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Starting login process for email: ${SecurityUtils.maskEmail(email)}")
                performLogin(email, password)
            }
        }
    }

    private fun performLogin(email: String, password: String) {
        loginButton.isEnabled = false
        loginButton.text = "Logging in..."

        lifecycleScope.launch {
            try {
                val result = loginUseCase(email, password)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Login successful")
                        runOnUiThread {
                            resetLoginButton()
                            Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()

                            // Start location service after successful login
                            if (!LocationForegroundService.isRunning()) {
                                val intent = Intent(this@LoginActivity, LocationForegroundService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(intent)
                                } else {
                                    startService(intent)
                                }
                            }

                            // Navigate to main activity
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Login failed", exception)
                        runOnUiThread {
                            resetLoginButton()
                            Toast.makeText(this@LoginActivity, "Login failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Login failed", e)
                runOnUiThread {
                    resetLoginButton()
                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isLoggedIn(): Boolean {
        // This should be implemented based on your auth repository
        // For now, return false to always show login screen
        return false
    }

    private fun resetLoginButton() {
        loginButton.isEnabled = true
        loginButton.text = "Login"
    }
}
