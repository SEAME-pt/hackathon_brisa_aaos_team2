package com.example.viaverde.domain.repository

import com.example.viaverde.data.model.TollPoint

/**
 * Repository interface for toll points operations
 */
interface TollRepository {

    /**
     * Get toll points from cache or fetch from API
     * @param authToken The authentication token
     * @param forceRefresh Force refresh from API even if cached
     * @return Result containing list of toll points or error
     */
    suspend fun getTollPoints(authToken: String, forceRefresh: Boolean = false): Result<List<TollPoint>>

    /**
     * Clear the cached toll points
     */
    fun clearCache()

    /**
     * Get cached toll points without making API call
     * @return Cached toll points or null if not available
     */
    fun getCachedTollPoints(): List<TollPoint>?
}
