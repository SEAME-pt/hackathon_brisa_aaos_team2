package com.example.viaverde.data.model

/**
 * Data model representing a toll point from the API
 */
data class TollPoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val isActive: Boolean = true
)
