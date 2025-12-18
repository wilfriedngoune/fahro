package com.example.fahr.ui.main.search.model


data class TripDocument(
    val departureDate: String = "",
    val departureTime: String = "",
    val departureAddress: String = "",
    val arrivalAddress: String = "",
    val stops: List<String> = emptyList(),
    val price: Double = 0.0,
    val driverId: String = ""
)
