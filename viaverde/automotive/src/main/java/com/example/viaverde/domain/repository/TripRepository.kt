package com.example.viaverde.domain.repository

import com.example.viaverde.data.model.Trip

interface TripRepository {
    suspend fun getTrips(authToken: String): Result<List<Trip>>
    fun getCachedTrips(): List<Trip>
    fun clearCache()
}
