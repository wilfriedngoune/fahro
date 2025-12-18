package com.example.fahr.ui.main.profile.model

data class TripRequest(
    val id: String,            // bookingId
    val tripId: String,        // id du trip dans "trips"
    val passengerId: String,   // id de l'utilisateur qui a booké
    val name: String,
    val avatarResId: Int,
    val departure: String,
    val arrival: String,
    val departureTime: String,
    val arrivalTime: String,
    val price: String
)
