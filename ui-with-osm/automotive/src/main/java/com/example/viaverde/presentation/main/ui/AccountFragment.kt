package com.example.viaverde.presentation.main.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.viaverde.R
import com.example.viaverde.core.security.SecurityUtils
import com.example.viaverde.domain.usecase.auth.LoginUseCase
import com.example.viaverde.domain.usecase.auth.LogoutUseCase
import com.example.viaverde.presentation.auth.ui.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Account fragment containing user account information and logout
 */
@AndroidEntryPoint
class AccountFragment : Fragment() {

    @Inject
    lateinit var logoutUseCase: LogoutUseCase

    @Inject
    lateinit var loginUseCase: LoginUseCase

    private lateinit var logoutButton: Button
    private lateinit var userEmailText: TextView

    companion object {
        private const val TAG = "AccountFragment"

        fun newInstance(): AccountFragment {
            return AccountFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        logoutButton = view.findViewById(R.id.logout_button)
        userEmailText = view.findViewById(R.id.user_email_text)

        // Setup logout button
        logoutButton.setOnClickListener {
            Log.d(TAG, "Logout button clicked")
            performLogout()
        }

        // Check if user is logged in and load user data
        lifecycleScope.launch {
            if (!loginUseCase.isLoggedIn()) {
                Log.w(TAG, "onViewCreated: No token found, redirecting to login")
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                return@launch
            }

            // Display user email if available
            val user = loginUseCase.getCurrentUser()
            if (user != null) {
                Log.d(TAG, "onViewCreated: User email found (masked: ${SecurityUtils.maskEmail(user.email)})")
                val maskedEmail = SecurityUtils.maskEmail(user.email)
                userEmailText.text = maskedEmail
            } else {
                Log.d(TAG, "onViewCreated: No user email found")
                userEmailText.text = "User information not available"
            }
        }
    }

    private fun performLogout() {
        // Perform logout using coroutine
        lifecycleScope.launch {
            try {
                logoutUseCase.invoke()
                Log.d(TAG, "Logout successful")

                // Navigate to login activity
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Logout failed", e)
                // Still navigate to login even if logout fails
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }
}
