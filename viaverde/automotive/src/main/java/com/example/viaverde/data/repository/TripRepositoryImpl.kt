package com.example.viaverde.data.repository

import android.util.Log
import com.example.viaverde.data.datasource.remote.TripApiService
import com.example.viaverde.data.model.Trip
import com.example.viaverde.domain.repository.TripRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepositoryImpl @Inject constructor(
    private val tripApiService: TripApiService
) : TripRepository {

    companion object {
        private const val TAG = "TripRepositoryImpl"
    }

    private var cachedTrips: List<Trip> = emptyList()

    override suspend fun getTrips(authToken: String): Result<List<Trip>> {
        return try {
            Log.d(TAG, "getTrips: Fetching trips from API")

            val result = tripApiService.getTrips(authToken)

            result.fold(
                onSuccess = { trips ->
                    Log.d(TAG, "getTrips: Successfully fetched ${trips.size} trips")
                    cachedTrips = trips
                    Result.success(trips)
                },
                onFailure = { exception ->
                    Log.e(TAG, "getTrips: Failed to fetch trips", exception)
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "getTrips: Error in repository", e)
            Result.failure(e)
        }
    }

    override fun getCachedTrips(): List<Trip> {
        return cachedTrips
    }

    override fun clearCache() {
        Log.d(TAG, "clearCache: Clearing cached trips")
        cachedTrips = emptyList()
    }
}
