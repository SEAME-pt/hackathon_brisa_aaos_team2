package com.example.viaverde.data.datasource.remote

import android.util.Log
import com.example.viaverde.data.model.Trip
import com.example.viaverde.data.model.LicensePlate
import com.example.viaverde.data.model.TripResponse
import com.example.viaverde.core.security.SecureNetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class TripApiService @Inject constructor(
    private val secureNetworkManager: SecureNetworkManager
) {
    companion object {
        private const val TAG = "TripApiService"
        private const val TRIPS_ENDPOINT = "/mtolling/services/mtolling/trips"
    }

    suspend fun getTrips(authToken: String): Result<List<Trip>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "getTrips: Fetching trips from API")

            return@withContext suspendCancellableCoroutine { continuation ->
                secureNetworkManager.get(TRIPS_ENDPOINT, authToken) { result ->
                    result.fold(
                        onSuccess = { responseBody ->
                            Log.d(TAG, "getTrips: API response received: $responseBody")
                            val trips = parseTripsResponse(responseBody)
                            Log.d(TAG, "getTrips: Successfully parsed ${trips.size} trips")
                            continuation.resume(Result.success(trips))
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "getTrips: API request failed", exception)
                            continuation.resume(Result.failure(exception))
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTrips: Error fetching trips", e)
            Result.failure(e)
        }
    }

    private fun parseTripsResponse(responseBody: String): List<Trip> {
        val trips = mutableListOf<Trip>()

        try {
            val jsonObject = JSONObject(responseBody)
            val dataArray = jsonObject.getJSONArray("data")

            for (i in 0 until dataArray.length()) {
                val tripObject = dataArray.getJSONObject(i)
                val licensePlateObject = tripObject.getJSONObject("licensePlate")

                val trip = Trip(
                    tripNumber = tripObject.optInt("tripNumber"),
                    totalDistance = tripObject.optDouble("totalDistance"),
                    totalDuration = tripObject.optInt("totalDuration"),
                    highways = tripObject.optString("highways"),
                    startDate = tripObject.optLong("startDate"),
                    totalCost = tripObject.optDouble("totalCost"),
                    licensePlate = LicensePlate(
                        value = licensePlateObject.optString("value"),
                        vehicleCategory = licensePlateObject.optString("vehicleCategory"),
                        default = licensePlateObject.optBoolean("default")
                    )
                )

                trips.add(trip)
            }

            Log.d(TAG, "parseTripsResponse: Parsed ${trips.size} trips from response")
        } catch (e: Exception) {
            Log.e(TAG, "parseTripsResponse: Error parsing trips response", e)
        }

        return trips
    }
}
