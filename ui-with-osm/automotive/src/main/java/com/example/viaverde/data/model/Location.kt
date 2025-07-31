package com.example.viaverde.data.model

/**
 * Data model representing location information
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val altitude: Double = 0.0,
    val provider: String = "unknown"
) {
    /**
     * Check if the location is valid
     */
    fun isValid(): Boolean {
        return latitude != 0.0 && longitude != 0.0 && accuracy > 0
    }

    /**
     * Get distance to another location in meters
     */
    fun distanceTo(other: Location): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            latitude, longitude,
            other.latitude, other.longitude,
            results
        )
        return results[0]
    }
}
