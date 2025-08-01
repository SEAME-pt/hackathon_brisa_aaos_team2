package com.example.viaverde.presentation.main.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.viaverde.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings fragment containing the settings interface
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    companion object {
        private const val TAG = "SettingsFragment"
        private const val PREFS_NAME = "settings"
        private const val KEY_AUTO_START = "auto_start_enabled"
        private const val KEY_TRIP_MONITORING = "trip_monitoring_enabled"

        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }

    private lateinit var autoStartToggle: Switch
    private lateinit var tripMonitoringToggle: Switch

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
        tripMonitoringToggle = view.findViewById(R.id.tripMonitoringToggle)

        // Load saved states
        loadAutoStartState()
        loadTripMonitoringState()

        // Setup toggles
        setupAutoStartToggleListener()
        setupTripMonitoringToggleListener()
    }

    private fun loadAutoStartState() {
        val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isAutoStartEnabled = sharedPrefs.getBoolean(KEY_AUTO_START, false)

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

    private fun loadTripMonitoringState() {
        val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isTripMonitoringEnabled = sharedPrefs.getBoolean(KEY_TRIP_MONITORING, false)

        Log.d(TAG, "loadTripMonitoringState: Loading trip monitoring state: $isTripMonitoringEnabled")

        // Set the toggle state without triggering the listener
        tripMonitoringToggle.setOnCheckedChangeListener(null)
        tripMonitoringToggle.isChecked = isTripMonitoringEnabled

        // Apply color based on loaded state
        if (isTripMonitoringEnabled) {
            tripMonitoringToggle.thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.via_verde_green, null))
            tripMonitoringToggle.trackTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.via_verde_green, null))
        } else {
            tripMonitoringToggle.thumbTintList = null
            tripMonitoringToggle.trackTintList = null
        }

        // Re-attach the listener
        setupTripMonitoringToggleListener()
    }

    private fun saveAutoStartState(isEnabled: Boolean) {
        val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putBoolean(KEY_AUTO_START, isEnabled)
        editor.apply()

        Log.d(TAG, "saveAutoStartState: Auto-start state saved: $isEnabled")
    }

    private fun saveTripMonitoringState(isEnabled: Boolean) {
        val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putBoolean(KEY_TRIP_MONITORING, isEnabled)
        editor.apply()

        Log.d(TAG, "saveTripMonitoringState: Trip monitoring state saved: $isEnabled")
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

            // Show appropriate message based on auto-start setting
            if (isChecked) {
                Toast.makeText(context, "Auto-start enabled: Service will start automatically on boot", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Auto-start disabled: Service will not start automatically on boot", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupTripMonitoringToggleListener() {
        tripMonitoringToggle.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Trip monitoring toggle changed to: $isChecked")

            // Apply green color when active
            if (isChecked) {
                tripMonitoringToggle.thumbTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.via_verde_green, null))
                tripMonitoringToggle.trackTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.via_verde_green, null))
            } else {
                tripMonitoringToggle.thumbTintList = null
                tripMonitoringToggle.trackTintList = null
            }

            // Save trip monitoring state
            saveTripMonitoringState(isChecked)

            // Show appropriate message based on trip monitoring setting
            if (isChecked) {
                Toast.makeText(context, "Trip monitoring enabled: Service will monitor your trips", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Trip monitoring disabled: Service will not monitor your trips", Toast.LENGTH_LONG).show()
            }
        }
    }
}
