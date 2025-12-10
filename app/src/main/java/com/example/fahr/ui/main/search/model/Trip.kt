package com.example.fahr.ui.main.search.model

data class Trip(
    val tripId: String,
    val driverName: String,
    val driverAvatar: Int,
    val departureTime: String,
    val address: String,
    val rating: Float
)
