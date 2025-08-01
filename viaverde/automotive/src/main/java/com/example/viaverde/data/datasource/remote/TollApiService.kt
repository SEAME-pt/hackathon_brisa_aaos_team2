package com.example.viaverde.data.datasource.remote

import android.util.Log
import com.example.viaverde.core.security.SecureNetworkManager
import com.example.viaverde.data.model.TollPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Remote API service for toll operations
 */
@Singleton
class TollApiService @Inject constructor(
    private val networkManager: SecureNetworkManager
) {

    companion object {
        private const val TOLLS_ENDPOINT = "/mtolling/services/mtolling/tolls"
    }

    /**
     * Fetch toll points from the API
     * @param authToken The authentication token
     * @return Result containing list of toll points or error
     */
    suspend fun getTollPoints(authToken: String): Result<List<TollPoint>> {
        return suspendCancellableCoroutine { continuation ->
            networkManager.get(TOLLS_ENDPOINT, authToken) { result ->
                result.fold(
                    onSuccess = { responseBody ->
                        try {
                            val tollPoints = parseTollPointsResponse(responseBody)
                            continuation.resume(Result.success(tollPoints))
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
     * Parse the JSON response to extract toll points from tollsList
     * Ignores geofencePoints and service areas
     */
    private fun parseTollPointsResponse(responseBody: String): List<TollPoint> {
        val tollPoints = mutableListOf<TollPoint>()

        try {
            val jsonResponse = JSONObject(responseBody)

                        // Get the tollsList array
            val tollsList = jsonResponse.optJSONArray("tollsList")

            if (tollsList != null) {
                for (i in 0 until tollsList.length()) {
                    val tollObject = tollsList.getJSONObject(i)

                    // Extract data directly from toll object (latitude/longitude are not nested)
                    val name = tollObject.optString("name", "")
                    val id = tollObject.optString("code", "") // Using "code" as ID
                    val latitude = tollObject.optDouble("latitude", 0.0)
                    val longitude = tollObject.optDouble("longitude", 0.0)
                    val highway = tollObject.optString("highway", "")
                    val type = tollObject.optString("type", "")

                    // Skip service areas (points containing "Área de Serviço" or "Serviço")
                    if (!name.contains("Área de Serviço", ignoreCase = true) &&
                        !name.contains("Serviço", ignoreCase = true)) {

                        val description = "Highway: $highway, Type: $type"

                        val tollPoint = TollPoint(
                            id = id,
                            name = name,
                            latitude = latitude,
                            longitude = longitude,
                            description = description,
                            isActive = true
                        )
                        tollPoints.add(tollPoint)

                        Log.d("TollApiService", "Added toll point: $name at ($latitude, $longitude)")
                    }
                }
            }

            Log.d("TollApiService", "Parsed ${tollPoints.size} toll points from tollsList")

        } catch (e: Exception) {
            Log.e("TollApiService", "Failed to parse toll points response", e)
            throw Exception("Failed to parse toll points response: ${e.message}")
        }

        return tollPoints
    }
}
