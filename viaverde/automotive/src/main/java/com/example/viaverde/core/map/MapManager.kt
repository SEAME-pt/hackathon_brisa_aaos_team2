package com.example.viaverde.core.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.util.Log
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapManager {

    companion object {
        private const val TAG = "MapManager"
        private const val DEFAULT_ZOOM = 15.0
        private const val DEFAULT_LATITUDE = 38.7223 // Lisbon coordinates
        private const val DEFAULT_LONGITUDE = -9.1393
    }

    private var mapView: MapView? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private val tollPointMarkers = mutableListOf<Marker>()
    private var currentLocation: Location? = null

    /**
     * Get current location
     */
    fun getCurrentLocation(): Location? = currentLocation

    /**
     * Initialize the map
     */
    fun initializeMap(context: Context, mapView: MapView) {
        Log.d(TAG, "initializeMap: Initializing map")

        this.mapView = mapView

        try {
            // Configure osmdroid
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

            // Set up map
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)

            // Set default location (Lisbon)
            val defaultLocation = GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            mapView.controller.setZoom(DEFAULT_ZOOM)
            mapView.controller.setCenter(defaultLocation)

            Log.d(TAG, "initializeMap: Map initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "initializeMap: Error initializing map", e)

            // Clear corrupted preferences and retry
            try {
                context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE).edit().clear().apply()
                Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                Log.d(TAG, "initializeMap: Retried with cleared preferences")
            } catch (retryException: Exception) {
                Log.e(TAG, "initializeMap: Failed to retry initialization", retryException)
            }
        }
    }

    /**
     * Set up location overlay
     */
    fun setupLocationOverlay(locationManager: android.location.LocationManager) {
        Log.d(TAG, "setupLocationOverlay: Setting up location overlay")

        mapView?.let { map ->
            myLocationOverlay = MyLocationNewOverlay(object : org.osmdroid.views.overlay.mylocation.IMyLocationProvider {
                override fun getLastKnownLocation(): Location? = currentLocation
                override fun destroy() {}
                override fun startLocationProvider(myLocationConsumer: org.osmdroid.views.overlay.mylocation.IMyLocationConsumer): Boolean = true
                override fun stopLocationProvider() {}
            }, map)

            myLocationOverlay?.setDrawAccuracyEnabled(true)
            myLocationOverlay?.enableMyLocation() // Enable the default location indicator
            map.overlays.add(myLocationOverlay)
            map.invalidate()

            // Try to center on last known location if available
            centerOnLastKnownLocation(locationManager)

            Log.d(TAG, "setupLocationOverlay: Location overlay set up successfully")
        }
    }

    /**
     * Update user location on map
     */
    fun updateUserLocation(location: Location) {
        Log.d(TAG, "updateUserLocation: Updating user location")

        val isFirstLocation = currentLocation == null
        currentLocation = location

        mapView?.let { map ->
            // Update location overlay (this will show the default location indicator)
            myLocationOverlay?.onLocationChanged(location, null)

            // Center map on current location if this is the first location received
            if (isFirstLocation) {
                Log.d(TAG, "updateUserLocation: First location received, centering map")
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                map.controller.animateTo(geoPoint)
            }

            map.invalidate()
            Log.d(TAG, "updateUserLocation: User location updated successfully")
        }
    }

    /**
     * Add toll point marker
     */
    fun addTollPointMarker(id: String, name: String, latitude: Double, longitude: Double) {
        Log.d(TAG, "addTollPointMarker: Adding toll point marker for $name")

        mapView?.let { map ->
            val geoPoint = GeoPoint(latitude, longitude)
            val marker = Marker(map).apply {
                position = geoPoint
                title = name
                snippet = "Toll Point"
                icon = createMarkerBitmap(Color.parseColor("#4CAF50")) // Green color
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }

            tollPointMarkers.add(marker)
            map.overlays.add(marker)
            map.invalidate()

            Log.d(TAG, "addTollPointMarker: Toll point marker added successfully")
        }
    }

    /**
     * Clear all toll point markers
     */
    fun clearTollPointMarkers() {
        Log.d(TAG, "clearTollPointMarkers: Clearing all toll point markers")

        mapView?.let { map ->
            map.overlays.removeAll(tollPointMarkers)
            tollPointMarkers.clear()
            map.invalidate()

            Log.d(TAG, "clearTollPointMarkers: All toll point markers cleared")
        }
    }

    /**
     * Center map on user location
     */
    fun centerOnUserLocation() {
        Log.d(TAG, "centerOnUserLocation: Centering map on user location")

        currentLocation?.let { location ->
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            mapView?.controller?.animateTo(geoPoint)
            Log.d(TAG, "centerOnUserLocation: Map centered on user location")
        } ?: run {
            Log.w(TAG, "centerOnUserLocation: No current location available")
        }
    }

    /**
     * Center map on last known location from location manager
     */
    fun centerOnLastKnownLocation(locationManager: android.location.LocationManager) {
        Log.d(TAG, "centerOnLastKnownLocation: Attempting to center on last known location")

        try {
            // Try to get last known location from GPS provider
            val lastKnownLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

            lastKnownLocation?.let { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                mapView?.controller?.animateTo(geoPoint)
                currentLocation = location
                Log.d(TAG, "centerOnLastKnownLocation: Map centered on last known location")
            } ?: run {
                Log.w(TAG, "centerOnLastKnownLocation: No last known location available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "centerOnLastKnownLocation: Error getting last known location", e)
        }
    }

    /**
     * Create marker bitmap for toll points
     */
    private fun createMarkerBitmap(color: Int): android.graphics.drawable.Drawable {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }

        // Draw circle
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

        return android.graphics.drawable.BitmapDrawable(bitmap)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        Log.d(TAG, "cleanup: Cleaning up map resources")

        tollPointMarkers.clear()
        currentLocation = null
        myLocationOverlay = null
        mapView = null

        Log.d(TAG, "cleanup: Map resources cleaned up")
    }
}
