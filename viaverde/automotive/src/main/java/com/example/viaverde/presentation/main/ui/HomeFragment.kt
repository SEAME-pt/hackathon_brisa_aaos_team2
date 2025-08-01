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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
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

    private var mapView: MapView? = null
    private var btnCurrentLocation: FloatingActionButton? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var locationManager: LocationManager? = null
    private var currentLocation: Location? = null
    private var customLocationProvider: IMyLocationProvider? = null
    private var locationMarker: Marker? = null

    // Custom markers list
    private val customMarkers = mutableListOf<Marker>()

    // Toll points markers list
    private val tollPointMarkers = mutableListOf<Marker>()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d(TAG, "Location permissions granted")
            setupLocationUpdates()
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

        // Initialize osmdroid configuration with error handling
        try {
            Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load osmdroid configuration, using defaults", e)
            // Clear corrupted preferences and use defaults
            try {
                requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE).edit().clear().apply()
                Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            } catch (e2: Exception) {
                Log.w(TAG, "Failed to reload osmdroid configuration, continuing with defaults", e2)
                // Continue with default configuration
            }
        }

        // Initialize views
        mapView = view.findViewById(R.id.mapView)
        btnCurrentLocation = view.findViewById(R.id.btnCurrentLocation)

        setupMap()
        setupLocationButton()
        checkLocationPermissions()

        // Delay toll points fetch to ensure map is properly initialized
        lifecycleScope.launch {
            delay(500) // Small delay to ensure map initialization
            fetchTollPoints()
        }
    }

    private fun setupMap() {
        mapView?.let { map ->
            // Set tile source to OpenStreetMap
            map.setTileSource(TileSourceFactory.MAPNIK)

            // Set default zoom level
            map.controller.setZoom(15.0)

            // Set default location (you can set this to a default location)
            map.controller.setCenter(GeoPoint(38.7223, -9.1393)) // Lisbon, Portugal as default

            // Create custom location provider
            customLocationProvider = object : IMyLocationProvider {
                override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean {
                    return true
                }

                override fun stopLocationProvider() {
                    // Stop location updates
                }

                override fun getLastKnownLocation(): Location? {
                    return currentLocation
                }

                override fun destroy() {
                    // Cleanup
                }
            }

            // Add my location overlay with custom provider
            myLocationOverlay = MyLocationNewOverlay(customLocationProvider, map)
            map.overlays.add(myLocationOverlay)

            // Enable location overlay
            myLocationOverlay?.enableMyLocation()
            myLocationOverlay?.enableFollowLocation()

            // Set overlay options for better visibility
            myLocationOverlay?.setDrawAccuracyEnabled(true)

            Log.d(TAG, "Location overlay setup complete")
        }
    }

    private fun setupLocationButton() {
        btnCurrentLocation?.setOnClickListener {
            currentLocation?.let { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                mapView?.controller?.animateTo(geoPoint)
                mapView?.controller?.setZoom(18.0)
                Toast.makeText(context, "Centered on your current location", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            setupLocationUpdates()
        }
    }

    private fun setupLocationUpdates() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Enable the location overlay
            myLocationOverlay?.enableMyLocation()

            // Check if GPS provider is available and enabled
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L, // 1 second
                        10f,   // 10 meters
                        this
                    )
                    Log.d(TAG, "GPS location updates started")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to request GPS location updates", e)
                }
            } else {
                Log.w(TAG, "GPS provider is not available or disabled")
            }

            // Check if network provider is available and enabled
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000L, // 1 second
                        10f,
                        this
                    )
                    Log.d(TAG, "Network location updates started")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to request network location updates", e)
                }
            } else {
                Log.w(TAG, "Network provider is not available or disabled")
            }

            // If no providers are available, show a message
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) != true &&
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) != true) {
                Log.w(TAG, "No location providers are available")
                Toast.makeText(context, "Location services are not available on this device", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")

        // Update map to show current location
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        mapView?.controller?.animateTo(geoPoint)

        // Add a visible marker at current location as backup
        mapView?.let { map ->
            // Remove previous marker
            locationMarker?.let { marker ->
                map.overlays.remove(marker)
            }

            // Add new marker
            locationMarker = Marker(map).apply {
                position = geoPoint
                title = "Current Location"
                snippet = "You are here"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // Set custom white icon with black border for current location
                setIcon(android.graphics.drawable.BitmapDrawable(resources, createMarkerBitmapWithBorder(android.graphics.Color.WHITE, android.graphics.Color.BLACK)))
            }
            map.overlays.add(locationMarker)
        }

        // Invalidate the map to refresh the overlay
        mapView?.invalidate()

        // Debug: Check if overlay is enabled
        Log.d(TAG, "Location overlay enabled: ${myLocationOverlay?.isMyLocationEnabled}")
        Log.d(TAG, "Location overlay following: ${myLocationOverlay?.isFollowLocationEnabled}")
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

    /**
     * Add a custom marker to the map
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param title Marker title
     * @param description Marker description
     * @param markerColor Color for the marker (optional, defaults to red)
     */
    fun addCustomMarker(
        latitude: Double,
        longitude: Double,
        title: String,
        description: String = "",
        markerColor: Int = android.graphics.Color.RED
    ) {
        mapView?.let { map ->
            val geoPoint = GeoPoint(latitude, longitude)
            val marker = Marker(map).apply {
                position = geoPoint
                this.title = title
                snippet = description
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // Set custom icon (you can replace this with a custom drawable)
                setIcon(android.graphics.drawable.BitmapDrawable(resources, createMarkerBitmap(markerColor)))
            }

            customMarkers.add(marker)
            map.overlays.add(marker)
            map.invalidate()

            Log.d(TAG, "Added custom marker: $title at ($latitude, $longitude)")
        }
    }

    /**
     * Remove a specific marker by title
     * @param title The title of the marker to remove
     */
    fun removeCustomMarker(title: String) {
        val markerToRemove = customMarkers.find { it.title == title }
        markerToRemove?.let { marker ->
            mapView?.overlays?.remove(marker)
            customMarkers.remove(marker)
            mapView?.invalidate()
            Log.d(TAG, "Removed custom marker: $title")
        }
    }

    /**
     * Remove all custom markers
     */
    fun removeAllCustomMarkers() {
        customMarkers.forEach { marker ->
            mapView?.overlays?.remove(marker)
        }
        customMarkers.clear()
        mapView?.invalidate()
        Log.d(TAG, "Removed all custom markers")
    }

    /**
     * Refresh toll points from API
     */
    fun refreshTollPoints() {
        fetchTollPoints()
    }

    /**
     * Create a simple colored marker bitmap
     */
    private fun createMarkerBitmap(color: Int): android.graphics.Bitmap {
        val size = 48
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            this.color = color
            isAntiAlias = true
        }

        // Draw a circle for the marker
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

        // Add a border
        paint.color = android.graphics.Color.WHITE
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3f, paint)

        return bitmap
    }

    /**
     * Create a marker bitmap with custom border color
     */
    private fun createMarkerBitmapWithBorder(fillColor: Int, borderColor: Int): android.graphics.Bitmap {
        val size = 48
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }

        // Draw the border first
        paint.color = borderColor
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

        // Draw the fill color
        paint.color = fillColor
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)

        return bitmap
    }

    /**
     * Add a toll point marker (example usage)
     */
    fun addTollPoint(latitude: Double, longitude: Double, tollName: String) {
        addCustomMarker(
            latitude = latitude,
            longitude = longitude,
            title = "Toll: $tollName",
            description = "Toll collection point",
            markerColor = android.graphics.Color.parseColor("#FFA500") // Orange color
        )
    }

    /**
     * Add a service area marker (example usage)
     */
    fun addServiceArea(latitude: Double, longitude: Double, areaName: String) {
        addCustomMarker(
            latitude = latitude,
            longitude = longitude,
            title = "Service Area: $areaName",
            description = "Rest area and services",
            markerColor = android.graphics.Color.BLUE
        )
    }

    /**
     * Fetch toll points from API and display them on the map
     */
    private fun fetchTollPoints() {
        lifecycleScope.launch {
            try {
                // Get auth token
                val authToken = authRepository.getAuthToken()
                if (authToken == null) {
                    Log.w(TAG, "No auth token available for fetching toll points")
                    return@launch
                }

                // Fetch toll points
                val result = tollRepository.getTollPoints(authToken.token)
                result.fold(
                    onSuccess = { tollPoints ->
                        Log.d(TAG, "Successfully fetched ${tollPoints.size} toll points")
                        displayTollPoints(tollPoints)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to fetch toll points", exception)
                        val errorMessage = when {
                            exception.message?.contains("Unable to resolve host") == true ->
                                "Network error: Cannot reach server. Please check your internet connection."
                            exception.message?.contains("timeout") == true ->
                                "Request timeout. Please try again."
                            else -> "Failed to load toll points: ${exception.message}"
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching toll points", e)
                val errorMessage = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "Network error: Cannot reach server. Please check your internet connection."
                    e.message?.contains("timeout") == true ->
                        "Request timeout. Please try again."
                    else -> "Error loading toll points: ${e.message}"
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()

                // For testing purposes, you can uncomment this to add sample toll points
                // addSampleTollPoints()
            }
        }
    }

    /**
     * Display toll points on the map
     */
    private fun displayTollPoints(tollPoints: List<TollPoint>) {
        // Clear existing toll point markers
        clearTollPointMarkers()

        // Add new toll point markers
        tollPoints.forEach { tollPoint ->
            addTollPointMarker(tollPoint)
        }

        Log.d(TAG, "Displayed ${tollPoints.size} toll points on map")
    }

    /**
     * Add a toll point marker to the map
     */
    private fun addTollPointMarker(tollPoint: TollPoint) {
        mapView?.let { map ->
            val geoPoint = GeoPoint(tollPoint.latitude, tollPoint.longitude)
            val marker = Marker(map).apply {
                position = geoPoint
                title = tollPoint.name
                snippet = tollPoint.description ?: "Toll collection point"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // Set custom icon for toll points (green color)
                setIcon(android.graphics.drawable.BitmapDrawable(resources, createMarkerBitmap(android.graphics.Color.parseColor("#4CAF50"))))
            }

            tollPointMarkers.add(marker)
            map.overlays.add(marker)
            map.invalidate()

            Log.d(TAG, "Added toll point marker: ${tollPoint.name} at (${tollPoint.latitude}, ${tollPoint.longitude})")
        }
    }

    /**
     * Clear all toll point markers from the map
     */
    private fun clearTollPointMarkers() {
        tollPointMarkers.forEach { marker ->
            mapView?.overlays?.remove(marker)
        }
        tollPointMarkers.clear()
        mapView?.invalidate()
        Log.d(TAG, "Cleared all toll point markers")
    }

    /**
     * Add sample toll points for testing when API is not available
     */
    private fun addSampleTollPoints() {
        val sampleTollPoints = listOf(
            TollPoint(
                id = "sample1",
                name = "Sample Toll Point 1",
                latitude = 38.7223,
                longitude = -9.1393,
                description = "Sample toll collection point",
                isActive = true
            ),
            TollPoint(
                id = "sample2",
                name = "Sample Toll Point 2",
                latitude = 38.7500,
                longitude = -9.1500,
                description = "Another sample toll point",
                isActive = true
            )
        )
        displayTollPoints(sampleTollPoints)
        Log.d(TAG, "Added ${sampleTollPoints.size} sample toll points for testing")
    }
}
