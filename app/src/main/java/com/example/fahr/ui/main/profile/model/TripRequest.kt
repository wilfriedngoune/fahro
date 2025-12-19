package com.example.fahr.ui.main.profile.model

data class TripRequest(
    val id: String,
    val tripId: String,
    val passengerId: String,
    val name: String,
    val avatarResId: Int,
    val departure: String,
    val arrival: String,
    val departureTime: String,
    val arrivalTime: String,
    val price: String
)
