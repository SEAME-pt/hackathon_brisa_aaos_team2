package com.example.viaverde.domain.repository

import com.example.viaverde.data.model.Location
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for location operations
 */
interface LocationRepository {

    /**
     * Start location tracking
     */
    suspend fun startLocationTracking(): Result<Unit>

    /**
     * Stop location tracking
     */
    suspend fun stopLocationTracking(): Result<Unit>

    /**
     * Get current location
     */
    suspend fun getCurrentLocation(): Location?

    /**
     * Send location to server
     */
    suspend fun sendLocationToServer(location: Location): Result<Unit>

    /**
     * Observe location updates
     */
    fun observeLocationUpdates(): Flow<Location>

    /**
     * Check if location tracking is active
     */
    suspend fun isLocationTrackingActive(): Boolean

    /**
     * Get location tracking status
     */
    fun observeLocationTrackingStatus(): Flow<Boolean>

    /**
     * Update current location (called by location service)
     */
    fun updateLocation(location: Location)
}
