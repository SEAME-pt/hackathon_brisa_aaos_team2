package com.example.viaverde.presentation.main.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.viaverde.R
import com.example.viaverde.data.model.TollPoint
import com.example.viaverde.domain.repository.AuthRepository
import com.example.viaverde.domain.repository.TollRepository
import com.example.viaverde.core.map.MapManager
import com.example.viaverde.core.permission.PermissionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Home fragment containing the main dashboard with map and current location
 */
@AndroidEntryPoint
class HomeFragment : Fragment(), LocationListener {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var tollRepository: TollRepository

    @Inject
    lateinit var mapManager: MapManager

    @Inject
    lateinit var permissionManager: PermissionManager

    private var mapView: MapView? = null
    private var btnCurrentLocation: FloatingActionButton? = null
    private var locationManager: LocationManager? = null
    private var currentLocation: Location? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d(TAG, "Location permissions granted")
            startLocationUpdates()
        } else {
            Log.w(TAG, "Location permissions denied")
            Toast.makeText(context, "Location permission is required to show your position on the map", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupMap()
        setupLocationUpdates()
        setupTollPoints()
    }

    private fun initializeViews(view: View) {
        mapView = view.findViewById(R.id.mapView)
        btnCurrentLocation = view.findViewById(R.id.btnCurrentLocation)

        btnCurrentLocation?.setOnClickListener {
            // Try to center on current location, fallback to last known location
            if (mapManager.getCurrentLocation() != null) {
                mapManager.centerOnUserLocation()
            } else {
                locationManager?.let { manager ->
                    mapManager.centerOnLastKnownLocation(manager)
                }
            }
        }
    }

    private fun setupMap() {
        mapView?.let { map ->
            mapManager.initializeMap(requireContext(), map)
        }
    }

    private fun setupLocationUpdates() {
        if (!permissionManager.hasBasicLocationPermissions(requireContext())) {
            Log.d(TAG, "setupLocationUpdates: Requesting location permissions")
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates: Starting location updates")

        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationManager?.let { manager ->
            mapManager.setupLocationOverlay(manager)

            try {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    manager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000L, // 5 seconds
                        10f,   // 10 meters
                        this
                    )
                    Log.d(TAG, "startLocationUpdates: GPS location updates started")
                }
            } catch (e: Exception) {
                Log.e(TAG, "startLocationUpdates: Error starting GPS updates", e)
            }
        }
    }

    private fun setupTollPoints() {
        lifecycleScope.launch {
            delay(500) // Small delay to ensure map is ready
            fetchAndDisplayTollPoints()
        }
    }

    private suspend fun fetchAndDisplayTollPoints() {
        try {
            Log.d(TAG, "fetchAndDisplayTollPoints: Fetching toll points")

            // Get auth token
            val authToken = authRepository.getAuthToken()
            if (authToken == null) {
                Log.w(TAG, "No auth token available for fetching toll points")
                return
            }

            val result = tollRepository.getTollPoints(authToken.token)
            result.fold(
                onSuccess = { tollPoints ->
                    Log.d(TAG, "Successfully fetched ${tollPoints.size} toll points")
                    displayTollPoints(tollPoints)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Failed to fetch toll points", exception)
                    Toast.makeText(context, "Failed to load toll points: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchAndDisplayTollPoints: Error fetching toll points", e)
        }
    }

    private fun displayTollPoints(tollPoints: List<TollPoint>) {
        Log.d(TAG, "displayTollPoints: Displaying ${tollPoints.size} toll points")

        // Clear existing markers
        mapManager.clearTollPointMarkers()

        // Add new markers
        tollPoints.forEach { tollPoint ->
            mapManager.addTollPointMarker(
                id = tollPoint.id,
                name = tollPoint.name,
                latitude = tollPoint.latitude,
                longitude = tollPoint.longitude
            )
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")

        // Update map with new location
        mapManager.updateUserLocation(location)
    }

    // Required for LocationListener interface (deprecated but still needed)
    @Deprecated("Deprecated in API level 29, but required for LocationListener interface")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    @Deprecated("Deprecated in API level 29, but required for LocationListener interface")
    override fun onProviderEnabled(provider: String) {}

    @Deprecated("Deprecated in API level 29, but required for LocationListener interface")
    override fun onProviderDisabled(provider: String) {}

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationManager?.removeUpdates(this)
        mapView?.onDetach()
    }

    companion object {
        private const val TAG = "HomeFragment"

        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}
