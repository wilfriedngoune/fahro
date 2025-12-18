package com.example.fahr.ui.main.search.model

data class Trip(
    val id: String,
    val driverId: String,
    val driverName: String,
    val driverAvatarResId: Int,
    val departureTimeRange: String,  // ex: "10:30 - 12:32"
    val routeSummary: String,        // ex: "Roseneck 8a -> TU Clausthal"
    val rating: Float
)
