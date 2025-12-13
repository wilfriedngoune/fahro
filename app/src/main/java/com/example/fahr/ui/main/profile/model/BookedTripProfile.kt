// BookedTripProfile.kt
package com.example.fahr.ui.main.profile.model

data class BookedTripProfile(
    val id: String,
    val departure: String,
    val arrival: String,
    val departureTime: String,
    val arrivalTime: String,
    val status: String   // "Pending", "Accepted", "Denied"
)
