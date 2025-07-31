package com.example.viaverde.data.model

/**
 * Data model representing a user
 */
data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val isLoggedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
