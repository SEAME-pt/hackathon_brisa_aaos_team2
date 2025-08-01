package com.example.viaverde.data.model

data class Trip(
    val tripNumber: Int,
    val totalDistance: Double,
    val totalDuration: Int,
    val highways: String,
    val startDate: Long,
    val totalCost: Double,
    val licensePlate: LicensePlate
)

data class LicensePlate(
    val value: String,
    val vehicleCategory: String,
    val default: Boolean
)

data class TripResponse(
    val count: Int,
    val totalCount: Int,
    val page: Int,
    val totalPages: Int,
    val data: List<Trip>
)
