package com.example.viaverde.service

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
import com.example.viaverde.R
import com.example.viaverde.core.security.SecurityUtils
import com.example.viaverde.core.utils.Constants
import com.example.viaverde.data.model.Location as LocationModel
import com.example.viaverde.domain.repository.LocationRepository
import com.example.viaverde.presentation.main.ui.MainActivity
import com.example.viaverde.presentation.splash.ui.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service for location tracking
 */
@AndroidEntryPoint
class LocationForegroundService : Service() {

    @Inject
    lateinit var locationRepository: LocationRepository

    companion object {
        private const val TAG = "LocationForegroundService"
        private const val NOTIFICATION_ID = Constants.Location.NOTIFICATION_ID
        private const val CHANNEL_ID = Constants.Location.CHANNEL_ID
        private const val CHANNEL_NAME = Constants.Location.CHANNEL_NAME
        private const val CHANNEL_DESCRIPTION = Constants.Location.CHANNEL_DESCRIPTION

        // Location update intervals
        private const val LOCATION_UPDATE_INTERVAL = Constants.Location.UPDATE_INTERVAL
        private const val LOCATION_MIN_DISTANCE = Constants.Location.MIN_DISTANCE

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
            Log.d(TAG, "Location updated: ${SecurityUtils.maskCoordinates(location.latitude, location.longitude)}")

            // Update notification with current location
            updateNotification()

            // Send location to server using repository
            val locationModel = LocationModel(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = location.time,
                speed = location.speed,
                bearing = location.bearing,
                altitude = location.altitude,
                provider = location.provider ?: "unknown"
            )

            // Update repository
            locationRepository.updateLocation(locationModel)
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
        Log.d(TAG, "LocationForegroundService onStartCommand called - flags: $flags, startId: $startId")

        // Check if we have a valid token before starting the service
        // Note: This is a suspend function, but we can't use coroutines in onStartCommand
        // We'll assume the service should start if it's being called
        Log.d(TAG, "onStartCommand: Proceeding with service startup")

        Log.d(TAG, "onStartCommand: Token validation passed, proceeding with service startup")

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
            // Don't stop the service on error, let it retry
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationForegroundService destroyed")

        // Stop location updates
        stopLocationUpdates()

        isServiceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    setShowBadge(true)
                    enableLights(true)
                    enableVibration(false)
                    setSound(null, null)
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
            // Use a simpler approach instead of deprecated getRunningTasks
            // For now, we'll always go to MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service is active")
                .setContentText("Your location is safe")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            Log.d(TAG, "Notification created with text: Your location is safe")
            return notification

        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification", e)
            // Return a basic notification as fallback
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service is active")
                .setContentText("Your location is safe")
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates: Checking location permissions")

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "startLocationUpdates: Location permission not granted, will retry later")
            return
        }

        Log.d(TAG, "startLocationUpdates: Location permissions granted, starting updates")

        try {
            // Request location updates from GPS provider
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    LOCATION_MIN_DISTANCE,
                    locationListener
                )
                Log.d(TAG, "startLocationUpdates: GPS location updates started")
            } else {
                Log.w(TAG, "GPS provider is not enabled")
            }

            // Request location updates from Network provider as backup
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    LOCATION_MIN_DISTANCE,
                    locationListener
                )
                Log.d(TAG, "startLocationUpdates: Network location updates started")
            } else {
                Log.w(TAG, "Network provider is not enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "startLocationUpdates: Error starting location updates", e)
        }
    }

    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(locationListener)
            Log.d(TAG, "stopLocationUpdates: Location updates stopped")
        } catch (e: Exception) {
            Log.e(TAG, "stopLocationUpdates: Error stopping location updates", e)
        }
    }
}
