package com.example.viaverde.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.viaverde.R
import com.example.viaverde.data.model.Trip
import com.example.viaverde.domain.repository.AuthRepository
import com.example.viaverde.domain.repository.TripRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TripMonitorService : Service() {

    companion object {
        private const val TAG = "TripMonitorService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "trip_notifications"
        private const val CHANNEL_NAME = "Trip Notifications"
        private const val MONITORING_INTERVAL = 2000L // 2 seconds

        @Volatile
        private var isServiceRunning = false

        fun isRunning(): Boolean = isServiceRunning
    }

    @Inject
    lateinit var tripRepository: TripRepository

    @Inject
    lateinit var authRepository: AuthRepository

    private var monitoringJob: Job? = null
    private var lastKnownTrips: List<Trip> = emptyList()
    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        isServiceRunning = true
        TripMonitorService.isServiceRunning = true
        Log.d(TAG, "TripMonitorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TripMonitorService onStartCommand called")

        // Check if trip monitoring is enabled in settings
        val sharedPrefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isTripMonitoringEnabled = sharedPrefs.getBoolean("trip_monitoring_enabled", false)

        Log.d(TAG, "onStartCommand: Trip monitoring setting: $isTripMonitoringEnabled")

        if (!isTripMonitoringEnabled) {
            Log.d(TAG, "onStartCommand: Trip monitoring disabled, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        if (monitoringJob == null || monitoringJob?.isActive == false) {
            startTripMonitoring()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TripMonitorService destroyed")

        stopTripMonitoring()
        isServiceRunning = false
        TripMonitorService.isServiceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startTripMonitoring() {
        Log.d(TAG, "startTripMonitoring: Starting trip monitoring")

        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isServiceRunning) {
                try {
                    checkForNewTrips()
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "startTripMonitoring: Error during monitoring", e)
                    delay(MONITORING_INTERVAL) // Continue monitoring even on error
                }
            }
        }
    }

    private fun stopTripMonitoring() {
        Log.d(TAG, "stopTripMonitoring: Stopping trip monitoring")

        monitoringJob?.cancel()
        monitoringJob = null
    }

    private suspend fun checkForNewTrips() {
        try {
            val authToken = authRepository.getAuthToken()
            if (authToken == null) {
                Log.w(TAG, "checkForNewTrips: No auth token available")
                return
            }

            val result = tripRepository.getTrips(authToken.token)
            result.fold(
                onSuccess = { trips ->
                    Log.d(TAG, "checkForNewTrips: Fetched ${trips.size} trips")

                    // Check for new trips
                    val newTrips = findNewTrips(trips)
                    if (newTrips.isNotEmpty()) {
                        Log.d(TAG, "checkForNewTrips: Found ${newTrips.size} new trips")
                        showNewTripNotifications(newTrips)
                    }

                    lastKnownTrips = trips
                },
                onFailure = { exception ->
                    Log.e(TAG, "checkForNewTrips: Failed to fetch trips", exception)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "checkForNewTrips: Error checking for new trips", e)
        }
    }

    private fun findNewTrips(currentTrips: List<Trip>): List<Trip> {
        if (lastKnownTrips.isEmpty()) {
            // First time running, don't show notifications for existing trips
            return emptyList()
        }

        // Find trips that are in currentTrips but not in lastKnownTrips
        val newTrips = mutableListOf<Trip>()

        for (trip in currentTrips) {
            val isNew = lastKnownTrips.none { it.tripNumber == trip.tripNumber }
            if (isNew) {
                newTrips.add(trip)
            }
        }

        return newTrips
    }

    private fun showNewTripNotifications(newTrips: List<Trip>) {
        for (trip in newTrips) {
            showTripNotification(trip)
        }
    }

    private fun showTripNotification(trip: Trip) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = "New Trip Detected"
        val message = "Highway: ${trip.highways}\nCost: €${String.format("%.2f", trip.totalCost)}"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + trip.tripNumber, notification)

        Log.d(TAG, "showTripNotification: Notification shown for trip ${trip.tripNumber}")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for new trips"
                    enableLights(true)
                    enableVibration(true)
                }

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)

                Log.d(TAG, "createNotificationChannel: Notification channel created")
            } catch (e: Exception) {
                Log.e(TAG, "createNotificationChannel: Error creating notification channel", e)
            }
        }
    }
}
