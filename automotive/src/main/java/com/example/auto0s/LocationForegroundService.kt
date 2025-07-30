package com.example.auto0s

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

class LocationForegroundService : Service() {

        companion object {
        private const val TAG = "LocationForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_service_channel"
        private const val CHANNEL_NAME = "Via Verde"
        private const val CHANNEL_DESCRIPTION = "Service for tracking vehicle location in real-time"

        // Location update intervals
        private const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
        private const val LOCATION_MIN_DISTANCE = 10f // 10 meters

        // Static flag to track service status
        @Volatile
        private var isServiceRunning = false

        fun isRunning(): Boolean = isServiceRunning
    }

    private lateinit var locationManager: LocationManager
    private var currentLocation: Location? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")

            // Update notification with current location
            updateNotification()

            // Here you can send the location to your backend server
            sendLocationToServer(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d(TAG, "Location provider status changed: $provider, status: $status")
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "Location provider enabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "Location provider disabled: $provider")
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        isServiceRunning = true
        Log.d(TAG, "LocationForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "LocationForegroundService started")

        // Check if we have a valid token before starting
        if (!hasValidToken()) {
            Log.w(TAG, "onStartCommand: No valid token found, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            // Create and start foreground service with notification
            val notification = createNotification()
            Log.d(TAG, "Notification created successfully")

            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started with notification")

            // Start location updates
            startLocationUpdates()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationForegroundService destroyed")
        stopLocationUpdates()
        isServiceRunning = false
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        // Check if GPS provider is available
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.w(TAG, "GPS provider is not enabled")
        }

        // Check if Network provider is available
        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.w(TAG, "Network provider is not enabled")
        }

        try {
            // Request location updates from GPS provider (high accuracy)
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    LOCATION_MIN_DISTANCE,
                    locationListener
                )
                Log.d(TAG, "GPS location updates started")
            }

            // Request location updates from Network provider (fallback)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    LOCATION_MIN_DISTANCE,
                    locationListener
                )
                Log.d(TAG, "Network location updates started")
            }

            // Get last known location if available
            val lastKnownLocation = getLastKnownLocation()
            lastKnownLocation?.let { location ->
                currentLocation = location
                Log.d(TAG, "Last known location: ${location.latitude}, ${location.longitude}")
                sendLocationToServer(location)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while requesting location updates", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Illegal argument exception while requesting location updates", e)
        }
    }

    private fun getLastKnownLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        var bestLocation: Location? = null

        // Try GPS provider first
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (gpsLocation != null) {
                bestLocation = gpsLocation
            }
        }

        // Try Network provider
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (networkLocation != null) {
                if (bestLocation == null || networkLocation.accuracy < bestLocation.accuracy) {
                    bestLocation = networkLocation
                }
            }
        }

        return bestLocation
    }

    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(locationListener)
            Log.d(TAG, "Location updates stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while stopping location updates", e)
        }
    }

        private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                }

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel", e)
            }
        }
    }

        private fun createNotification(): Notification {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val locationText = currentLocation?.let {
                "Lat: ${String.format("%.8f", it.latitude)}, Lon: ${String.format("%.8f", it.longitude)}"
            } ?: "Waiting for location..."

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Via Verde")
                .setContentText(locationText)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false)
                .build()

            Log.d(TAG, "Notification created with text: $locationText")
            return notification

        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification", e)
            // Return a basic notification as fallback
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Via Verde")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun sendLocationToServer(location: Location) {
        // TODO: Implement your server communication logic here
        // This is where you would send the location data to your backend

        val locationData = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "timestamp" to location.time,
            "speed" to location.speed,
            "bearing" to location.bearing,
            "altitude" to location.altitude,
            "provider" to location.provider
        )

        Log.d(TAG, "Location data ready for server: $locationData")

        // ApiService.sendLocation(locationData)
    }

    private fun hasValidToken(): Boolean {
        val prefs = getSharedPreferences("via_verde_prefs", MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val hasToken = token != null && token.isNotEmpty()
        Log.d(TAG, "hasValidToken: Token present: $hasToken")
        return hasToken
    }
}
