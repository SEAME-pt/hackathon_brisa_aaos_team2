package com.example.viaverde.data.repository

import com.example.viaverde.data.datasource.local.SecurePreferencesDataSource
import com.example.viaverde.data.datasource.remote.LocationApiService
import com.example.viaverde.data.model.Location
import com.example.viaverde.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of LocationRepository that coordinates between local and remote data sources
 */
@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val localDataSource: SecurePreferencesDataSource,
    private val remoteDataSource: LocationApiService
) : LocationRepository {

    private val _locationUpdates = MutableStateFlow<Location?>(null)
    private val _locationTrackingStatus = MutableStateFlow(false)

    override suspend fun startLocationTracking(): Result<Unit> {
        return try {
            _locationTrackingStatus.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopLocationTracking(): Result<Unit> {
        return try {
            _locationTrackingStatus.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentLocation(): Location? {
        return _locationUpdates.value
    }

    override suspend fun sendLocationToServer(location: Location): Result<Unit> {
        return try {
            val authToken = localDataSource.getAuthToken()
            if (authToken != null) {
                remoteDataSource.sendLocation(location, authToken)
            } else {
                Result.failure(Exception("No authentication token available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeLocationUpdates(): Flow<Location> {
        return _locationUpdates.asStateFlow().mapNotNull { it }
    }

    override suspend fun isLocationTrackingActive(): Boolean {
        return _locationTrackingStatus.value
    }

    override fun observeLocationTrackingStatus(): Flow<Boolean> {
        return _locationTrackingStatus.asStateFlow()
    }

    /**
     * Update current location (called by location service)
     */
    override fun updateLocation(location: Location) {
        _locationUpdates.value = location
    }
}
