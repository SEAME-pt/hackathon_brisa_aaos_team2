package com.example.viaverde.presentation.main.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.example.viaverde.R

/**
 * Settings fragment containing the settings interface
 */
class SettingsFragment : Fragment() {

    private lateinit var autoStartToggle: Switch

    companion object {
        private const val TAG = "SettingsFragment"

        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        autoStartToggle = view.findViewById(R.id.auto_start_toggle)

        // Load saved auto-start state
        loadAutoStartState()

        // Setup auto-start toggle
        autoStartToggle.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Auto-start toggle changed to: $isChecked")

            // Apply green color when active
            if (isChecked) {
                autoStartToggle.thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.via_verde_green, null))
                autoStartToggle.trackTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.via_verde_green, null))
            } else {
                autoStartToggle.thumbTintList = null
                autoStartToggle.trackTintList = null
            }

            // Save auto-start state
            saveAutoStartState(isChecked)
        }
    }

    private fun loadAutoStartState() {
        val sharedPrefs = requireActivity().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val isAutoStartEnabled = sharedPrefs.getBoolean("auto_start_enabled", false)

        Log.d(TAG, "loadAutoStartState: Loading auto-start state: $isAutoStartEnabled")

        // Set the toggle state without triggering the listener
        autoStartToggle.setOnCheckedChangeListener(null)
        autoStartToggle.isChecked = isAutoStartEnabled

        // Apply color based on loaded state
        if (isAutoStartEnabled) {
            autoStartToggle.thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.via_verde_green, null))
            autoStartToggle.trackTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.via_verde_green, null))
        } else {
            autoStartToggle.thumbTintList = null
            autoStartToggle.trackTintList = null
        }

        // Re-attach the listener
        setupAutoStartToggleListener()
    }

    private fun saveAutoStartState(isEnabled: Boolean) {
        val sharedPrefs = requireActivity().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putBoolean("auto_start_enabled", isEnabled)
        editor.apply()

        Log.d(TAG, "saveAutoStartState: Auto-start state saved: $isEnabled")
    }

    private fun setupAutoStartToggleListener() {
        autoStartToggle.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Auto-start toggle changed to: $isChecked")

            // Apply green color when active
            if (isChecked) {
                autoStartToggle.thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.via_verde_green, null))
                autoStartToggle.trackTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.via_verde_green, null))
            } else {
                autoStartToggle.thumbTintList = null
                autoStartToggle.trackTintList = null
            }

            // Save auto-start state
            saveAutoStartState(isChecked)
        }
    }
}
