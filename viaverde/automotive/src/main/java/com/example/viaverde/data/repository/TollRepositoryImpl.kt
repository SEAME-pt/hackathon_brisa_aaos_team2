package com.example.viaverde.data.repository

import com.example.viaverde.data.datasource.remote.TollApiService
import com.example.viaverde.data.model.TollPoint
import com.example.viaverde.domain.repository.TollRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for toll points data management
 */
@Singleton
class TollRepositoryImpl @Inject constructor(
    private val tollApiService: TollApiService
) : TollRepository {

    // In-memory cache for toll points (cleared when app is shutdown)
    private var cachedTollPoints: List<TollPoint>? = null

    /**
     * Get toll points from cache or fetch from API
     * @param authToken The authentication token
     * @param forceRefresh Force refresh from API even if cached
     * @return List of toll points
     */
    override suspend fun getTollPoints(authToken: String, forceRefresh: Boolean): Result<List<TollPoint>> {
        return withContext(Dispatchers.IO) {
            try {
                // Return cached data if available and not forcing refresh
                if (!forceRefresh && cachedTollPoints != null) {
                    return@withContext Result.success(cachedTollPoints!!)
                }

                // Fetch from API
                val result = tollApiService.getTollPoints(authToken)

                result.fold(
                    onSuccess = { tollPoints ->
                        // Cache the fetched data
                        cachedTollPoints = tollPoints
                        Result.success(tollPoints)
                    },
                    onFailure = { exception ->
                        // Return cached data if available, otherwise return error
                        if (cachedTollPoints != null) {
                            Result.success(cachedTollPoints!!)
                        } else {
                            Result.failure(exception)
                        }
                    }
                )
            } catch (e: Exception) {
                // Return cached data if available, otherwise return error
                if (cachedTollPoints != null) {
                    Result.success(cachedTollPoints!!)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * Clear the cached toll points
     */
    override fun clearCache() {
        cachedTollPoints = null
    }

    /**
     * Get cached toll points without making API call
     * @return Cached toll points or null if not available
     */
    override fun getCachedTollPoints(): List<TollPoint>? {
        return cachedTollPoints
    }
}
