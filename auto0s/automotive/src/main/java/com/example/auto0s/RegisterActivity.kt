package com.example.auto0s

import android.content.Context
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RegisterActivity"

        fun start(context: Context) {
            Log.d(TAG, "start: Starting RegisterActivity")
            val intent = Intent(context, RegisterActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var logoutButton: Button
    private lateinit var passwordToggle: ImageButton
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity started")
        setContentView(R.layout.activity_register)

        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        registerButton = findViewById(R.id.register_button)
        logoutButton = findViewById(R.id.logout_button)
        passwordToggle = findViewById(R.id.password_toggle)

        Log.d(TAG, "onCreate: Views initialized")

        val token = getToken()
        if (token != null) {
            Log.d(TAG, "onCreate: User is already logged in")
            // User is logged in
            emailInput.visibility = View.GONE
            passwordInput.visibility = View.GONE
            passwordToggle.visibility = View.GONE
            registerButton.visibility = View.GONE
            logoutButton.visibility = View.VISIBLE
        } else {
            Log.d(TAG, "onCreate: User is not logged in")
            logoutButton.visibility = View.GONE
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

            // Move cursor to end to maintain position
            passwordInput.setSelection(passwordInput.text.length)
        }

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            Log.d(TAG, "Login button clicked - Email: $email, Password length: ${password.length}")

            if (email.isEmpty() || password.isEmpty()) {
                Log.w(TAG, "Login attempt failed: Empty email or password")
                Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Starting login process for email: $email")
                loginUser(email, password)
            }
        }

        // Remove logout button functionality since it's now in Settings
        logoutButton.visibility = View.GONE
    }

    private fun loginUser(email: String, password: String) {
        Log.d(TAG, "loginUser: Starting login request for email: $email")

        val client = OkHttpClient()
        val json = JSONObject()
        json.put("email", email)
        json.put("password", password)
        val body = json.toString().toRequestBody("application/json".toMediaType())

        Log.d(TAG, "loginUser: Request body created: ${json.toString()}")

        val request = Request.Builder()
            .url("https://dev.a-to-be.com/mtolling/services/mtolling/login")
            .post(body)
            .build()

        Log.d(TAG, "loginUser: Sending request to: ${request.url}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Login request failed", e)
                runOnUiThread {
                    resetLoginButton()
                    Toast.makeText(this@RegisterActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Login response received - Code: ${response.code}, Body: $responseBody")

                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        // Try to get token from different possible field names
                        val token = if (jsonResponse.has("authToken")) jsonResponse.getString("authToken")
                            else if (jsonResponse.has("token")) jsonResponse.getString("token")
                            else if (jsonResponse.has("access_token")) jsonResponse.getString("access_token")
                            else null
                        Log.d(TAG, "Login response parsed - Token present: ${token != null}")

                        if (token != null && token.isNotEmpty()) {
                            Log.d(TAG, "Login successful - Token saved")
                            saveToken(token)
                            saveUserEmail(email)
                            runOnUiThread {
                                resetLoginButton()
                                Toast.makeText(this@RegisterActivity, "Login successful", Toast.LENGTH_SHORT).show()
                                // Start location service after successful login
                                if (!LocationForegroundService.isRunning()) {
                                    val intent = Intent(this@RegisterActivity, LocationForegroundService::class.java)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(intent)
                                    } else {
                                        startService(intent)
                                    }
                                }
                                // Navigate to main activity
                                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            Log.w(TAG, "Login failed: No token in response")
                            runOnUiThread {
                                resetLoginButton()
                                Toast.makeText(this@RegisterActivity, "No token returned", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Login failed: Invalid JSON response", e)
                        runOnUiThread {
                            resetLoginButton()
                            Toast.makeText(this@RegisterActivity, "Invalid response", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.w(TAG, "Login failed: HTTP ${response.code}")
                    runOnUiThread {
                        resetLoginButton()
                        Toast.makeText(this@RegisterActivity, "Login failed: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun saveToken(token: String) {
        Log.d(TAG, "saveToken: Saving token to SharedPreferences")
        val prefs = getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("token", token).apply()
    }

    private fun saveUserEmail(email: String) {
        Log.d(TAG, "saveUserEmail: Saving email to SharedPreferences")
        val prefs = getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_email", email).apply()
    }

    private fun getToken(): String? {
        val prefs = getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        Log.d(TAG, "getToken: Token present: ${token != null}")
        return token
    }

    private fun clearToken() {
        Log.d(TAG, "clearToken: Removing token from SharedPreferences")
        val prefs = getSharedPreferences("via_verde_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("token").apply()
    }

    private fun resetLoginButton() {
        registerButton.isEnabled = true
        registerButton.text = "Login"
    }
}
