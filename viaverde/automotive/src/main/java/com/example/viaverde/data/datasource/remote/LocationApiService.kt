package com.example.viaverde.data.datasource.remote

import com.example.viaverde.core.security.SecureNetworkManager
import com.example.viaverde.data.model.Location
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Remote API service for location operations
 */
@Singleton
class LocationApiService @Inject constructor(
    private val networkManager: SecureNetworkManager
) {

    companion object {
        private const val LOCATION_ENDPOINT = "/mtolling/services/mtolling/location"
    }

    /**
     * Send location to server
     */
    suspend fun sendLocation(location: Location, authToken: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val jsonBody = JSONObject().apply {
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("accuracy", location.accuracy)
                put("timestamp", location.timestamp)
                put("speed", location.speed)
                put("bearing", location.bearing)
                put("altitude", location.altitude)
                put("provider", location.provider)
            }.toString()

            networkManager.post(LOCATION_ENDPOINT, jsonBody, authToken) { result ->
                result.fold(
                    onSuccess = {
                        continuation.resume(Result.success(Unit))
                    },
                    onFailure = { exception ->
                        continuation.resume(Result.failure(exception))
                    }
                )
            }
        }
    }

    /**
     * Get location history (placeholder for future implementation)
     */
    suspend fun getLocationHistory(authToken: String): Result<List<Location>> {
        return suspendCancellableCoroutine { continuation ->
            networkManager.get(LOCATION_ENDPOINT, authToken) { result ->
                result.fold(
                    onSuccess = { responseBody ->
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val locations = parseLocationHistory(jsonResponse)
                            continuation.resume(Result.success(locations))
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    },
                    onFailure = { exception ->
                        continuation.resume(Result.failure(exception))
                    }
                )
            }
        }
    }

    /**
     * Parse location history from JSON response
     */
    private fun parseLocationHistory(jsonResponse: JSONObject): List<Location> {
        // This is a placeholder implementation
        // In a real app, you would parse the actual JSON structure
        return emptyList()
    }
}
